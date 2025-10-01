from typing import Optional

from pydantic import BaseModel, Field


class SolverConfig(BaseModel):
    """Configuration for solver execution."""
    algorithm: str = Field(default="auto", description="Algorithm to use (auto, simplex, ipm, etc.)")
    time_limit: float = Field(default=300.0, description="Time limit in seconds")
    gap_tolerance: float = Field(default=1e-4, description="MIP gap tolerance")
    max_iterations: Optional[int] = Field(default=None, description="Maximum number of iterations")
    threads: int = Field(default=0, description="Number of threads (0=auto)")
    verbose: bool = Field(default=False, description="Enable verbose output")
    seed: Optional[int] = Field(default=None, description="Random seed for reproducibility")
