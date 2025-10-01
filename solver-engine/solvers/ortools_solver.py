"""OR-Tools solver adapter for LP, MILP, and MIQP problems."""

import logging
import time
from typing import Any, Dict, Optional

from ortools.linear_solver import pywraplp

from models.config import SolverConfig
from models.problem import ObjectiveSense, ProblemDefinition, VariableType
from models.result import SolverResult
from solvers.base import AbstractSolver

logger = logging.getLogger(__name__)


class ORToolsSolver(AbstractSolver):
    """OR-Tools solver supporting LP, MILP, and MIQP problems."""

    SUPPORTED_TYPES = {"LP", "MILP", "MIQP"}

    def solve(self, problem: ProblemDefinition, config: Optional[SolverConfig] = None, **kwargs: Any) -> SolverResult:
        """Solve the given optimization problem using OR-Tools.

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
            # Select solver type based on problem characteristics
            solver_type = self._select_solver_type(problem)
            solver = pywraplp.Solver.CreateSolver(solver_type)

            if solver is None:
                return SolverResult(
                    status="error",
                    solve_time=time.time() - start_time,
                )

            # Apply configuration
            self._apply_config(solver, config)

            # Create variables
            var_map = {}
            for var_def in problem.variables:
                var = self._create_variable(solver, var_def)
                var_map[var_def.name] = var

            # Add constraints
            for constraint_def in problem.constraints:
                self._add_constraint(solver, var_map, constraint_def)

            # Set objective
            if problem.objective:
                self._set_objective(solver, var_map, problem.objective)

            # Solve
            status_code = solver.Solve()
            solve_time = time.time() - start_time

            # Map status
            status = self._map_status(status_code)

            # Extract results
            objective_value = None
            variables = {}
            if status in ("optimal", "feasible"):
                objective_value = solver.Objective().Value()
                for name, var in var_map.items():
                    variables[name] = var.solution_value()

            return SolverResult(
                status=status,
                objective_value=objective_value,
                variables=variables,
                solve_time=solve_time,
            )

        except Exception as e:
            logger.error("OR-Tools solver failed: %s", str(e), exc_info=True)
            return SolverResult(
                status="error",
                solve_time=time.time() - start_time,
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        """Check if this solver supports the given problem type."""
        return True  # OR-Tools supports LP, MILP, MIQP

    def cancel(self, job_id: str) -> bool:
        """Cancel a running solve job. OR-Tools does not support async cancellation."""
        logger.warning("OR-Tools does not support async cancellation for job %s", job_id)
        return False

    def _select_solver_type(self, problem: ProblemDefinition) -> str:
        """Select the appropriate OR-Tools solver based on problem characteristics."""
        has_integer = any(
            v.var_type in (VariableType.INTEGER, VariableType.BINARY)
            for v in problem.variables
        )
        if has_integer:
            return "CBC"  # CBC for MILP/MIQP
        return "GLOP"  # GLOP for pure LP

    def _create_variable(self, solver: pywraplp.Solver, var_def) -> Any:
        """Create an OR-Tools variable from a variable definition."""
        lb = var_def.lower_bound if var_def.lower_bound is not None else -solver.infinity()
        ub = var_def.upper_bound if var_def.upper_bound is not None else solver.infinity()

        if var_def.var_type == VariableType.BINARY:
            return solver.IntVar(0, 1, var_def.name)
        elif var_def.var_type == VariableType.INTEGER:
            return solver.IntVar(lb, ub, var_def.name)
        else:
            return solver.NumVar(lb, ub, var_def.name)

    def _add_constraint(self, solver: pywraplp.Solver, var_map: Dict, constraint_def) -> None:
        """Add a constraint to the solver model.

        Supports simple linear expressions of the form: c1*x1 + c2*x2 + ... op rhs
        where the expression string contains terms like '3*x1 + 2*x2'.
        """
        try:
            expr = self._parse_linear_expression(solver, var_map, constraint_def.expression)

            lb = constraint_def.lower_bound
            ub = constraint_def.upper_bound

            if lb is not None and ub is not None:
                solver.Add(lb <= expr)
                solver.Add(expr <= ub)
            elif lb is not None:
                solver.Add(lb <= expr)
            elif ub is not None:
                solver.Add(expr <= ub)
            else:
                # Default: treat as equality if no bounds specified
                logger.debug("Constraint %s has no bounds, skipping", constraint_def.name)
        except Exception as e:
            logger.warning("Failed to add constraint %s: %s", constraint_def.name, str(e))

    def _parse_linear_expression(self, solver: pywraplp.Solver, var_map: Dict, expression: str) -> Any:
        """Parse a linear expression string into an OR-Tools linear expression.

        Supports expressions like: '3*x1 + 2*x2 - x3'
        """
        expr = 0
        if not expression or not expression.strip():
            return expr

        # Split into terms
        terms = expression.replace("-", "+-").split("+")
        for term in terms:
            term = term.strip()
            if not term:
                continue

            if "*" in term:
                parts = term.split("*")
                coeff = float(parts[0].strip())
                var_name = parts[1].strip()
                if var_name in var_map:
                    expr += coeff * var_map[var_name]
                else:
                    logger.warning("Variable %s not found in model", var_name)
            else:
                # Constant term
                try:
                    expr += float(term)
                except ValueError:
                    # Variable with an implicit coefficient of 1 or -1, e.g. "x3" or "-x3".
                    # The leading sign and surrounding spaces must be stripped before lookup,
                    # otherwise terms like "- order_B" would be dropped silently.
                    sign = 1.0
                    name_token = term.strip()
                    if name_token.startswith("-"):
                        sign = -1.0
                        name_token = name_token[1:].strip()
                    elif name_token.startswith("+"):
                        name_token = name_token[1:].strip()
                    if name_token in var_map:
                        expr += sign * var_map[name_token]
                    else:
                        logger.warning("Unparseable term: %s", term)

        return expr

    def _set_objective(self, solver: pywraplp.Solver, var_map: Dict, objective) -> None:
        """Set the objective function."""
        expr = self._parse_linear_expression(solver, var_map, objective.expression)

        if objective.sense == ObjectiveSense.MAXIMIZE:
            solver.Maximize(expr)
        else:
            solver.Minimize(expr)

    def _apply_config(self, solver: pywraplp.Solver, config: SolverConfig) -> None:
        """Apply solver configuration parameters."""
        if config.time_limit:
            solver.SetTimeLimit(int(config.time_limit * 1000))  # OR-Tools expects milliseconds

        if config.max_iterations:
            solver.SetNumThreads(config.threads or 0)

    def _map_status(self, status_code: int) -> str:
        """Map OR-Tools status code to our status string."""
        status_map = {
            pywraplp.Solver.OPTIMAL: "optimal",
            pywraplp.Solver.FEASIBLE: "feasible",
            pywraplp.Solver.INFEASIBLE: "infeasible",
            pywraplp.Solver.UNBOUNDED: "unbounded",
            pywraplp.Solver.ABNORMAL: "error",
            pywraplp.Solver.NOT_SOLVED: "not_solved",
        }
        return status_map.get(status_code, "error")
