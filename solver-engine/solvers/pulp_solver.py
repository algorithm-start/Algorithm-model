"""PuLP solver adapter bridging to CBC, CPLEX, Gurobi if available."""

import logging
import time
from typing import Any, Dict, Optional

import pulp

from models.config import SolverConfig
from models.problem import ObjectiveSense, ProblemDefinition, VariableType
from models.result import SolverResult
from solvers.base import AbstractSolver

logger = logging.getLogger(__name__)


class PuLPSolver(AbstractSolver):
    """PuLP solver supporting LP and MILP problems.

    Bridges to CBC (default), CPLEX, or Gurobi if available.
    Most flexible for commercial solver integration.
    """

    SUPPORTED_TYPES = {"LP", "MILP"}

    def solve(self, problem: ProblemDefinition, config: Optional[SolverConfig] = None, **kwargs: Any) -> SolverResult:
        """Solve the given optimization problem using PuLP.

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
            # Create PuLP problem
            sense = pulp.LpMaximize if (
                problem.objective and problem.objective.sense == ObjectiveSense.MAXIMIZE
            ) else pulp.LpMinimize

            prob = pulp.LpProblem(problem.name, sense)

            # Create variables
            var_map = {}
            for var_def in problem.variables:
                var = self._create_variable(var_def)
                var_map[var_def.name] = var

            # Add constraints
            for constraint_def in problem.constraints:
                self._add_constraint(prob, var_map, constraint_def)

            # Set objective
            if problem.objective and problem.objective.expression:
                obj_expr = self._parse_expression(problem.objective.expression, var_map)
                prob += obj_expr

            # Select solver
            solver = self._get_solver(config)

            # Apply configuration
            if config.time_limit:
                solver.timeLimit = config.time_limit
            if config.gap_tolerance:
                if hasattr(solver, "gapRel"):
                    solver.gapRel = config.gap_tolerance

            # Solve
            status_code = prob.solve(solver)
            solve_time = time.time() - start_time

            # Map status
            status = self._map_status(status_code)

            # Extract results
            objective_value = None
            variables = {}
            if status in ("optimal", "feasible"):
                objective_value = pulp.value(prob.objective)
                for name, var in var_map.items():
                    var_val = var.varValue
                    if var_val is not None:
                        variables[name] = var_val

            return SolverResult(
                status=status,
                objective_value=objective_value,
                variables=variables,
                solve_time=solve_time,
            )

        except Exception as e:
            logger.error("PuLP solver failed: %s", str(e), exc_info=True)
            return SolverResult(
                status="error",
                solve_time=time.time() - start_time,
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        """Check if this solver supports the given problem type.

        PuLP supports LP and MILP.
        """
        # Check for nonlinear - PuLP doesn't support nonlinear
        nonlinear_keywords = ["sin", "cos", "exp", "log", "sqrt", "**2", "^2"]
        if problem.objective and problem.objective.expression:
            if any(kw in problem.objective.expression for kw in nonlinear_keywords):
                return False
        return True

    def cancel(self, job_id: str) -> bool:
        """Cancel a running solve job. PuLP does not support async cancellation."""
        logger.warning("PuLP does not support async cancellation for job %s", job_id)
        return False

    def _create_variable(self, var_def) -> pulp.LpVariable:
        """Create a PuLP variable from a variable definition."""
        lb = var_def.lower_bound if var_def.lower_bound is not None else None
        ub = var_def.upper_bound if var_def.upper_bound is not None else None

        if var_def.var_type == VariableType.BINARY:
            return pulp.LpVariable(var_def.name, cat="Binary")
        elif var_def.var_type == VariableType.INTEGER:
            return pulp.LpVariable(var_def.name, lowBound=lb, upBound=ub, cat="Integer")
        else:
            return pulp.LpVariable(var_def.name, lowBound=lb, upBound=ub, cat="Continuous")

    def _add_constraint(self, prob: pulp.LpProblem, var_map: Dict, constraint_def) -> None:
        """Add a constraint to the PuLP problem."""
        try:
            expr = self._parse_expression(constraint_def.expression, var_map)

            lb = constraint_def.lower_bound
            ub = constraint_def.upper_bound

            if lb is not None and ub is not None:
                prob += (lb <= expr, constraint_def.name + "_lb")
                prob += (expr <= ub, constraint_def.name + "_ub")
            elif ub is not None:
                prob += (expr <= ub, constraint_def.name)
            elif lb is not None:
                prob += (lb <= expr, constraint_def.name)
            else:
                logger.debug("Constraint %s has no bounds, skipping", constraint_def.name)
        except Exception as e:
            logger.warning("Failed to add constraint %s: %s", constraint_def.name, str(e))

    def _parse_expression(self, expression: str, var_map: Dict) -> Any:
        """Parse an expression string into a PuLP linear expression.

        Supports expressions like: '3*x1 + 2*x2 - x3'
        """
        expr = 0
        if not expression or not expression.strip():
            return expr

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
                try:
                    expr += float(term)
                except ValueError:
                    # Variable with an implicit coefficient of 1 or -1, e.g. "x3" or "-x3".
                    # Strip the leading sign before lookup, otherwise "- x3" would be dropped.
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

    def _get_solver(self, config: SolverConfig) -> pulp.LpSolver:
        """Get the appropriate PuLP solver.

        Tries commercial solvers first (CPLEX, Gurobi), falls back to CBC.
        """
        algorithm = config.algorithm.lower() if config.algorithm else "auto"

        if algorithm in ("cplex", "cplex_cmd"):
            try:
                return pulp.CPLEX_CMD(msg=config.verbose)
            except Exception:
                logger.info("CPLEX not available, falling back to CBC")

        if algorithm in ("gurobi", "gurobi_cmd"):
            try:
                return pulp.GUROBI_CMD(msg=config.verbose)
            except Exception:
                logger.info("Gurobi not available, falling back to CBC")

        # Default: CBC
        return pulp.PULP_CBC_CMD(
            msg=config.verbose,
            timeLimit=config.time_limit if config.time_limit else None,
            gapRel=config.gap_tolerance if config.gap_tolerance else None,
            threads=config.threads if config.threads and config.threads > 0 else None,
        )

    def _map_status(self, status_code: int) -> str:
        """Map PuLP status code to our status string."""
        status_map = {
            pulp.LpStatusOptimal: "optimal",
            pulp.LpStatusNotSolved: "not_solved",
            pulp.LpStatusInfeasible: "infeasible",
            pulp.LpStatusUnbounded: "unbounded",
            pulp.LpStatusUndefined: "error",
        }
        return status_map.get(status_code, "error")
