"""Genetic Algorithm for general optimization problems.

Standard GA with tournament selection, SBX crossover, and polynomial mutation.
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
from solvers.expression_parser import evaluate_expression

logger = logging.getLogger(__name__)


class GeneticAlgorithmSolver(AbstractSolver):
    """Genetic Algorithm for general optimization problems."""

    SUPPORTED_TYPES = {"LP", "MILP", "NLP", "MINLP"}

    def solve(
        self,
        problem: ProblemDefinition,
        config: Optional[SolverConfig] = None,
        **kwargs: Any,
    ) -> SolverResult:
        self._start_time = time.time()
        config = config or SolverConfig()

        try:
            # Parse custom parameters
            custom = kwargs.get("custom_params", {}) or {}
            pop_size = int(custom.get("population_size", 100))
            generations = int(custom.get("generations", 500))
            cx_rate = float(custom.get("crossover_rate", 0.9))
            mut_rate = float(custom.get("mutation_rate", 0.1))

            if config.seed is not None:
                np.random.seed(config.seed)

            result = self._evolve(
                problem, config, pop_size, generations, cx_rate, mut_rate
            )
            result.solve_time = time.time() - self._start_time
            return result

        except Exception as e:
            logger.error(
                "Genetic Algorithm solver failed: %s", str(e), exc_info=True
            )
            return SolverResult(
                status="error",
                solve_time=time.time() - self._start_time,
                error_message=str(e),
            )

    def supports(self, problem: ProblemDefinition) -> bool:
        return True

    def cancel(self, job_id: str) -> bool:
        return False

    # ------------------------------------------------------------------
    # Main GA loop
    # ------------------------------------------------------------------

    def _evolve(
        self,
        problem: ProblemDefinition,
        config: SolverConfig,
        pop_size: int,
        generations: int,
        cx_rate: float,
        mut_rate: float,
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

        # Variable info
        lb = np.array(
            [v.lower_bound if v.lower_bound is not None else -1e6 for v in problem.variables]
        )
        ub = np.array(
            [v.upper_bound if v.upper_bound is not None else 1e6 for v in problem.variables]
        )
        var_types = [v.var_type for v in problem.variables]

        # Initialize population
        population = self._init_population(pop_size, n, lb, ub, var_types)

        # Evaluate fitness
        fitness = np.array([
            self._evaluate(ind, problem, var_name_to_idx, maximize)
            for ind in population
        ])

        best_idx = np.argmax(fitness)
        best_ind = population[best_idx].copy()
        best_fit = fitness[best_idx]

        for gen in range(generations):
            if config.time_limit and (time.time() - self._start_time) > config.time_limit:
                break

            # Selection + Crossover + Mutation
            new_pop = []
            # Elitism: keep best individual
            new_pop.append(best_ind.copy())

            while len(new_pop) < pop_size:
                # Tournament selection
                p1 = self._tournament_select(population, fitness, 3)
                p2 = self._tournament_select(population, fitness, 3)

                # Crossover
                if np.random.random() < cx_rate:
                    c1, c2 = self._sbx_crossover(p1, p2, lb, ub, eta=2.0)
                else:
                    c1, c2 = p1.copy(), p2.copy()

                # Mutation
                if np.random.random() < mut_rate:
                    c1 = self._polynomial_mutation(c1, lb, ub, var_types, eta=20.0)
                if np.random.random() < mut_rate:
                    c2 = self._polynomial_mutation(c2, lb, ub, var_types, eta=20.0)

                new_pop.append(c1)
                if len(new_pop) < pop_size:
                    new_pop.append(c2)

            population = np.array(new_pop[:pop_size])

            # Evaluate fitness
            fitness = np.array([
                self._evaluate(ind, problem, var_name_to_idx, maximize)
                for ind in population
            ])

            # Update best
            gen_best_idx = np.argmax(fitness)
            if fitness[gen_best_idx] > best_fit:
                best_fit = fitness[gen_best_idx]
                best_ind = population[gen_best_idx].copy()

        # Build result
        variables = {}
        for i, name in enumerate(var_names):
            variables[name] = float(self._enforce_type(best_ind[i], var_types[i]))

        obj_val = self._compute_objective(best_ind, problem, var_name_to_idx)

        status = "optimal" if best_fit > -1e30 else "error"
        return SolverResult(
            status=status,
            objective_value=obj_val,
            variables=variables,
        )

    # ------------------------------------------------------------------
    # GA operators
    # ------------------------------------------------------------------

    def _init_population(
        self,
        pop_size: int,
        n: int,
        lb: np.ndarray,
        ub: np.ndarray,
        var_types: List[VariableType],
    ) -> np.ndarray:
        """Initialize population with random individuals within bounds."""
        pop = np.random.uniform(lb, ub, size=(pop_size, n))
        # Enforce integer/binary types
        for j in range(n):
            if var_types[j] == VariableType.BINARY:
                pop[:, j] = np.random.randint(0, 2, size=pop_size)
            elif var_types[j] == VariableType.INTEGER:
                pop[:, j] = np.random.randint(
                    np.floor(lb[j]), np.ceil(ub[j]) + 1, size=pop_size
                )
        return pop

    def _evaluate(
        self,
        individual: np.ndarray,
        problem: ProblemDefinition,
        var_name_to_idx: Dict[str, int],
        maximize: bool,
    ) -> float:
        """Evaluate fitness with penalty for constraint violations.

        Tournament selection picks the HIGHEST fitness.
        For minimization: fitness = -obj - penalty (lower obj => higher fitness)
        For maximization: fitness = obj - penalty (higher obj => higher fitness)
        """
        obj = self._compute_objective(individual, problem, var_name_to_idx)
        penalty = self._compute_penalty(individual, problem, var_name_to_idx)

        if maximize:
            return obj - penalty
        else:
            return -obj - penalty

    def _compute_objective(
        self,
        individual: np.ndarray,
        problem: ProblemDefinition,
        var_name_to_idx: Dict[str, int],
    ) -> float:
        """Compute objective value."""
        if not problem.objective or not problem.objective.expression:
            return 0.0
        return evaluate_expression(
            problem.objective.expression, var_name_to_idx, individual
        )

    def _compute_penalty(
        self,
        individual: np.ndarray,
        problem: ProblemDefinition,
        var_name_to_idx: Dict[str, int],
    ) -> float:
        """Compute penalty for constraint violations."""
        penalty = 0.0
        penalty_coeff = 1e6

        for constraint in problem.constraints:
            val = evaluate_expression(
                constraint.expression, var_name_to_idx, individual
            )
            lb = constraint.lower_bound
            ub = constraint.upper_bound

            if ub is not None and val > ub:
                penalty += penalty_coeff * (val - ub)
            if lb is not None and val < lb:
                penalty += penalty_coeff * (lb - val)

        return penalty

    def _tournament_select(
        self,
        population: np.ndarray,
        fitness: np.ndarray,
        tournament_size: int,
    ) -> np.ndarray:
        """Tournament selection."""
        indices = np.random.choice(len(population), size=tournament_size, replace=False)
        best = indices[np.argmax(fitness[indices])]
        return population[best].copy()

    def _sbx_crossover(
        self,
        p1: np.ndarray,
        p2: np.ndarray,
        lb: np.ndarray,
        ub: np.ndarray,
        eta: float = 2.0,
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Simulated Binary Crossover (SBX)."""
        n = len(p1)
        c1 = p1.copy()
        c2 = p2.copy()

        for i in range(n):
            if np.random.random() > 0.5:
                continue
            if abs(p1[i] - p2[i]) < 1e-14:
                continue

            y1 = min(p1[i], p2[i])
            y2 = max(p1[i], p2[i])

            rand = np.random.random()

            # Beta from distribution
            beta = 1.0 + 2.0 * min(y1 - lb[i], ub[i] - y2) / (y2 - y1)
            alpha = 2.0 - beta ** (-(eta + 1.0))

            if rand <= 1.0 / alpha:
                betaq = (rand * alpha) ** (1.0 / (eta + 1.0))
            else:
                betaq = (1.0 / (2.0 - rand * alpha)) ** (1.0 / (eta + 1.0))

            child1_val = 0.5 * ((y1 + y2) - betaq * (y2 - y1))
            child2_val = 0.5 * ((y1 + y2) + betaq * (y2 - y1))

            c1[i] = np.clip(child1_val, lb[i], ub[i])
            c2[i] = np.clip(child2_val, lb[i], ub[i])

            # Randomly swap
            if np.random.random() > 0.5:
                c1[i], c2[i] = c2[i], c1[i]

        return c1, c2

    def _polynomial_mutation(
        self,
        individual: np.ndarray,
        lb: np.ndarray,
        ub: np.ndarray,
        var_types: List[VariableType],
        eta: float = 20.0,
    ) -> np.ndarray:
        """Polynomial mutation."""
        child = individual.copy()
        n = len(child)

        for i in range(n):
            if np.random.random() > 0.1:  # per-gene mutation probability
                continue

            y = child[i]
            delta1 = (y - lb[i]) / (ub[i] - lb[i] + 1e-14)
            delta2 = (ub[i] - y) / (ub[i] - lb[i] + 1e-14)

            rand = np.random.random()
            mut_pow = 1.0 / (eta + 1.0)

            if rand < 0.5:
                xy = 1.0 - delta1
                val = 2.0 * rand + (1.0 - 2.0 * rand) * (xy ** (eta + 1.0))
                deltaq = val ** mut_pow - 1.0
            else:
                xy = 1.0 - delta2
                val = 2.0 * (1.0 - rand) + 2.0 * (rand - 0.5) * (xy ** (eta + 1.0))
                deltaq = 1.0 - val ** mut_pow

            y_new = y + deltaq * (ub[i] - lb[i])
            child[i] = np.clip(y_new, lb[i], ub[i])

            # Enforce variable type
            child[i] = self._enforce_type(child[i], var_types[i])

        return child

    def _enforce_type(self, val: float, var_type: VariableType) -> float:
        """Enforce variable type (integer/binary rounding)."""
        if var_type == VariableType.BINARY:
            return 1.0 if val > 0.5 else 0.0
        elif var_type == VariableType.INTEGER:
            return float(np.round(val))
        return val
