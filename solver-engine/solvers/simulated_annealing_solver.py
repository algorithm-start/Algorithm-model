"""Simulated Annealing for general optimization problems.

Standard SA with Metropolis acceptance criterion, Gaussian perturbation,
and geometric cooling schedule. Supports multiple restarts.
Pure Python implementation using only numpy.
"""

import logging
import math
import time
from typing import Any, Dict, List, Optional

import numpy as np

from models.config import SolverConfig
from models.problem import ObjectiveSense, ProblemDefinition, VariableType
from models.result import SolverResult
from solvers.base import AbstractSolver
from solvers.expression_parser import evaluate_expression

logger = logging.getLogger(__name__)


class SimulatedAnnealingSolver(AbstractSolver):
    """Simulated Annealing for general optimization problems."""

    SUPPORTED_TYPES = {"LP", "MILP", "NLP", "MINLP"}

    def solve(
        self,
        problem: ProblemDefinition,
        config: Optional[SolverConfig] = None,
        **kwargs: Any,
    ) -> SolverResult:
        start_time = time.time()
        config = config or SolverConfig()

        try:
            # Parse custom parameters
            custom = kwargs.get("custom_params", {}) or {}
            init_temp = float(custom.get("initial_temperature", 1000.0))
            cooling_rate = float(custom.get("cooling_rate", 0.995))
            max_iter = int(custom.get("max_iterations", 10000))
            min_temp = float(custom.get("min_temperature", 1e-8))
            num_restarts = int(custom.get("restarts", 3))

            if config.seed is not None:
                np.random.seed(config.seed)

            result = self._anneal(
                problem, config, init_temp, cooling_rate, max_iter,
                min_temp, num_restarts, start_time,
            )
            result.solve_time = time.time() - start_time
            return result

        except Exception as e:
            logger.error(
                "Simulated Annealing solver failed: %s", str(e), exc_info=True
            )
            return SolverResult(
                status="error",
                solve_time=time.time() - start_time,
                error_message=str(e),
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        return True

    def cancel(self, job_id: str) -> bool:
        return False

    # ------------------------------------------------------------------
    # SA algorithm
    # ------------------------------------------------------------------

    def _anneal(
        self,
        problem: ProblemDefinition,
        config: SolverConfig,
        init_temp: float,
        cooling_rate: float,
        max_iter: int,
        min_temp: float,
        num_restarts: int,
        start_time: float,
    ) -> SolverResult:
        n = len(problem.variables)
        if n == 0:
            return SolverResult(status="error", error_message="No variables")

        var_name_to_idx = {v.name: i for i, v in enumerate(problem.variables)}
        var_names = [v.name for v in problem.variables]
        maximize = (
            problem.objective
            and problem.objective.sense == ObjectiveSense.MAXIMIZE
        )

        lb = np.array(
            [v.lower_bound if v.lower_bound is not None else -1e6 for v in problem.variables]
        )
        ub = np.array(
            [v.upper_bound if v.upper_bound is not None else 1e6 for v in problem.variables]
        )
        var_types = [v.var_type for v in problem.variables]

        # Scale factors for perturbation (proportional to variable range)
        scale = (ub - lb) * 0.1
        scale = np.maximum(scale, 1e-6)

        best_solution = None
        best_obj = None

        for restart in range(num_restarts):
            if config.time_limit and (time.time() - start_time) > config.time_limit:
                break

            # Random initial solution
            current = self._random_solution(n, lb, ub, var_types)
            current_obj = self._eval_objective(current, problem, var_name_to_idx)
            current_penalty = self._eval_penalty(current, problem, var_name_to_idx)
            current_energy = self._total_energy(current_obj, current_penalty, maximize)

            restart_best = current.copy()
            restart_best_obj = current_obj
            restart_best_penalty = current_penalty
            restart_best_energy = current_energy

            temp = init_temp

            for iteration in range(max_iter):
                if config.time_limit and (time.time() - start_time) > config.time_limit:
                    break

                # Generate neighbor
                neighbor = self._perturb(current, lb, ub, var_types, scale)
                neighbor_obj = self._eval_objective(neighbor, problem, var_name_to_idx)
                neighbor_penalty = self._eval_penalty(neighbor, problem, var_name_to_idx)
                neighbor_energy = self._total_energy(neighbor_obj, neighbor_penalty, maximize)

                # Metropolis acceptance criterion
                delta = neighbor_energy - current_energy
                # For minimization: accept if delta < 0 (improvement)
                # For maximization (energy = -obj + penalty): accept if delta < 0
                accept = False
                if delta < 0:
                    accept = True
                elif temp > 1e-15:
                    probability = math.exp(-delta / temp)
                    if np.random.random() < probability:
                        accept = True

                if accept:
                    current = neighbor
                    current_obj = neighbor_obj
                    current_penalty = neighbor_penalty
                    current_energy = neighbor_energy

                    # Track best feasible solution for this restart
                    if current_penalty < 1e-6:  # Feasible
                        if restart_best_penalty >= 1e-6:  # Previous was infeasible
                            restart_best = current.copy()
                            restart_best_obj = current_obj
                            restart_best_penalty = current_penalty
                            restart_best_energy = current_energy
                        elif maximize and current_obj > restart_best_obj:
                            restart_best = current.copy()
                            restart_best_obj = current_obj
                            restart_best_penalty = current_penalty
                            restart_best_energy = current_energy
                        elif not maximize and current_obj < restart_best_obj:
                            restart_best = current.copy()
                            restart_best_obj = current_obj
                            restart_best_penalty = current_penalty
                            restart_best_energy = current_energy

                # Cool down
                temp *= cooling_rate
                if temp < min_temp:
                    break

            # Update global best from this restart
            if restart_best_penalty < 1e-6:
                if best_obj is None:
                    best_solution = restart_best
                    best_obj = restart_best_obj
                elif maximize and restart_best_obj > best_obj:
                    best_solution = restart_best
                    best_obj = restart_best_obj
                elif not maximize and restart_best_obj < best_obj:
                    best_solution = restart_best
                    best_obj = restart_best_obj

        if best_solution is None:
            return SolverResult(
                status="error",
                error_message="No feasible solution found",
            )

        # Build result
        variables = {}
        for i, name in enumerate(var_names):
            variables[name] = float(self._enforce_type(best_solution[i], var_types[i]))

        return SolverResult(
            status="optimal",
            objective_value=float(best_obj),
            variables=variables,
        )

    # ------------------------------------------------------------------
    # Helper methods
    # ------------------------------------------------------------------

    def _random_solution(
        self,
        n: int,
        lb: np.ndarray,
        ub: np.ndarray,
        var_types: List[VariableType],
    ) -> np.ndarray:
        """Generate a random feasible solution within bounds."""
        x = np.random.uniform(lb, ub, size=n)
        for j in range(n):
            if var_types[j] == VariableType.BINARY:
                x[j] = np.random.randint(0, 2)
            elif var_types[j] == VariableType.INTEGER:
                x[j] = np.random.randint(
                    int(np.floor(lb[j])), int(np.ceil(ub[j])) + 1
                )
        return x

    def _perturb(
        self,
        current: np.ndarray,
        lb: np.ndarray,
        ub: np.ndarray,
        var_types: List[VariableType],
        scale: np.ndarray,
    ) -> np.ndarray:
        """Generate a neighbor solution by perturbation."""
        neighbor = current.copy()
        # Perturb a subset of variables
        n = len(current)
        num_perturb = max(1, n // 3)
        indices = np.random.choice(n, size=num_perturb, replace=False)

        for idx in indices:
            if var_types[idx] == VariableType.BINARY:
                neighbor[idx] = 1.0 - neighbor[idx]  # Flip
            elif var_types[idx] == VariableType.INTEGER:
                # Uniform random integer perturbation
                perturbation = np.random.randint(-max(1, int(scale[idx])),
                                                  max(1, int(scale[idx])) + 1)
                neighbor[idx] = np.clip(neighbor[idx] + perturbation, lb[idx], ub[idx])
            else:
                # Gaussian perturbation
                neighbor[idx] += np.random.normal(0, scale[idx])
                neighbor[idx] = np.clip(neighbor[idx], lb[idx], ub[idx])

        return neighbor

    def _eval_objective(
        self,
        x: np.ndarray,
        problem: ProblemDefinition,
        var_name_to_idx: Dict[str, int],
    ) -> float:
        """Evaluate the objective function."""
        if not problem.objective or not problem.objective.expression:
            return 0.0
        return evaluate_expression(
            problem.objective.expression, var_name_to_idx, x
        )

    def _eval_penalty(
        self,
        x: np.ndarray,
        problem: ProblemDefinition,
        var_name_to_idx: Dict[str, int],
    ) -> float:
        """Evaluate constraint violation penalty."""
        penalty = 0.0
        penalty_coeff = 1e6

        for constraint in problem.constraints:
            val = evaluate_expression(
                constraint.expression, var_name_to_idx, x
            )
            if constraint.upper_bound is not None and val > constraint.upper_bound:
                penalty += penalty_coeff * (val - constraint.upper_bound)
            if constraint.lower_bound is not None and val < constraint.lower_bound:
                penalty += penalty_coeff * (constraint.lower_bound - val)

        return penalty

    def _total_energy(
        self,
        obj: float,
        penalty: float,
        maximize: bool,
    ) -> float:
        """Compute total energy for SA (lower is better).

        For minimization: energy = obj + penalty
        For maximization: energy = -obj + penalty
        """
        if maximize:
            return -obj + penalty
        else:
            return obj + penalty

    def _enforce_type(self, val: float, var_type: VariableType) -> float:
        """Enforce variable type."""
        if var_type == VariableType.BINARY:
            return 1.0 if val > 0.5 else 0.0
        elif var_type == VariableType.INTEGER:
            return float(np.round(val))
        return val
