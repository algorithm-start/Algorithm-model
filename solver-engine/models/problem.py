from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class VariableType(str, Enum):
    CONTINUOUS = "continuous"
    INTEGER = "integer"
    BINARY = "binary"


class Variable(BaseModel):
    """Decision variable definition."""
    name: str = Field(..., description="Variable name")
    lower_bound: float = Field(default=0.0, description="Lower bound")
    upper_bound: float = Field(default=float("inf"), description="Upper bound")
    var_type: VariableType = Field(default=VariableType.CONTINUOUS, description="Variable type")


class Constraint(BaseModel):
    """Constraint definition."""
    name: str = Field(..., description="Constraint name")
    expression: str = Field(..., description="Constraint expression")
    lower_bound: Optional[float] = Field(default=None, description="Lower bound")
    upper_bound: Optional[float] = Field(default=None, description="Upper bound")


class ObjectiveSense(str, Enum):
    MINIMIZE = "minimize"
    MAXIMIZE = "maximize"


class ObjectiveFunction(BaseModel):
    """Objective function definition."""
    sense: ObjectiveSense = Field(default=ObjectiveSense.MINIMIZE, description="Optimization sense")
    expression: str = Field(..., description="Objective expression")


class ProblemDefinition(BaseModel):
    """Complete problem definition."""
    name: str = Field(..., description="Problem name")
    variables: List[Variable] = Field(default_factory=list, description="Decision variables")
    constraints: List[Constraint] = Field(default_factory=list, description="Constraints")
    objective: Optional[ObjectiveFunction] = Field(default=None, description="Objective function")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Additional metadata")
