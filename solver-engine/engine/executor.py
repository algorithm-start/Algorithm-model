"""Job executor for managing solver execution lifecycle."""

import logging
import threading
import time
import uuid
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict, Optional

from models.config import SolverConfig
from models.problem import ProblemDefinition
from models.result import SolverResult
from solvers.base import AbstractSolver
from solvers.registry import SolverRegistry, get_registry
from engine.detector import ProblemTypeDetector, ProblemType
from engine.selector import AlgorithmSelector

logger = logging.getLogger(__name__)


class JobStatus(str, Enum):
    """Status of a solver job."""
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


@dataclass
class SolverJob:
    """Represents a solver job with its state.

    Attributes:
        job_id: Unique identifier for the job.
        problem: The problem definition.
        config: The solver configuration.
        status: Current job status.
        result: The solver result (if completed).
        solver_name: Name of the solver used.
        algorithm: Algorithm used for solving.
        error_message: Error message if failed.
        create_time: When the job was created.
        start_time: When execution started.
        end_time: When execution completed.
    """
    job_id: str
    problem: ProblemDefinition
    config: SolverConfig
    status: JobStatus = JobStatus.PENDING
    result: Optional[SolverResult] = None
    solver_name: Optional[str] = None
    algorithm: Optional[str] = None
    error_message: Optional[str] = None
    create_time: float = field(default_factory=time.time)
    start_time: Optional[float] = None
    end_time: Optional[float] = None
    _cancel_event: threading.Event = field(default_factory=threading.Event, repr=False)


class JobExecutor:
    """Executes solver jobs and manages their lifecycle.

    Supports both synchronous and asynchronous execution with:
    - Timeout management
    - Cancellation via threading
    - Error handling and status reporting
    """

    def __init__(self, registry: Optional[SolverRegistry] = None) -> None:
        self._registry = registry or get_registry()
        self._detector = ProblemTypeDetector()
        self._selector = AlgorithmSelector(self._registry)
        self._jobs: Dict[str, SolverJob] = {}
        self._lock = threading.Lock()

    def execute(self, problem: ProblemDefinition, config: Optional[SolverConfig] = None) -> SolverResult:
        """Execute a solver job synchronously.

        Runs through detector -> selector -> executor pipeline.

        Args:
            problem: The problem definition to solve.
            config: Optional solver configuration.

        Returns:
            SolverResult containing the solution.
        """
        config = config or SolverConfig()
        start_time = time.time()

        try:
            # Step 1: Detect problem type
            detection = self._detector.analyze(problem)
            problem_type = detection.problem_type.value
            problem_size = detection.num_variables + detection.num_constraints
            logger.info("Detected problem type: %s (confidence=%.2f, size=%d)",
                        problem_type, detection.confidence, problem_size)

            # Step 2: Select solver and algorithm
            solver_name, algorithm = self._selector.select(problem_type, problem_size, config)
            logger.info("Selected solver: %s, algorithm: %s", solver_name, algorithm)

            # Step 3: Get solver instance
            solver = self._registry.get_solver(problem_type, preferred_solver=solver_name)
            if solver is None:
                return SolverResult(
                    status="error",
                    solve_time=time.time() - start_time,
                )

            # Step 4: Execute with timeout
            time_limit = config.time_limit or 300.0
            result = self._execute_with_timeout(solver, problem, config, time_limit)

            # Add solve time if not set
            if result.solve_time == 0.0:
                result.solve_time = time.time() - start_time

            return result

        except Exception as e:
            logger.error("Job execution failed: %s", str(e), exc_info=True)
            return SolverResult(
                status="error",
                solve_time=time.time() - start_time,
            )

    def submit(self, problem: ProblemDefinition, config: Optional[SolverConfig] = None) -> str:
        """Submit a problem for asynchronous solving.

        Args:
            problem: The problem to solve.
            config: Optional solver configuration.

        Returns:
            A job ID for tracking.
        """
        config = config or SolverConfig()
        job_id = str(uuid.uuid4())

        job = SolverJob(
            job_id=job_id,
            problem=problem,
            config=config,
        )

        with self._lock:
            self._jobs[job_id] = job

        # Start execution in background thread
        thread = threading.Thread(
            target=self._run_job,
            args=(job,),
            daemon=True,
        )
        thread.start()

        logger.info("Submitted async job %s", job_id)
        return job_id

    def get_status(self, job_id: str) -> Optional[str]:
        """Get the status of a running job.

        Args:
            job_id: The job ID to check.

        Returns:
            The job status string, or None if not found.
        """
        with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                return None
            return job.status.value

    def get_result(self, job_id: str) -> Optional[SolverResult]:
        """Get the result of a completed job.

        Args:
            job_id: The job ID to retrieve results for.

        Returns:
            The solver result, or None if not complete.
        """
        with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                return None
            if job.status in (JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED):
                return job.result
            return None

    def cancel(self, job_id: str) -> bool:
        """Cancel a running job.

        Args:
            job_id: The job ID to cancel.

        Returns:
            True if the job was successfully cancelled.
        """
        with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                return False
            if job.status in (JobStatus.PENDING, JobStatus.RUNNING):
                job._cancel_event.set()
                job.status = JobStatus.CANCELLED
                job.end_time = time.time()
                logger.info("Job %s cancelled", job_id)
                return True
            return False

    def _run_job(self, job: SolverJob) -> None:
        """Run a solver job in a background thread.

        Args:
            job: The job to execute.
        """
        job.status = JobStatus.RUNNING
        job.start_time = time.time()

        try:
            # Detect problem type
            detection = self._detector.analyze(job.problem)
            problem_type = detection.problem_type.value
            problem_size = detection.num_variables + detection.num_constraints

            # Select solver
            solver_name, algorithm = self._selector.select(
                problem_type, problem_size, job.config
            )
            job.solver_name = solver_name
            job.algorithm = algorithm

            # Check for cancellation
            if job._cancel_event.is_set():
                job.status = JobStatus.CANCELLED
                return

            # Get solver
            solver = self._registry.get_solver(problem_type, preferred_solver=solver_name)
            if solver is None:
                job.status = JobStatus.FAILED
                job.error_message = f"No solver available for type {problem_type}"
                return

            # Execute with timeout
            time_limit = job.config.time_limit or 300.0
            result = self._execute_with_timeout(solver, job.problem, job.config, time_limit)

            # Check if cancelled during execution
            if job._cancel_event.is_set():
                job.status = JobStatus.CANCELLED
                return

            job.result = result
            job.status = JobStatus.COMPLETED
            job.end_time = time.time()

        except Exception as e:
            logger.error("Job %s failed: %s", job.job_id, str(e), exc_info=True)
            job.status = JobStatus.FAILED
            job.error_message = str(e)
            job.result = SolverResult(status="error")
            job.end_time = time.time()

    def _execute_with_timeout(self, solver: AbstractSolver, problem: ProblemDefinition,
                               config: SolverConfig, timeout: float) -> SolverResult:
        """Execute a solver with a timeout.

        Args:
            solver: The solver instance to use.
            problem: The problem definition.
            config: The solver configuration.
            timeout: Maximum execution time in seconds.

        Returns:
            SolverResult containing the solution.
        """
        result_container = {"result": None, "error": None}
        cancel_event = threading.Event()

        def run_solver():
            try:
                result_container["result"] = solver.solve(problem, config)
            except Exception as e:
                result_container["error"] = e

        # Run solver in a thread
        solver_thread = threading.Thread(target=run_solver, daemon=True)
        solver_thread.start()
        solver_thread.join(timeout=timeout)

        if solver_thread.is_alive():
            # Timeout - solver is still running
            logger.warning("Solver timed out after %.1f seconds", timeout)
            # Try to cancel
            solver.cancel("timeout")
            return SolverResult(
                status="timeout",
                solve_time=timeout,
            )

        if result_container["error"] is not None:
            raise result_container["error"]

        return result_container["result"] or SolverResult(status="error")

    def list_jobs(self) -> Dict[str, str]:
        """List all jobs and their statuses.

        Returns:
            Dict mapping job_id to status string.
        """
        with self._lock:
            return {jid: job.status.value for jid, job in self._jobs.items()}

    def cleanup(self, max_age: float = 3600.0) -> int:
        """Clean up old completed/failed jobs.

        Args:
            max_age: Maximum age in seconds for completed jobs.

        Returns:
            Number of jobs cleaned up.
        """
        now = time.time()
        to_remove = []

        with self._lock:
            for job_id, job in self._jobs.items():
                if job.status in (JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED):
                    if job.end_time and (now - job.end_time) > max_age:
                        to_remove.append(job_id)

            for job_id in to_remove:
                del self._jobs[job_id]

        if to_remove:
            logger.info("Cleaned up %d old jobs", len(to_remove))

        return len(to_remove)
