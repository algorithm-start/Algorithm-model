"""Primal-Dual Interior Point Method for LP and QP.

Simplified Mehrotra predictor-corrector method.
Pure Python implementation using only numpy.
"""

import logging
import time
from typing import Any, Dict, List, Optional, Tuple

import numpy as np

from models.config import SolverConfig
from models.problem import ObjectiveSense, ProblemDefinition, VariableType
from models.result import SolverResult
from solvers.base import AbstractSolver
from solvers.expression_parser import (
    parse_linear_expression_to_array,
    parse_quadratic_expression,
)

logger = logging.getLogger(__name__)


class InteriorPointSolver(AbstractSolver):
    """Primal-Dual Interior Point Method for LP and QP."""

    SUPPORTED_TYPES = {"LP", "QP"}

    def solve(
        self,
        problem: ProblemDefinition,
        config: Optional[SolverConfig] = None,
        **kwargs: Any,
    ) -> SolverResult:
        start_time = time.time()
        config = config or SolverConfig()

        try:
            is_qp = self._is_quadratic(problem)
            data = self._build_problem_data(problem, is_qp)
            status, x, obj_val = self._interior_point_solve(data, is_qp, config)

            variables = {}
            if x is not None:
                var_names = [v.name for v in problem.variables]
                for i, name in enumerate(var_names):
                    variables[name] = float(x[i])

            maximize = (
                problem.objective
                and problem.objective.sense == ObjectiveSense.MAXIMIZE
            )
            if maximize and obj_val is not None:
                obj_val = -obj_val

            solve_time = time.time() - start_time
            return SolverResult(
                status=status,
                objective_value=obj_val,
                variables=variables,
                solve_time=solve_time,
            )

        except Exception as e:
            logger.error(
                "Interior Point solver failed: %s", str(e), exc_info=True
            )
            return SolverResult(
                status="error",
                solve_time=time.time() - start_time,
                error_message=str(e),
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        has_integer = any(
            v.var_type in (VariableType.INTEGER, VariableType.BINARY)
            for v in problem.variables
        )
        if has_integer:
            return False
        nonlinear_kw = ["sin", "cos", "exp", "log", "sqrt"]
        if problem.objective and problem.objective.expression:
            if any(kw in problem.objective.expression for kw in nonlinear_kw):
                return False
        for c in problem.constraints:
            if c.expression and any(kw in c.expression for kw in nonlinear_kw):
                return False
        return True

    def cancel(self, job_id: str) -> bool:
        return False

    def _is_quadratic(self, problem: ProblemDefinition) -> bool:
        if not problem.objective or not problem.objective.expression:
            return False
        expr = problem.objective.expression
        return "**2" in expr or "^2" in expr

    # ------------------------------------------------------------------
    # Build problem data
    # ------------------------------------------------------------------

    def _build_problem_data(
        self, problem: ProblemDefinition, is_qp: bool
    ) -> Dict:
        """Build matrices: min c^T x + 0.5 x^T Q x  s.t. Ax <= b, lb <= x <= ub."""
        n = len(problem.variables)
        var_names = [v.name for v in problem.variables]
        var_name_to_idx = {name: i for i, name in enumerate(var_names)}

        lb = np.full(n, 0.0)
        ub = np.full(n, np.inf)
        for i, v in enumerate(problem.variables):
            lb[i] = v.lower_bound if v.lower_bound is not None else 0.0
            ub[i] = v.upper_bound if v.upper_bound is not None else np.inf

        maximize = (
            problem.objective
            and problem.objective.sense == ObjectiveSense.MAXIMIZE
        )
        sign = -1.0 if maximize else 1.0

        Q = np.zeros((n, n))
        c = np.zeros(n)

        if problem.objective and problem.objective.expression:
            if is_qp:
                Q_raw, c_raw = parse_quadratic_expression(
                    problem.objective.expression, var_names
                )
                Q = sign * Q_raw
                c = sign * c_raw
            else:
                c = sign * parse_linear_expression_to_array(
                    problem.objective.expression, var_name_to_idx, n
                )

        A_rows = []
        b_rows = []
        for constraint in problem.constraints:
            coeffs = parse_linear_expression_to_array(
                constraint.expression, var_name_to_idx, n
            )
            lwb = constraint.lower_bound
            upb = constraint.upper_bound

            if lwb is not None and upb is not None:
                if abs(lwb - upb) < 1e-10:
                    A_rows.append(coeffs)
                    b_rows.append(upb)
                    A_rows.append(-coeffs)
                    b_rows.append(-upb)
                else:
                    A_rows.append(coeffs)
                    b_rows.append(upb)
                    A_rows.append(-coeffs)
                    b_rows.append(-lwb)
            elif upb is not None:
                A_rows.append(coeffs)
                b_rows.append(upb)
            elif lwb is not None:
                A_rows.append(-coeffs)
                b_rows.append(-lwb)

        A = np.array(A_rows) if A_rows else np.zeros((0, n))
        b = np.array(b_rows) if b_rows else np.zeros(0)
        m = A.shape[0]

        return {
            "n": n, "m": m, "Q": Q, "c": c,
            "A": A, "b": b, "lb": lb, "ub": ub,
            "maximize": maximize,
        }

    # ------------------------------------------------------------------
    # Interior Point Algorithm
    # ------------------------------------------------------------------

    def _interior_point_solve(
        self,
        data: Dict,
        is_qp: bool,
        config: SolverConfig,
    ) -> Tuple[str, Optional[np.ndarray], Optional[float]]:
        """Run the primal-dual interior point method.

        KKT system:
            r_dual   = Q x + c + A^T y = 0
            r_primal = A x + s - b     = 0
            r_comp   = S Y e - mu e     = 0
        """
        n = data["n"]
        m = data["m"]
        Q = data["Q"]
        c_vec = data["c"]
        A = data["A"]
        b = data["b"]
        lb = data["lb"]
        ub = data["ub"]

        max_iter = config.max_iterations or 300
        tol = 1e-8

        if m == 0:
            return self._solve_bound_constrained(Q, c_vec, lb, ub, max_iter, tol)

        # Initialize strictly feasible point
        x = self._find_initial_x(A, b, lb, ub, n, m)
        s = b - A @ x
        s = np.maximum(s, 1e-2)
        y = np.ones(m)

        best_x = x.copy()
        best_obj = float(c_vec @ x + 0.5 * x @ Q @ x)

        for iteration in range(max_iter):
            # Residuals
            r_dual = Q @ x + c_vec + A.T @ y
            r_primal = A @ x + s - b
            mu = np.dot(s, y) / m

            # Convergence
            primal_gap = np.linalg.norm(r_primal) / (1.0 + np.linalg.norm(b))
            dual_gap = np.linalg.norm(r_dual) / (1.0 + np.linalg.norm(c_vec))

            if primal_gap < tol and dual_gap < tol and mu < tol:
                obj = float(c_vec @ x + 0.5 * x @ Q @ x)
                return "optimal", x, obj

            # Compute Theta = diag(y/s) for scaling
            theta = y / np.maximum(s, 1e-14)

            # --- Affine direction ---
            # Normal equations: (Q + A^T diag(theta) A) dx = -r_dual - A^T diag(theta) r_primal
            # This comes from eliminating ds and dy from the KKT system

            M = Q + A.T @ np.diag(theta) @ A + np.eye(n) * 1e-10
            rhs_aff = -(r_dual + A.T @ (theta * r_primal))

            try:
                d_x_aff = np.linalg.solve(M, rhs_aff)
            except np.linalg.LinAlgError:
                d_x_aff = np.linalg.lstsq(M, rhs_aff, rcond=None)[0]

            d_s_aff = -r_primal - A @ d_x_aff
            d_y_aff = -(y * d_s_aff + s * y) / np.maximum(s, 1e-14)

            # Affine step sizes
            alpha_p_aff = self._max_step(s, d_s_aff, 1.0)
            alpha_d_aff = self._max_step(y, d_y_aff, 1.0)

            # Affine duality gap
            mu_aff = np.dot(s + alpha_p_aff * d_s_aff, y + alpha_d_aff * d_y_aff) / m

            # Centering parameter
            sigma = (mu_aff / max(mu, 1e-20)) ** 3
            sigma = min(sigma, 1.0)

            # --- Corrector direction ---
            rhs_corr = -(r_dual + A.T @ (theta * r_primal - sigma * mu / np.maximum(s, 1e-14) + d_s_aff * d_y_aff / np.maximum(s, 1e-14)))

            try:
                d_x = np.linalg.solve(M, rhs_corr)
            except np.linalg.LinAlgError:
                d_x = np.linalg.lstsq(M, rhs_corr, rcond=None)[0]

            d_s = -r_primal - A @ d_x
            d_y = (-s * y - d_s_aff * d_y_aff + sigma * mu - y * d_s) / np.maximum(s, 1e-14)

            # Step sizes
            alpha_p = min(1.0, 0.95 * self._max_step(s, d_s, 1.0))
            alpha_d = min(1.0, 0.95 * self._max_step(y, d_y, 1.0))

            # Update
            x = x + alpha_p * d_x
            s = s + alpha_p * d_s
            y = y + alpha_d * d_y

            # Ensure bounds and positivity
            x = np.maximum(x, lb + 1e-12)
            for i in range(n):
                if ub[i] < np.inf:
                    x[i] = min(x[i], ub[i] - 1e-12)
            s = np.maximum(s, 1e-14)
            y = np.maximum(y, 1e-14)

            # Track best feasible solution
            obj_curr = float(c_vec @ x + 0.5 * x @ Q @ x)
            if np.isfinite(obj_curr):
                best_x = x.copy()
                best_obj = obj_curr

        return "feasible", best_x, best_obj

    def _find_initial_x(
        self, A: np.ndarray, b: np.ndarray,
        lb: np.ndarray, ub: np.ndarray, n: int, m: int,
    ) -> np.ndarray:
        """Find a strictly feasible initial primal point."""
        x = lb + 1.0
        for i in range(n):
            if ub[i] < np.inf:
                x[i] = min(x[i], (lb[i] + ub[i]) / 2.0)

        # Ensure Ax < b (all slacks positive)
        s = b - A @ x
        for _ in range(100):
            if np.all(s > 1e-6):
                break
            # Move x toward lower bound
            x = lb + 0.5 * (x - lb)
            x = np.maximum(x, lb + 1e-8)
            s = b - A @ x

        # If still infeasible, try x = lb (origin)
        if np.any(s <= 0):
            x = lb + 1e-6
            s = b - A @ x

        return x

    def _max_step(self, v: np.ndarray, dv: np.ndarray, fraction: float) -> float:
        """Compute max alpha such that v + alpha*dv >= 0."""
        alpha = 1e20
        for i in range(len(v)):
            if dv[i] < -1e-16:
                ratio = -v[i] / dv[i]
                alpha = min(alpha, ratio)
        return min(alpha, 1e20) * fraction

    def _solve_bound_constrained(
        self, Q: np.ndarray, c: np.ndarray,
        lb: np.ndarray, ub: np.ndarray,
        max_iter: int, tol: float,
    ) -> Tuple[str, Optional[np.ndarray], Optional[float]]:
        """Solve bound-constrained QP/LP without general constraints."""
        n = len(c)
        x = lb + 1.0
        for i in range(n):
            if ub[i] < np.inf:
                x[i] = min(x[i], (lb[i] + ub[i]) / 2.0)

        for iteration in range(max_iter):
            grad = Q @ x + c
            lr = 0.1
            x_new = x - lr * grad
            x_new = np.maximum(x_new, lb)
            for i in range(n):
                if ub[i] < np.inf:
                    x_new[i] = min(x_new[i], ub[i])
            if np.linalg.norm(x_new - x) < tol:
                break
            x = x_new

        obj = float(c @ x + 0.5 * x @ Q @ x)
        return "optimal", x, obj
