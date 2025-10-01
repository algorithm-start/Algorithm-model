"""Solver registry and factory for discovering and instantiating solver implementations."""

import logging
from typing import Dict, List, Optional, Type

from models.problem import ProblemDefinition, VariableType
from solvers.base import AbstractSolver

logger = logging.getLogger(__name__)

# Problem type constants
PROBLEM_LP = "LP"
PROBLEM_MILP = "MILP"
PROBLEM_QP = "QP"
PROBLEM_MIQP = "MIQP"
PROBLEM_MINLP = "MINLP"
PROBLEM_NLP = "NLP"


class SolverRegistry:
    """Registry that discovers solver implementations and maps problem types to capable solvers."""

    def __init__(self) -> None:
        self._solvers: Dict[str, AbstractSolver] = {}
        self._type_map: Dict[str, List[str]] = {}
        self._discover_solvers()

    def _discover_solvers(self) -> None:
        """Discover and register all available solver implementations."""
        # Import solvers - each is registered if its dependencies are available
        solver_classes = []

        try:
            from solvers.ortools_solver import ORToolsSolver
            solver_classes.append(ORToolsSolver)
        except Exception as e:
            logger.warning("OR-Tools solver not available: %s", str(e))

        try:
            from solvers.highs_solver import HighsSolver
            solver_classes.append(HighsSolver)
        except Exception as e:
            logger.warning("HiGHS solver not available: %s", str(e))

        try:
            from solvers.scipy_solver import SciPySolver
            solver_classes.append(SciPySolver)
        except Exception as e:
            logger.warning("SciPy solver not available: %s", str(e))

        try:
            from solvers.pulp_solver import PuLPSolver
            solver_classes.append(PuLPSolver)
        except Exception as e:
            logger.warning("PuLP solver not available: %s", str(e))

        # Built-in solvers (pure Python, always available)
        try:
            from solvers.simplex_solver import SimplexSolver
            solver_classes.append(SimplexSolver)
        except Exception as e:
            logger.warning("Simplex solver not available: %s", str(e))

        try:
            from solvers.branch_bound_solver import BranchBoundSolver
            solver_classes.append(BranchBoundSolver)
        except Exception as e:
            logger.warning("Branch and Bound solver not available: %s", str(e))

        try:
            from solvers.interior_point_solver import InteriorPointSolver
            solver_classes.append(InteriorPointSolver)
        except Exception as e:
            logger.warning("Interior Point solver not available: %s", str(e))

        try:
            from solvers.genetic_algorithm_solver import GeneticAlgorithmSolver
            solver_classes.append(GeneticAlgorithmSolver)
        except Exception as e:
            logger.warning("Genetic Algorithm solver not available: %s", str(e))

        try:
            from solvers.simulated_annealing_solver import SimulatedAnnealingSolver
            solver_classes.append(SimulatedAnnealingSolver)
        except Exception as e:
            logger.warning("Simulated Annealing solver not available: %s", str(e))

        # Register each solver
        for solver_class in solver_classes:
            try:
                solver = solver_class()
                name = solver_class.__name__
                self._solvers[name] = solver

                # Map supported types
                supported_types = getattr(solver, "SUPPORTED_TYPES", set())
                for ptype in supported_types:
                    if ptype not in self._type_map:
                        self._type_map[ptype] = []
                    self._type_map[ptype].append(name)

                logger.info("Registered solver: %s (types: %s)", name, supported_types)
            except Exception as e:
                logger.warning("Failed to register solver %s: %s", solver_class.__name__, str(e))

    def get_solver(self, problem_type: str, preferred_solver: Optional[str] = None) -> Optional[AbstractSolver]:
        """Get a solver instance for the given problem type.

        Args:
            problem_type: The type of problem to solve (LP, MILP, etc.).
            preferred_solver: Optional preferred solver name.

        Returns:
            An AbstractSolver instance, or None if no suitable solver is found.
        """
        # If a preferred solver is specified, try it first
        if preferred_solver:
            solver = self._solvers.get(preferred_solver)
            if solver and problem_type in getattr(solver, "SUPPORTED_TYPES", set()):
                return solver
            logger.info("Preferred solver %s not available for type %s, using default", preferred_solver, problem_type)

        # Get solver from type mapping
        solver_names = self._type_map.get(problem_type, [])
        if solver_names:
            return self._solvers.get(solver_names[0])

        # Fallback: return first available solver
        if self._solvers:
            name = next(iter(self._solvers))
            logger.warning("No solver found for type %s, falling back to %s", problem_type, name)
            return self._solvers[name]

        return None

    def get_all_solvers(self) -> Dict[str, AbstractSolver]:
        """Get all registered solver instances."""
        return dict(self._solvers)

    def get_supported_types(self) -> Dict[str, List[str]]:
        """Get mapping of problem types to solver names."""
        return dict(self._type_map)

    def detect_problem_type(self, problem: ProblemDefinition) -> str:
        """Detect the problem type from a problem definition.

        Args:
            problem: The problem definition to analyze.

        Returns:
            A string identifying the problem type.
        """
        has_integer = any(
            v.var_type in (VariableType.INTEGER, VariableType.BINARY)
            for v in problem.variables
        )
        has_quadratic = self._has_quadratic_objective(problem)
        has_nonlinear = self._has_nonlinear_expressions(problem)

        if has_nonlinear:
            return PROBLEM_MINLP if has_integer else PROBLEM_NLP
        elif has_quadratic:
            return PROBLEM_MIQP if has_integer else PROBLEM_QP
        elif has_integer:
            return PROBLEM_MILP
        else:
            return PROBLEM_LP

    def _has_quadratic_objective(self, problem: ProblemDefinition) -> bool:
        """Check if the objective function is quadratic."""
        if not problem.objective or not problem.objective.expression:
            return False
        expr = problem.objective.expression
        return "**2" in expr or "^2" in expr

    def _has_nonlinear_expressions(self, problem: ProblemDefinition) -> bool:
        """Check if any constraint contains nonlinear expressions."""
        nonlinear_keywords = ["sin", "cos", "exp", "log", "sqrt"]
        for c in problem.constraints:
            if c.expression:
                if any(kw in c.expression for kw in nonlinear_keywords):
                    return True
        # Also check objective
        if problem.objective and problem.objective.expression:
            if any(kw in problem.objective.expression for kw in nonlinear_keywords):
                return True
        return False


# Global singleton instance
_registry: Optional[SolverRegistry] = None


def get_registry() -> SolverRegistry:
    """Get the global solver registry instance."""
    global _registry
    if _registry is None:
        _registry = SolverRegistry()
    return _registry
