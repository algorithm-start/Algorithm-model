"""Algorithm selector for choosing the best solver and algorithm for a problem."""

import logging
from typing import Optional, Tuple

from models.config import SolverConfig
from models.problem import ProblemDefinition, VariableType
from solvers.base import AbstractSolver
from solvers.registry import SolverRegistry, get_registry

logger = logging.getLogger(__name__)


class AlgorithmSelector:
    """Selects the appropriate solver algorithm for a given problem type and size.

    Selection rules:
    - Small LP -> scipy/simplex (fast for small problems)
    - Large LP -> HiGHS (efficient for large-scale LP)
    - MILP -> OR-Tools (CBC) or PuLP/CBC (robust MILP solvers)
    - QP/MIQP -> OR-Tools (if supported) or scipy
    - NLP/MINLP -> scipy (nonlinear optimization)
    - Problem size considerations for heuristic vs exact methods
    """

    # Size thresholds
    SMALL_PROBLEM_THRESHOLD = 100   # vars + constraints
    MEDIUM_PROBLEM_THRESHOLD = 1000
    LARGE_PROBLEM_THRESHOLD = 10000

    def __init__(self, registry: Optional[SolverRegistry] = None) -> None:
        self._registry = registry or get_registry()

    def select(self, problem_type: str, problem_size: int,
               config: Optional[SolverConfig] = None) -> Tuple[str, str]:
        """Select a solver and algorithm for the given problem.

        Args:
            problem_type: The detected problem type (LP, MILP, etc.).
            problem_size: The estimated problem size (num vars + num constraints).
            config: Optional solver configuration with preferences.

        Returns:
            Tuple of (solver_name, algorithm) where solver_name identifies
            the solver class and algorithm is the method to use.
        """
        # Check if config specifies a preferred algorithm
        if config and config.algorithm and config.algorithm != "auto":
            solver = self._registry.get_solver(problem_type, preferred_solver=None)
            if solver:
                solver_name = type(solver).__name__
                return solver_name, config.algorithm

        # Select based on problem type and size
        if problem_type == "LP":
            return self._select_lp(problem_size)
        elif problem_type == "MILP":
            return self._select_milp(problem_size)
        elif problem_type == "QP":
            return self._select_qp(problem_size)
        elif problem_type == "MIQP":
            return self._select_miqp(problem_size)
        elif problem_type in ("NLP", "MINLP"):
            return self._select_nonlinear(problem_size)
        else:
            logger.warning("Unknown problem type %s, defaulting to OR-Tools", problem_type)
            return "ORToolsSolver", "auto"

    def select_for_problem(self, problem: ProblemDefinition,
                           config: Optional[SolverConfig] = None) -> Tuple[str, str]:
        """Select a solver for a ProblemDefinition directly.

        Args:
            problem: The problem definition.
            config: Optional solver configuration.

        Returns:
            Tuple of (solver_name, algorithm).
        """
        problem_type = self._registry.detect_problem_type(problem)
        problem_size = len(problem.variables) + len(problem.constraints)
        return self.select(problem_type, problem_size, config)

    def _select_lp(self, size: int) -> Tuple[str, str]:
        """Select solver for linear programming problems."""
        if size <= self.SMALL_PROBLEM_THRESHOLD:
            # Small LP: scipy linprog is fast
            solver = self._registry.get_solver("LP", preferred_solver="SciPySolver")
            if solver:
                return "SciPySolver", "simplex"
            # Fallback to HiGHS
            solver = self._registry.get_solver("LP", preferred_solver="HighsSolver")
            if solver:
                return "HighsSolver", "simplex"

        if size <= self.LARGE_PROBLEM_THRESHOLD:
            # Medium LP: OR-Tools GLOP
            solver = self._registry.get_solver("LP", preferred_solver="ORToolsSolver")
            if solver:
                return "ORToolsSolver", "glop"

        # Large LP: HiGHS (best for large-scale)
        solver = self._registry.get_solver("LP", preferred_solver="HighsSolver")
        if solver:
            return "HighsSolver", "simplex"

        # Fallback: any available LP solver
        solver = self._registry.get_solver("LP")
        if solver:
            return type(solver).__name__, "auto"

        return "ORToolsSolver", "glop"

    def _select_milp(self, size: int) -> Tuple[str, str]:
        """Select solver for mixed-integer linear programming problems."""
        if size <= self.MEDIUM_PROBLEM_THRESHOLD:
            # Small/medium MILP: OR-Tools CBC
            solver = self._registry.get_solver("MILP", preferred_solver="ORToolsSolver")
            if solver:
                return "ORToolsSolver", "cbc"

        # Large MILP: PuLP/CBC (more configurable)
        solver = self._registry.get_solver("MILP", preferred_solver="PuLPSolver")
        if solver:
            return "PuLPSolver", "cbc"

        # Fallback to OR-Tools
        solver = self._registry.get_solver("MILP", preferred_solver="ORToolsSolver")
        if solver:
            return "ORToolsSolver", "cbc"

        # Any available MILP solver
        solver = self._registry.get_solver("MILP")
        if solver:
            return type(solver).__name__, "auto"

        return "ORToolsSolver", "cbc"

    def _select_qp(self, size: int) -> Tuple[str, str]:
        """Select solver for quadratic programming problems."""
        # OR-Tools supports QP via Gurobi or SCIP if available
        solver = self._registry.get_solver("QP", preferred_solver="ORToolsSolver")
        if solver:
            return "ORToolsSolver", "glop"

        # Fallback: scipy with SLSQP for continuous QP
        solver = self._registry.get_solver("LP", preferred_solver="SciPySolver")
        if solver:
            return "SciPySolver", "slsqp"

        return "ORToolsSolver", "glop"

    def _select_miqp(self, size: int) -> Tuple[str, str]:
        """Select solver for mixed-integer quadratic programming problems."""
        # OR-Tools CBC can handle MIQP
        solver = self._registry.get_solver("MIQP", preferred_solver="ORToolsSolver")
        if solver:
            return "ORToolsSolver", "cbc"

        # Fallback: PuLP
        solver = self._registry.get_solver("MILP", preferred_solver="PuLPSolver")
        if solver:
            return "PuLPSolver", "cbc"

        return "ORToolsSolver", "cbc"

    def _select_nonlinear(self, size: int) -> Tuple[str, str]:
        """Select solver for nonlinear programming problems."""
        # scipy.optimize.minimize is the best option for NLP
        solver = self._registry.get_solver("NLP", preferred_solver="SciPySolver")
        if solver:
            return "SciPySolver", "slsqp"

        # Fallback to any available solver
        solver = self._registry.get_solver("LP")
        if solver:
            return type(solver).__name__, "auto"

        return "SciPySolver", "slsqp"
