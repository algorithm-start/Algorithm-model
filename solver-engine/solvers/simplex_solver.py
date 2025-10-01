"""Two-Phase Simplex Method for Linear Programming.

Pure Python implementation using only numpy. No external solver libraries required.
"""

import logging
import time
from typing import Any, Dict, List, Optional

import numpy as np

from models.config import SolverConfig
from models.problem import ObjectiveSense, ProblemDefinition, VariableType
from models.result import SolverResult
from solvers.base import AbstractSolver
from solvers.expression_parser import parse_linear_expression_to_array

logger = logging.getLogger(__name__)


class SimplexSolver(AbstractSolver):
    """Standard Two-Phase Simplex Method for Linear Programming."""

    SUPPORTED_TYPES = {"LP"}

    def solve(
        self,
        problem: ProblemDefinition,
        config: Optional[SolverConfig] = None,
        **kwargs: Any,
    ) -> SolverResult:
        start_time = time.time()
        config = config or SolverConfig()

        try:
            c, constraints, bounds, var_names, var_shifts = (
                self._build_standard_form(problem)
            )

            maximize = (
                problem.objective
                and problem.objective.sense == ObjectiveSense.MAXIMIZE
            )

            status, x, obj_val = self._simplex_solve(
                c, constraints, bounds, maximize, config
            )

            variables = {}
            if x is not None:
                for i, name in enumerate(var_names):
                    variables[name] = float(x[i] + var_shifts[i])

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
            logger.error("Simplex solver failed: %s", str(e), exc_info=True)
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

    # ------------------------------------------------------------------
    # Build standard form
    # ------------------------------------------------------------------

    def _build_standard_form(self, problem: ProblemDefinition):
        """Convert ProblemDefinition to standard LP form.

        Returns:
            c: objective coefficients (for minimization)
            constraints: list of (coeffs, rhs, type) where type is 'le', 'ge', or 'eq'
            bounds: list of (lb, ub) for each variable
            var_names: list of variable names
            var_shifts: array of shifts applied to variables
        """
        n = len(problem.variables)
        if n == 0:
            raise ValueError("Problem has no variables")

        var_names = [v.name for v in problem.variables]
        var_name_to_idx = {name: i for i, name in enumerate(var_names)}

        # Objective
        c = np.zeros(n)
        if problem.objective and problem.objective.expression:
            c = parse_linear_expression_to_array(
                problem.objective.expression, var_name_to_idx, n
            )
            if problem.objective.sense == ObjectiveSense.MAXIMIZE:
                c = -c

        # Bounds — shift variables so lower bound >= 0
        bounds = []
        var_shifts = np.zeros(n)
        for i, v in enumerate(problem.variables):
            lb = v.lower_bound if v.lower_bound is not None else 0.0
            ub = v.upper_bound if v.upper_bound is not None else None
            if lb < 0:
                var_shifts[i] = lb
                lb = 0.0
            bounds.append((lb, ub))

        # Constraints: keep track of type for each
        constraints = []
        for constraint in problem.constraints:
            coeffs = parse_linear_expression_to_array(
                constraint.expression, var_name_to_idx, n
            )
            shift_adj = coeffs @ var_shifts

            lb = constraint.lower_bound
            ub = constraint.upper_bound

            if lb is not None and ub is not None:
                if abs(lb - ub) < 1e-10:
                    # Equality constraint
                    rhs = ub - shift_adj
                    constraints.append((coeffs, rhs, 'eq'))
                else:
                    # Range: lb <= ax <= ub -> ax <= ub and ax >= lb
                    constraints.append((coeffs, ub - shift_adj, 'le'))
                    constraints.append((-coeffs, -(lb - shift_adj), 'le'))
            elif ub is not None:
                constraints.append((coeffs, ub - shift_adj, 'le'))
            elif lb is not None:
                # ax >= lb -> -ax <= -lb
                constraints.append((-coeffs, -(lb - shift_adj), 'le'))
            else:
                logger.warning(
                    "Constraint %s has no bounds, skipping", constraint.name
                )

        return c, constraints, bounds, var_names, var_shifts

    # ------------------------------------------------------------------
    # Internal: simplex algorithm
    # ------------------------------------------------------------------

    def _simplex_solve(
        self,
        c: np.ndarray,
        constraints: list,
        bounds: List,
        maximize: bool,
        config: SolverConfig,
    ):
        """Run two-phase simplex method.

        All constraints are in the form a^T x <= b or a^T x = b.
        Returns: (status, x, objective_value)
        """
        n = len(c)
        max_iter = config.max_iterations or 10000

        if not constraints:
            return self._solve_unconstrained(c, bounds, maximize)

        m = len(constraints)

        # Build tableau with slack and artificial variables
        # For <= constraints: add slack variable (identity column)
        # For =  constraints: add artificial variable
        n_slack = sum(1 for _, _, t in constraints if t == 'le')
        n_artificial = sum(1 for _, _, t in constraints if t == 'eq')
        n_total = n + n_slack + n_artificial

        A_full = np.zeros((m, n_total))
        b_full = np.zeros(m)
        constraint_types = []

        slack_idx = n
        art_idx = n + n_slack

        for i, (coeffs, rhs, ctype) in enumerate(constraints):
            A_full[i, :n] = coeffs
            b_full[i] = rhs
            constraint_types.append(ctype)

            if ctype == 'le':
                A_full[i, slack_idx] = 1.0
                slack_idx += 1
            elif ctype == 'eq':
                A_full[i, art_idx] = 1.0
                art_idx += 1

        # Ensure b >= 0 (multiply rows with negative RHS by -1)
        for i in range(m):
            if b_full[i] < 0:
                A_full[i] *= -1
                b_full[i] *= -1
                # Flip constraint type
                if constraint_types[i] == 'le':
                    # Was: ax + s = -b (negative), after flip: -ax - s = b
                    # Now it's effectively: -ax <= b, which needs a new slack
                    # But we already added slack with +1 coefficient, which is now -1
                    # This means the slack is no longer usable as basic variable
                    # We need an artificial variable instead
                    constraint_types[i] = 'le_neg'
                elif constraint_types[i] == 'eq':
                    pass  # Artificial variable also got flipped, but that's ok

        # Now determine initial basis
        basic = []
        needs_phase1 = False
        slack_idx = n
        art_idx = n + n_slack

        for i in range(m):
            ctype = constraint_types[i]
            if ctype == 'le':
                # Slack variable is basic (coefficient is +1)
                basic.append(slack_idx)
                slack_idx += 1
            elif ctype == 'le_neg':
                # Slack variable has -1 coefficient, need artificial variable
                # Add an artificial variable for this row
                needs_phase1 = True
                slack_idx += 1  # Skip this slack (not usable as basic)
                # We'll use artificial variable
                basic.append(art_idx)
                art_idx += 1
            elif ctype == 'eq':
                basic.append(art_idx)
                art_idx += 1
                needs_phase1 = True

        # Recount artificial variables (some from le_neg rows)
        n_artificial = art_idx - (n + n_slack)
        # Rebuild A_full to include artificial vars for le_neg rows
        if any(ct == 'le_neg' for ct in constraint_types):
            # Need to rebuild with proper artificial variables
            n_extra_art = sum(1 for ct in constraint_types if ct == 'le_neg')
            n_total = n + n_slack + n_artificial  # Already correct
            A_new = np.zeros((m, n_total))
            s_idx = n
            a_idx = n + n_slack
            basic = []

            for i, (coeffs, rhs, ctype) in enumerate(constraints):
                A_new[i, :n] = A_full[i, :n]
                b_full_i = b_full[i]

                if constraint_types[i] == 'le':
                    A_new[i, s_idx] = 1.0
                    basic.append(s_idx)
                    s_idx += 1
                elif constraint_types[i] == 'le_neg':
                    A_new[i, s_idx] = -1.0  # Slack with -1 coefficient
                    A_new[i, a_idx] = 1.0   # Artificial with +1
                    basic.append(a_idx)
                    s_idx += 1
                    a_idx += 1
                elif constraint_types[i] == 'eq':
                    A_new[i, a_idx] = 1.0
                    basic.append(a_idx)
                    a_idx += 1

            A_full = A_new

        if needs_phase1:
            status, basic = self._phase1(A_full, b_full, c, basic, n, n_slack, n_artificial, max_iter, constraint_types)
            if status != "optimal":
                return status, None, None
        else:
            pass  # basic is already set correctly

        # Phase 2: optimize original objective
        status, x_ext, obj_val = self._phase2(A_full, b_full, c, basic, n, n_slack, bounds, max_iter)

        if status not in ("optimal", "feasible"):
            return status, None, None

        x = x_ext[:n]
        obj_val = float(c @ x)
        return "optimal", x, obj_val

    def _phase1(
        self,
        A: np.ndarray,
        b: np.ndarray,
        c: np.ndarray,
        basic: List[int],
        n_orig: int,
        n_slack: int,
        n_artificial: int,
        max_iter: int,
        constraint_types: list,
    ):
        """Phase 1: find initial basic feasible solution using artificial variables."""
        m, n = A.shape
        n_total = n

        # Phase 1 objective: minimize sum of artificial variables
        c_phase1 = np.zeros(n_total)
        for i in range(n_orig + n_slack, n_orig + n_slack + n_artificial):
            c_phase1[i] = 1.0

        # Build tableau
        tableau, basis = self._build_tableau(A, b, c_phase1, basic)

        # Solve
        status, tableau, basis = self._run_simplex(tableau, basis, max_iter)

        if status != "optimal":
            return "infeasible", None

        # Check if all artificial variables are zero
        phase1_obj = tableau[-1, -1]
        if abs(phase1_obj) > 1e-8:
            return "infeasible", None

        # Remove artificial variables from basis if any remain
        new_basic = []
        for b_idx in basis:
            if b_idx < n_orig + n_slack:
                new_basic.append(b_idx)
            else:
                # Artificial variable still in basis with zero value — pivot out
                row = basis.index(b_idx)
                pivoted = False
                for j in range(n_orig + n_slack):
                    if j not in basis and abs(tableau[row, j]) > 1e-10:
                        self._pivot(tableau, basis, row, j)
                        new_basic.append(j)
                        pivoted = True
                        break
                if not pivoted:
                    new_basic.append(b_idx)

        return "optimal", new_basic

    def _phase2(
        self,
        A: np.ndarray,
        b: np.ndarray,
        c: np.ndarray,
        basic: List[int],
        n_orig: int,
        n_slack: int,
        bounds: List,
        max_iter: int,
    ):
        """Phase 2: optimize original objective from the BFS found in Phase 1."""
        m = A.shape[0]
        # Extend c with zeros for slack, large positive for artificial variables
        # (prevents artificial variables from entering basis in Phase 2)
        c_ext = np.zeros(A.shape[1])
        c_ext[:n_orig] = c
        n_art = A.shape[1] - n_orig - n_slack
        if n_art > 0:
            c_ext[n_orig + n_slack:] = 1e10  # Big-M for artificial vars

        tableau, basis = self._build_tableau(A, b, c_ext, basic)
        status, tableau, basis = self._run_simplex(tableau, basis, max_iter)

        if status == "unbounded":
            return "unbounded", None, None
        if status != "optimal":
            return status, None, None

        # Extract solution
        x = np.zeros(A.shape[1])
        for i, b_idx in enumerate(basis):
            x[b_idx] = tableau[i, -1]

        # Apply bounds
        for i in range(n_orig):
            lb, ub = bounds[i]
            if lb is not None:
                x[i] = max(x[i], lb)
            if ub is not None:
                x[i] = min(x[i], ub)

        obj_val = float(c @ x[:n_orig])
        return "optimal", x, obj_val

    def _build_tableau(self, A, b, c, basic):
        """Build the simplex tableau."""
        m, n = A.shape
        tableau = np.zeros((m + 1, n + 1))
        tableau[:m, :n] = A
        tableau[:m, -1] = b
        tableau[-1, :n] = c

        # Eliminate basic variable columns from objective row
        for i, b_idx in enumerate(basic):
            if abs(tableau[i, b_idx]) < 1e-12:
                continue
            factor = tableau[-1, b_idx] / tableau[i, b_idx]
            tableau[-1] -= factor * tableau[i]

        return tableau, list(basic)

    def _run_simplex(self, tableau, basis, max_iter):
        """Run simplex with Bland's rule for anti-cycling."""
        m = len(basis)
        n = tableau.shape[1] - 1

        for iteration in range(max_iter):
            # Find entering variable (Bland's rule)
            pivot_col = -1
            for j in range(n):
                if j in basis:
                    continue
                if tableau[-1, j] < -1e-10:
                    pivot_col = j
                    break

            if pivot_col == -1:
                return "optimal", tableau, basis

            # Minimum ratio test
            pivot_row = -1
            min_ratio = np.inf
            for i in range(m):
                if tableau[i, pivot_col] > 1e-10:
                    ratio = tableau[i, -1] / tableau[i, pivot_col]
                    if ratio < min_ratio - 1e-10:
                        min_ratio = ratio
                        pivot_row = i
                    elif abs(ratio - min_ratio) < 1e-10:
                        if basis[i] < basis[pivot_row]:
                            pivot_row = i

            if pivot_row == -1:
                return "unbounded", tableau, basis

            self._pivot(tableau, basis, pivot_row, pivot_col)

        return "timeout", tableau, basis

    def _pivot(self, tableau, basis, pivot_row, pivot_col):
        """Perform a pivot operation."""
        pivot_val = tableau[pivot_row, pivot_col]
        tableau[pivot_row] /= pivot_val

        for i in range(tableau.shape[0]):
            if i == pivot_row:
                continue
            factor = tableau[i, pivot_col]
            if abs(factor) < 1e-15:
                continue
            tableau[i] -= factor * tableau[pivot_row]

        basis[pivot_row] = pivot_col

    def _solve_unconstrained(self, c, bounds, maximize):
        """Solve an unconstrained LP (no constraints)."""
        n = len(c)
        x = np.zeros(n)
        for i in range(n):
            lb, ub = bounds[i]
            if c[i] > 0:
                x[i] = lb if lb is not None else 0.0
            elif c[i] < 0:
                x[i] = ub if ub is not None else float("inf")
                if x[i] == float("inf"):
                    return "unbounded", None, None
            else:
                x[i] = lb if lb is not None else 0.0

        obj_val = float(c @ x)
        return "optimal", x, obj_val
