from abc import ABC, abstractmethod
from typing import Any, Dict, Optional

from models.problem import ProblemDefinition
from models.result import SolverResult


class AbstractSolver(ABC):
    """Abstract base class for all solvers."""

    @abstractmethod
    def solve(self, problem: ProblemDefinition, **kwargs: Any) -> SolverResult:
        """Solve the given optimization problem.

        Args:
            problem: The problem definition to solve.
            **kwargs: Additional solver-specific parameters.

        Returns:
            SolverResult containing the solution.
        """
        ...

    @abstractmethod
    def supports(self, problem: ProblemDefinition) -> bool:
        """Check if this solver supports the given problem type.

        Args:
            problem: The problem definition to check.

        Returns:
            True if this solver can handle the problem.
        """
        ...

    @abstractmethod
    def cancel(self, job_id: str) -> bool:
        """Cancel a running solve job.

        Args:
            job_id: The ID of the job to cancel.

        Returns:
            True if the job was successfully cancelled.
        """
        ...
