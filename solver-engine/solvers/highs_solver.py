"""HiGHS solver adapter for LP and MILP problems."""

import logging
import time
from typing import Any, Dict, Optional

import highspy

from models.config import SolverConfig
from models.problem import ObjectiveSense, ProblemDefinition, VariableType
from models.result import SolverResult
from solvers.base import AbstractSolver

logger = logging.getLogger(__name__)


class HighsSolver(AbstractSolver):
    """HiGHS solver supporting LP and MILP problems."""

    SUPPORTED_TYPES = {"LP", "MILP"}

    def solve(self, problem: ProblemDefinition, config: Optional[SolverConfig] = None, **kwargs: Any) -> SolverResult:
        """Solve the given optimization problem using HiGHS.

        Args:
            problem: The problem definition to solve.
            config: Optional solver configuration.
            **kwargs: Additional solver-specific parameters.

        Returns:
            SolverResult containing the solution.
        """
        start_time = time.time()
        config = config or SolverConfig()

        try:
            h = highspy.Highs()

            # Apply configuration
            self._apply_config(h, config)

            # Set number of threads
            if config.threads and config.threads > 0:
                h.setOptionValue("threads", config.threads)

            # Build the model
            num_vars = len(problem.variables)
            if num_vars == 0:
                return SolverResult(
                    status="error",
                    solve_time=time.time() - start_time,
                )

            # Variable bounds and types
            lower_bounds = []
            upper_bounds = []
            integrality = []

            for var_def in problem.variables:
                lb = var_def.lower_bound if var_def.lower_bound is not None else 0.0
                ub = var_def.upper_bound if var_def.upper_bound is not None else 1e20
                lower_bounds.append(lb)
                upper_bounds.append(ub)

                if var_def.var_type == VariableType.BINARY:
                    integrality.append(1)  # Integer
                elif var_def.var_type == VariableType.INTEGER:
                    integrality.append(1)
                else:
                    integrality.append(0)  # Continuous

            # Build coefficient matrix for constraints
            num_cons = len(problem.constraints)
            var_name_to_idx = {v.name: i for i, v in enumerate(problem.variables)}

            # Objective coefficients
            obj_coeffs = [0.0] * num_vars
            if problem.objective and problem.objective.expression:
                obj_coeffs = self._parse_objective_coeffs(
                    problem.objective.expression, var_name_to_idx, num_vars
                )

            # Constraint matrix (sparse)
            starts = [0]
            indices = []
            values = []
            lower_bounds_con = []
            upper_bounds_con = []

            for constraint_def in problem.constraints:
                row_indices, row_values = self._parse_constraint_coeffs(
                    constraint_def.expression, var_name_to_idx, num_vars
                )
                indices.extend(row_indices)
                values.extend(row_values)
                starts.append(len(indices))

                lb = constraint_def.lower_bound
                ub = constraint_def.upper_bound
                lower_bounds_con.append(lb if lb is not None else -1e20)
                upper_bounds_con.append(ub if ub is not None else 1e20)

            # Add model to HiGHS
            h.addVars(num_vars, lower_bounds, upper_bounds)

            if num_cons > 0:
                h.addRows(
                    num_cons,
                    lower_bounds_con,
                    upper_bounds_con,
                    starts,
                    indices,
                    values,
                )

            # Set objective
            sense = 1  # minimize
            if problem.objective and problem.objective.sense == ObjectiveSense.MAXIMIZE:
                sense = -1  # maximize
            h.changeObjectiveSense(sense)
            for i, coeff in enumerate(obj_coeffs):
                h.changeColCost(i, coeff * sense if sense == -1 else coeff)

            # Set integrality
            for i, is_int in enumerate(integrality):
                if is_int:
                    h.changeColIntegrality(i, 1)

            # Solve
            h.solve()
            solve_time = time.time() - start_time

            # Get results
            status = self._map_status(h)
            objective_value = None
            variables = {}

            if status in ("optimal", "feasible"):
                sol = h.getSolution()
                col_value = sol.col_value
                objective_value = h.getInfoValue("objective_function_value")[1]

                for i, var_def in enumerate(problem.variables):
                    if i < len(col_value):
                        variables[var_def.name] = float(col_value[i])

            return SolverResult(
                status=status,
                objective_value=objective_value,
                variables=variables,
                solve_time=solve_time,
            )

        except Exception as e:
            logger.error("HiGHS solver failed: %s", str(e), exc_info=True)
            return SolverResult(
                status="error",
                solve_time=time.time() - start_time,
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        """Check if this solver supports the given problem type."""
        # HiGHS supports LP and MILP
        return True

    def cancel(self, job_id: str) -> bool:
        """Cancel a running solve job. HiGHS does not support async cancellation."""
        logger.warning("HiGHS does not support async cancellation for job %s", job_id)
        return False

    def _apply_config(self, h: Any, config: SolverConfig) -> None:
        """Apply solver configuration parameters."""
        if config.time_limit:
            h.setOptionValue("time_limit", config.time_limit)
        if config.gap_tolerance:
            h.setOptionValue("mip_rel_gap", config.gap_tolerance)
        if config.max_iterations:
            h.setOptionValue("simplex_iteration_limit", config.max_iterations)

    def _parse_objective_coeffs(self, expression: str, var_name_to_idx: Dict, num_vars: int) -> list:
        """Parse objective expression into coefficient array."""
        coeffs = [0.0] * num_vars
        if not expression or not expression.strip():
            return coeffs

        terms = expression.replace("-", "+-").split("+")
        for term in terms:
            term = term.strip()
            if not term:
                continue

            if "*" in term:
                parts = term.split("*")
                coeff = float(parts[0].strip())
                var_name = parts[1].strip()
                if var_name in var_name_to_idx:
                    coeffs[var_name_to_idx[var_name]] += coeff
                else:
                    logger.warning("Variable %s not found in model", var_name)
            else:
                try:
                    float(term)  # Constant, ignore for now
                except ValueError:
                    # Variable with an implicit coefficient of 1 or -1, e.g. "x3" or "-x3".
                    sign = 1.0
                    name_token = term.strip()
                    if name_token.startswith("-"):
                        sign = -1.0
                        name_token = name_token[1:].strip()
                    elif name_token.startswith("+"):
                        name_token = name_token[1:].strip()
                    if name_token in var_name_to_idx:
                        coeffs[var_name_to_idx[name_token]] += sign

        return coeffs

    def _parse_constraint_coeffs(self, expression: str, var_name_to_idx: Dict, num_vars: int) -> tuple:
        """Parse constraint expression into sparse row coefficients.

        Returns:
            Tuple of (indices, values) for sparse representation.
        """
        indices = []
        values = []

        if not expression or not expression.strip():
            return indices, values

        terms = expression.replace("-", "+-").split("+")
        for term in terms:
            term = term.strip()
            if not term:
                continue

            if "*" in term:
                parts = term.split("*")
                coeff = float(parts[0].strip())
                var_name = parts[1].strip()
                if var_name in var_name_to_idx:
                    indices.append(var_name_to_idx[var_name])
                    values.append(coeff)
                else:
                    logger.warning("Variable %s not found in model", var_name)
            else:
                try:
                    float(term)  # Constant in constraint, skip for matrix
                except ValueError:
                    # Variable with an implicit coefficient of 1 or -1, e.g. "x3" or "-x3".
                    sign = 1.0
                    name_token = term.strip()
                    if name_token.startswith("-"):
                        sign = -1.0
                        name_token = name_token[1:].strip()
                    elif name_token.startswith("+"):
                        name_token = name_token[1:].strip()
                    if name_token in var_name_to_idx:
                        indices.append(var_name_to_idx[name_token])
                        values.append(sign)

        return indices, values

    def _map_status(self, h: Any) -> str:
        """Map HiGHS model status to our status string."""
        try:
            status = h.getInfoValue("primal_status")[1]
            # HiGHS status codes
            status_map = {
                2: "optimal",     # Feasible
                1: "feasible",    # Feasible
                0: "not_solved",
                -1: "infeasible",
            }
            return status_map.get(status, "error")
        except Exception:
            # Fallback: check model status
            try:
                model_status = h.getModelStatus()
                if model_status == 1:  # Not loaded
                    return "error"
                elif model_status == 2:  # Optimal
                    return "optimal"
                elif model_status == 3:  # Infeasible
                    return "infeasible"
                elif model_status == 4:  # Unbounded
                    return "unbounded"
                elif model_status == 6:  # Time limit reached
                    return "timeout"
                else:
                    return "error"
            except Exception:
                return "error"
