"""Solver result model."""

from typing import Dict, Optional

from pydantic import BaseModel, Field


class SolverResult(BaseModel):
    """Result from a solver execution."""
    status: str = Field(..., description="Solver status (optimal, infeasible, error, timeout, etc.)")
    objective_value: Optional[float] = Field(default=None, description="Objective function value")
    variables: Dict[str, float] = Field(default_factory=dict, description="Variable assignments")
    solve_time: float = Field(default=0.0, description="Solve time in seconds")
    error_message: Optional[str] = Field(default=None, description="Error message if solver failed")
