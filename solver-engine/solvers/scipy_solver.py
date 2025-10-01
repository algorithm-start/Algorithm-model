"""SciPy solver adapter for LP and nonlinear problems."""

import logging
import time
from typing import Any, Dict, Optional

import numpy as np
from scipy.optimize import linprog, minimize

from models.config import SolverConfig
from models.problem import ObjectiveSense, ProblemDefinition, VariableType
from models.result import SolverResult
from solvers.base import AbstractSolver

logger = logging.getLogger(__name__)


class SciPySolver(AbstractSolver):
    """SciPy solver supporting LP (via linprog) and nonlinear (via minimize) problems."""

    SUPPORTED_TYPES = {"LP", "NLP"}

    def solve(self, problem: ProblemDefinition, config: Optional[SolverConfig] = None, **kwargs: Any) -> SolverResult:
        """Solve the given optimization problem using SciPy.

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
            # Determine problem type and select method
            is_linear = self._is_linear_problem(problem)
            has_integer = any(
                v.var_type in (VariableType.INTEGER, VariableType.BINARY)
                for v in problem.variables
            )

            if has_integer:
                logger.warning("SciPy solver does not support integer variables natively; relaxing to continuous")

            if is_linear:
                result = self._solve_lp(problem, config)
            else:
                result = self._solve_nonlinear(problem, config)

            result.solve_time = time.time() - start_time
            return result

        except Exception as e:
            logger.error("SciPy solver failed: %s", str(e), exc_info=True)
            return SolverResult(
                status="error",
                solve_time=time.time() - start_time,
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        """Check if this solver supports the given problem type.

        SciPy is best for continuous problems (LP, nonlinear).
        It can handle integer variables by relaxing them.
        """
        return True

    def cancel(self, job_id: str) -> bool:
        """Cancel a running solve job. SciPy does not support async cancellation."""
        logger.warning("SciPy does not support async cancellation for job %s", job_id)
        return False

    def _is_linear_problem(self, problem: ProblemDefinition) -> bool:
        """Check if the problem is purely linear."""
        nonlinear_keywords = ["sin", "cos", "exp", "log", "sqrt", "**", "^"]
        if problem.objective and problem.objective.expression:
            expr = problem.objective.expression
            if any(kw in expr for kw in nonlinear_keywords):
                return False
        for c in problem.constraints:
            if c.expression and any(kw in c.expression for kw in nonlinear_keywords):
                return False
        return True

    def _solve_lp(self, problem: ProblemDefinition, config: SolverConfig) -> SolverResult:
        """Solve a linear programming problem using scipy.optimize.linprog.

        Problem format for linprog:
            minimize c^T x
            subject to A_ub @ x <= b_ub
                       A_eq @ x == b_eq
                       bounds
        """
        num_vars = len(problem.variables)
        if num_vars == 0:
            return SolverResult(status="error")

        var_name_to_idx = {v.name: i for i, v in enumerate(problem.variables)}

        # Objective coefficients
        c = np.zeros(num_vars)
        maximize = False
        if problem.objective and problem.objective.expression:
            c = self._parse_expression_coeffs(problem.objective.expression, var_name_to_idx, num_vars)
            if problem.objective.sense == ObjectiveSense.MAXIMIZE:
                maximize = True
                c = -c  # linprog minimizes, so negate for maximization

        # Bounds
        bounds = []
        for var_def in problem.variables:
            lb = var_def.lower_bound if var_def.lower_bound is not None else 0.0
            ub = var_def.upper_bound if var_def.upper_bound is not None else None
            bounds.append((lb, ub))

        # Constraints
        A_ub = []
        b_ub = []
        A_eq = []
        b_eq = []

        for constraint_def in problem.constraints:
            coeffs = self._parse_expression_coeffs(
                constraint_def.expression, var_name_to_idx, num_vars
            )

            lb = constraint_def.lower_bound
            ub = constraint_def.upper_bound

            if lb is not None and ub is not None:
                # Range constraint: lb <= ax <= ub
                # -> ax <= ub and -ax <= -lb
                A_ub.append(coeffs)
                b_ub.append(ub)
                A_ub.append(-coeffs)
                b_ub.append(-lb)
            elif ub is not None:
                # Upper bound: ax <= ub
                A_ub.append(coeffs)
                b_ub.append(ub)
            elif lb is not None:
                # Lower bound: ax >= lb -> -ax <= -lb
                A_ub.append(-coeffs)
                b_ub.append(-lb)
            else:
                logger.warning("Constraint %s has no bounds, skipping", constraint_def.name)

        # Solve
        method = "highs"  # Default to HiGHS backend
        options = {}
        if config.time_limit:
            options["time_limit"] = config.time_limit
        if config.max_iterations:
            options["maxiter"] = config.max_iterations

        result = linprog(
            c,
            A_ub=np.array(A_ub) if A_ub else None,
            b_ub=np.array(b_ub) if b_ub else None,
            A_eq=np.array(A_eq) if A_eq else None,
            b_eq=np.array(b_eq) if b_eq else None,
            bounds=bounds,
            method=method,
            options=options or None,
        )

        # Map status
        status_map = {0: "optimal", 1: "timeout", 2: "infeasible", 3: "unbounded", 4: "error"}
        status = status_map.get(result.status, "error")

        objective_value = None
        variables = {}
        if result.success:
            objective_value = float(-result.fun) if maximize else float(result.fun)
            for i, var_def in enumerate(problem.variables):
                variables[var_def.name] = float(result.x[i])

        return SolverResult(
            status=status,
            objective_value=objective_value,
            variables=variables,
        )

    def _solve_nonlinear(self, problem: ProblemDefinition, config: SolverConfig) -> SolverResult:
        """Solve a nonlinear problem using scipy.optimize.minimize."""
        num_vars = len(problem.variables)
        if num_vars == 0:
            return SolverResult(status="error")

        var_name_to_idx = {v.name: i for i, v in enumerate(problem.variables)}

        # Initial guess
        x0 = np.zeros(num_vars)
        for i, var_def in enumerate(problem.variables):
            lb = var_def.lower_bound if var_def.lower_bound is not None else 0.0
            x0[i] = max(lb, 0.0)

        # Bounds
        bounds = []
        for var_def in problem.variables:
            lb = var_def.lower_bound if var_def.lower_bound is not None else None
            ub = var_def.upper_bound if var_def.upper_bound is not None else None
            bounds.append((lb, ub))

        # Objective function
        def objective_func(x):
            return self._evaluate_expression(problem.objective.expression, var_name_to_idx, x)

        # Constraints for scipy.optimize.minimize
        constraints = []
        for constraint_def in problem.constraints:
            lb = constraint_def.lower_bound
            ub = constraint_def.upper_bound

            if ub is not None:
                constraints.append({
                    "type": "ineq",
                    "fun": lambda x, expr=constraint_def.expression, idx=var_name_to_idx, b=ub: b - self._evaluate_expression(expr, idx, x),
                })
            if lb is not None:
                constraints.append({
                    "type": "ineq",
                    "fun": lambda x, expr=constraint_def.expression, idx=var_name_to_idx, b=lb: self._evaluate_expression(expr, idx, x) - b,
                })

        # Select method
        method = "SLSQP"  # Supports bounds and constraints

        options = {}
        if config.max_iterations:
            options["maxiter"] = config.max_iterations

        result = minimize(
            objective_func,
            x0,
            method=method,
            bounds=bounds,
            constraints=constraints or None,
            options=options or None,
        )

        status = "optimal" if result.success else "error"
        objective_value = float(result.fun)
        if problem.objective and problem.objective.sense == ObjectiveSense.MAXIMIZE:
            objective_value = -objective_value

        variables = {}
        for i, var_def in enumerate(problem.variables):
            variables[var_def.name] = float(result.x[i])

        return SolverResult(
            status=status,
            objective_value=objective_value,
            variables=variables,
        )

    def _parse_expression_coeffs(self, expression: str, var_name_to_idx: Dict, num_vars: int) -> np.ndarray:
        """Parse a linear expression string into coefficient array."""
        coeffs = np.zeros(num_vars)
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
                try:
                    float(term)  # Constant term
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
                    if name_token in var_name_to_idx:
                        coeffs[var_name_to_idx[name_token]] += sign

        return coeffs

    def _evaluate_expression(self, expression: str, var_name_to_idx: Dict, x: np.ndarray) -> float:
        """Evaluate an expression with given variable values.

        Supports simple linear and some nonlinear expressions.
        """
        if not expression or not expression.strip():
            return 0.0

        # Create variable namespace
        namespace = {name: x[idx] for name, idx in var_name_to_idx.items()}
        namespace["__builtins__"] = {}

        # Add math functions for nonlinear expressions
        import math
        for func_name in ["sin", "cos", "exp", "log", "sqrt", "abs", "pow"]:
            namespace[func_name] = getattr(math, func_name)

        try:
            # Sanitize: only allow safe operations
            result = eval(expression, namespace)
            return float(result)
        except Exception as e:
            logger.warning("Failed to evaluate expression '%s': %s", expression, str(e))
            return 0.0
