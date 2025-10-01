"""Branch and Bound Method for Mixed Integer Linear Programming.

Uses the built-in SimplexSolver for LP relaxation subproblems.
Pure Python implementation using only numpy.
"""

import logging
import time
from typing import Any, Dict, List, Optional, Tuple

import numpy as np

from models.config import SolverConfig
from models.problem import (
    ObjectiveSense,
    ProblemDefinition,
    Variable,
    VariableType,
)
from models.result import SolverResult
from solvers.base import AbstractSolver

logger = logging.getLogger(__name__)


class BranchBoundSolver(AbstractSolver):
    """Branch and Bound for Mixed Integer Linear Programming."""

    SUPPORTED_TYPES = {"MILP"}

    def solve(
        self,
        problem: ProblemDefinition,
        config: Optional[SolverConfig] = None,
        **kwargs: Any,
    ) -> SolverResult:
        self._start_time = time.time()
        config = config or SolverConfig()

        try:
            result = self._branch_and_bound(problem, config)
            result.solve_time = time.time() - self._start_time
            return result

        except Exception as e:
            logger.error(
                "Branch and Bound solver failed: %s", str(e), exc_info=True
            )
            return SolverResult(
                status="error",
                solve_time=time.time() - self._start_time,
                error_message=str(e),
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        has_integer = any(
            v.var_type in (VariableType.INTEGER, VariableType.BINARY)
            for v in problem.variables
        )
        if not has_integer:
            return False
        # Check for nonlinear
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
    # Internal: Branch and Bound
    # ------------------------------------------------------------------

    def _branch_and_bound(
        self, problem: ProblemDefinition, config: SolverConfig
    ) -> SolverResult:
        from solvers.simplex_solver import SimplexSolver

        simplex = SimplexSolver()

        maximize = (
            problem.objective
            and problem.objective.sense == ObjectiveSense.MAXIMIZE
        )

        # Identify integer/binary variables
        int_var_indices = []
        for i, v in enumerate(problem.variables):
            if v.var_type in (VariableType.INTEGER, VariableType.BINARY):
                int_var_indices.append(i)

        if not int_var_indices:
            # Pure LP — just solve with simplex
            return simplex.solve(problem, config)

        max_nodes = config.max_iterations or 10000
        node_count = 0

        # Best incumbent
        best_obj = None
        best_vars: Dict[str, float] = {}

        # Stack for DFS: each entry is a list of extra bounds (var_idx, lb, ub)
        stack: List[List[Tuple[int, Optional[float], Optional[float]]]] = [[]]

        while stack and node_count < max_nodes:
            if config.time_limit and (time.time() - self._start_time) > config.time_limit:
                break

            bounds_additions = stack.pop()
            node_count += 1

            # Build LP relaxation with additional bounds
            relaxed = self._build_relaxation(problem, bounds_additions)

            lp_result = simplex.solve(relaxed, config)

            if lp_result.status not in ("optimal", "feasible"):
                continue  # Infeasible or error -> prune

            lp_obj = lp_result.objective_value
            if lp_obj is None:
                continue

            # Pruning: if LP relaxation is worse than best incumbent
            if best_obj is not None:
                if maximize and lp_obj <= best_obj + config.gap_tolerance:
                    continue
                if not maximize and lp_obj >= best_obj - config.gap_tolerance:
                    continue

            # Check if solution is integer-feasible
            frac_var = self._find_branching_variable(
                lp_result.variables, problem, int_var_indices
            )

            if frac_var is None:
                # Integer feasible -> update incumbent
                if best_obj is None:
                    best_obj = lp_obj
                    best_vars = dict(lp_result.variables)
                else:
                    if maximize and lp_obj > best_obj:
                        best_obj = lp_obj
                        best_vars = dict(lp_result.variables)
                    elif not maximize and lp_obj < best_obj:
                        best_obj = lp_obj
                        best_vars = dict(lp_result.variables)
                continue

            # Branch on frac_var
            var_idx, frac_val = frac_var

            floor_val = np.floor(frac_val)
            ceil_val = np.ceil(frac_val)

            # For binary variables, ensure bounds are 0 or 1
            if problem.variables[var_idx].var_type == VariableType.BINARY:
                floor_val = 0.0
                ceil_val = 1.0

            # Branch: x <= floor_val
            stack.append(bounds_additions + [(var_idx, None, floor_val)])

            # Branch: x >= ceil_val
            stack.append(bounds_additions + [(var_idx, ceil_val, None)])

        status = "optimal" if best_obj is not None else "infeasible"
        if node_count >= max_nodes and best_obj is not None:
            status = "feasible"

        return SolverResult(
            status=status,
            objective_value=best_obj,
            variables=best_vars,
        )

    def _build_relaxation(
        self,
        problem: ProblemDefinition,
        extra_bounds: List[Tuple[int, Optional[float], Optional[float]]],
    ) -> ProblemDefinition:
        """Build LP relaxation with additional variable bounds."""
        new_vars = []
        for i, v in enumerate(problem.variables):
            new_lb = v.lower_bound
            new_ub = v.upper_bound

            for var_idx, lb, ub in extra_bounds:
                if var_idx == i:
                    if lb is not None:
                        new_lb = max(new_lb, lb) if new_lb is not None else lb
                    if ub is not None:
                        new_ub = min(new_ub, ub) if new_ub is not None else ub

            new_vars.append(
                Variable(
                    name=v.name,
                    lower_bound=new_lb if new_lb is not None else 0.0,
                    upper_bound=new_ub,
                    var_type=VariableType.CONTINUOUS,  # Relax to continuous
                )
            )

        return ProblemDefinition(
            name=problem.name + "_relax",
            variables=new_vars,
            constraints=problem.constraints,
            objective=problem.objective,
            metadata=problem.metadata,
        )

    def _find_branching_variable(
        self,
        variables: Dict[str, float],
        problem: ProblemDefinition,
        int_var_indices: List[int],
    ) -> Optional[Tuple[int, float]]:
        """Find the integer variable with the largest fractional part.

        Returns: (var_index, value) or None if all integer vars are integral.
        """
        best_idx = None
        best_frac = 0.0

        for idx in int_var_indices:
            var_name = problem.variables[idx].name
            val = variables.get(var_name, 0.0)
            frac = val - np.floor(val)
            # Choose variable closest to 0.5 (most fractional)
            frac_dist = abs(frac - 0.5)
            if frac_dist < 0.5 - 1e-6:  # Has fractional part
                if best_idx is None or frac_dist < abs(best_frac - 0.5):
                    best_idx = idx
                    best_frac = frac

        if best_idx is None:
            return None
        return best_idx, variables.get(problem.variables[best_idx].name, 0.0)
