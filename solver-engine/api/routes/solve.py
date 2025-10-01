"""Solve API routes for the solver engine."""

import logging
from typing import Dict, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from engine.detector import ProblemTypeDetector, DetectionResult
from engine.executor import JobExecutor, JobStatus
from engine.selector import AlgorithmSelector
from models.config import SolverConfig
from models.problem import ProblemDefinition
from models.result import SolverResult
from solvers.registry import get_registry

logger = logging.getLogger(__name__)

router = APIRouter()

# Global executor instance
_executor: Optional[JobExecutor] = None
_detector: Optional[ProblemTypeDetector] = None


def _get_executor() -> JobExecutor:
    """Get or create the global executor instance."""
    global _executor
    if _executor is None:
        _executor = JobExecutor()
    return _executor


def _get_detector() -> ProblemTypeDetector:
    """Get or create the global detector instance."""
    global _detector
    if _detector is None:
        _detector = ProblemTypeDetector()
    return _detector


# --- Request/Response Models ---

class SolveRequest(BaseModel):
    """Request model for the /solve endpoint."""
    problem: ProblemDefinition = Field(..., description="Problem definition")
    config: SolverConfig = Field(default_factory=SolverConfig, description="Solver configuration")


class AsyncSolveRequest(BaseModel):
    """Request model for the /solve/async endpoint."""
    problem: ProblemDefinition = Field(..., description="Problem definition")
    config: SolverConfig = Field(default_factory=SolverConfig, description="Solver configuration")


class SolveResponse(BaseModel):
    """Response model for the /solve endpoint."""
    status: str = Field(..., description="Solver status")
    objective_value: Optional[float] = Field(default=None, description="Objective function value")
    variables: Dict[str, float] = Field(default_factory=dict, description="Variable assignments")
    solve_time: float = Field(default=0.0, description="Solve time in seconds")
    problem_type: Optional[str] = Field(default=None, description="Detected problem type")
    solver_used: Optional[str] = Field(default=None, description="Solver used")
    algorithm: Optional[str] = Field(default=None, description="Algorithm used")
    error_message: Optional[str] = Field(default=None, description="Error message if failed")


class AsyncJobResponse(BaseModel):
    """Response model for the /solve/async endpoint."""
    job_id: str = Field(..., description="Job ID for tracking")
    status: str = Field(default="pending", description="Initial job status")


class JobStatusResponse(BaseModel):
    """Response model for the /solve/{job_id}/status endpoint."""
    job_id: str = Field(..., description="Job ID")
    status: str = Field(..., description="Current job status")
    result: Optional[SolveResponse] = Field(default=None, description="Result if completed")
    error_message: Optional[str] = Field(default=None, description="Error message if failed")


class CancelResponse(BaseModel):
    """Response model for the /solve/{job_id}/cancel endpoint."""
    job_id: str = Field(..., description="Job ID")
    cancelled: bool = Field(..., description="Whether the job was cancelled")


class DetectionResponse(BaseModel):
    """Response model for problem type detection."""
    problem_type: str = Field(..., description="Detected problem type")
    confidence: float = Field(..., description="Detection confidence")
    has_integer_vars: bool = Field(..., description="Has integer variables")
    has_quadratic: bool = Field(..., description="Has quadratic terms")
    has_nonlinear: bool = Field(..., description="Has nonlinear terms")
    num_variables: int = Field(..., description="Number of variables")
    num_constraints: int = Field(..., description="Number of constraints")
    reasoning: str = Field(..., description="Detection reasoning")


# --- Routes ---

@router.post("/solve", response_model=SolveResponse)
async def solve_problem(request: SolveRequest):
    """Submit a problem to be solved synchronously.

    Runs through detector -> selector -> executor pipeline and returns result.
    """
    logger.info("Received solve request for problem: %s", request.problem.name)

    try:
        # Detect problem type
        detector = _get_detector()
        detection = detector.analyze(request.problem)
        logger.info("Detected problem type: %s (confidence=%.2f)",
                    detection.problem_type.value, detection.confidence)

        # Execute
        executor = _get_executor()
        result = executor.execute(request.problem, request.config)

        # Build response
        response = SolveResponse(
            status=result.status,
            objective_value=result.objective_value,
            variables=result.variables,
            solve_time=result.solve_time,
            problem_type=detection.problem_type.value,
        )

        if result.status == "error" and not result.variables:
            response.error_message = "Solver failed to find a solution"

        return response

    except Exception as e:
        logger.error("Solve request failed: %s", str(e), exc_info=True)
        raise HTTPException(status_code=500, detail=f"Solver error: {str(e)}")


@router.post("/solve/async", response_model=AsyncJobResponse)
async def solve_async(request: AsyncSolveRequest):
    """Submit a problem for asynchronous solving.

    Returns a job ID that can be used to check status and retrieve results.
    """
    logger.info("Received async solve request for problem: %s", request.problem.name)

    try:
        executor = _get_executor()
        job_id = executor.submit(request.problem, request.config)
        return AsyncJobResponse(job_id=job_id, status="pending")
    except Exception as e:
        logger.error("Async solve submission failed: %s", str(e), exc_info=True)
        raise HTTPException(status_code=500, detail=f"Failed to submit job: {str(e)}")


@router.get("/solve/{job_id}/status", response_model=JobStatusResponse)
async def get_job_status(job_id: str):
    """Check the status of an async solve job."""
    executor = _get_executor()

    status = executor.get_status(job_id)
    if status is None:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")

    result = executor.get_result(job_id)

    response = JobStatusResponse(
        job_id=job_id,
        status=status,
    )

    if result is not None:
        response.result = SolveResponse(
            status=result.status,
            objective_value=result.objective_value,
            variables=result.variables,
            solve_time=result.solve_time,
        )

    return response


@router.post("/solve/{job_id}/cancel", response_model=CancelResponse)
async def cancel_job(job_id: str):
    """Cancel a running async solve job."""
    executor = _get_executor()

    cancelled = executor.cancel(job_id)
    if not cancelled:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot cancel job {job_id} (not found or already completed)"
        )

    return CancelResponse(job_id=job_id, cancelled=True)


@router.post("/detect", response_model=DetectionResponse)
async def detect_problem_type(problem: ProblemDefinition):
    """Detect the type of an optimization problem.

    Analyzes the problem definition and returns the detected type
    with confidence and details.
    """
    detector = _get_detector()
    detection = detector.analyze(problem)

    return DetectionResponse(
        problem_type=detection.problem_type.value,
        confidence=detection.confidence,
        has_integer_vars=detection.has_integer_vars,
        has_quadratic=detection.has_quadratic,
        has_nonlinear=detection.has_nonlinear,
        num_variables=detection.num_variables,
        num_constraints=detection.num_constraints,
        reasoning=detection.reasoning,
    )
