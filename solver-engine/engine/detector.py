"""Problem type detector for analyzing optimization problem definitions."""

import logging
from dataclasses import dataclass
from enum import Enum
from typing import List, Optional

from models.problem import ObjectiveSense, ProblemDefinition, VariableType

logger = logging.getLogger(__name__)


class ProblemType(str, Enum):
    """Enumeration of supported problem types."""
    LP = "LP"
    MILP = "MILP"
    QP = "QP"
    MIQP = "MIQP"
    NLP = "NLP"
    MINLP = "MINLP"
    UNKNOWN = "UNKNOWN"


@dataclass
class DetectionResult:
    """Result of problem type detection.

    Attributes:
        problem_type: The detected problem type.
        confidence: Confidence score (0.0 - 1.0).
        has_integer_vars: Whether the problem has integer/binary variables.
        has_quadratic: Whether the problem has quadratic terms.
        has_nonlinear: Whether the problem has nonlinear terms.
        num_variables: Number of variables.
        num_constraints: Number of constraints.
        reasoning: Explanation of the detection result.
    """
    problem_type: ProblemType
    confidence: float
    has_integer_vars: bool
    has_quadratic: bool
    has_nonlinear: bool
    num_variables: int
    num_constraints: int
    reasoning: str


class ProblemTypeDetector:
    """Detects the type of optimization problem from its definition.

    Analysis rules:
    - All variables continuous + linear constraints/objective -> LP
    - Any integer/binary variables + linear -> MILP
    - Quadratic objective + continuous -> QP
    - Quadratic objective + integer vars -> MIQP
    - Nonlinear expressions + continuous -> NLP
    - Nonlinear expressions + integer vars -> MINLP
    """

    # Keywords that indicate nonlinear expressions
    NONLINEAR_KEYWORDS = ["sin", "cos", "tan", "exp", "log", "ln", "sqrt", "abs"]

    # Keywords that indicate quadratic expressions
    QUADRATIC_KEYWORDS = ["**2", "^2", "squared"]

    def analyze(self, problem: ProblemDefinition) -> DetectionResult:
        """Analyze a problem definition and detect its type.

        Args:
            problem: The problem definition to analyze.

        Returns:
            DetectionResult with the detected type and analysis details.
        """
        num_vars = len(problem.variables)
        num_cons = len(problem.constraints)

        # Analyze variable types
        has_integer = self._check_integer_variables(problem)
        integer_var_count = sum(
            1 for v in problem.variables
            if v.var_type in (VariableType.INTEGER, VariableType.BINARY)
        )

        # Analyze expressions
        has_quadratic = self._check_quadratic(problem)
        has_nonlinear = self._check_nonlinear(problem)

        # Determine problem type
        problem_type, confidence, reasoning = self._classify(
            has_integer, has_quadratic, has_nonlinear, num_vars, num_cons,
            integer_var_count
        )

        return DetectionResult(
            problem_type=problem_type,
            confidence=confidence,
            has_integer_vars=has_integer,
            has_quadratic=has_quadratic,
            has_nonlinear=has_nonlinear,
            num_variables=num_vars,
            num_constraints=num_cons,
            reasoning=reasoning,
        )

    def detect(self, problem: ProblemDefinition) -> Optional[str]:
        """Detect the problem type (legacy interface).

        Args:
            problem: The problem definition to analyze.

        Returns:
            A string identifier for the problem type, or None if unknown.
        """
        result = self.analyze(problem)
        if result.problem_type == ProblemType.UNKNOWN:
            return None
        return result.problem_type.value

    def _check_integer_variables(self, problem: ProblemDefinition) -> bool:
        """Check if the problem has integer or binary variables."""
        return any(
            v.var_type in (VariableType.INTEGER, VariableType.BINARY)
            for v in problem.variables
        )

    def _check_quadratic(self, problem: ProblemDefinition) -> bool:
        """Check if the problem has quadratic terms."""
        # Check objective
        if problem.objective and problem.objective.expression:
            if any(kw in problem.objective.expression for kw in self.QUADRATIC_KEYWORDS):
                return True
            # Check for product of two variables (e.g., x1*x2)
            if self._has_variable_products(problem.objective.expression):
                return True

        # Check constraints
        for c in problem.constraints:
            if c.expression:
                if any(kw in c.expression for kw in self.QUADRATIC_KEYWORDS):
                    return True
                if self._has_variable_products(c.expression):
                    return True

        return False

    def _check_nonlinear(self, problem: ProblemDefinition) -> bool:
        """Check if the problem has nonlinear expressions."""
        # Check objective
        if problem.objective and problem.objective.expression:
            if any(kw in problem.objective.expression for kw in self.NONLINEAR_KEYWORDS):
                return True

        # Check constraints
        for c in problem.constraints:
            if c.expression:
                if any(kw in c.expression for kw in self.NONLINEAR_KEYWORDS):
                    return True

        # Check metadata for problem type hints
        if problem.metadata and "problem_type" in problem.metadata:
            ptype = problem.metadata["problem_type"]
            if ptype in ("NLP", "MINLP", "nonlinear"):
                return True

        return False

    def _has_variable_products(self, expression: str) -> bool:
        """Check if an expression contains products of variables.

        Simple heuristic: look for patterns like x1*x2 where both sides
        look like variable names (not numeric coefficients).
        """
        # This is a simplified check - a full parser would be more robust
        terms = expression.replace("-", "+-").split("+")
        var_names = set()
        for term in terms:
            term = term.strip()
            if "*" in term:
                parts = term.split("*")
                for part in parts:
                    part = part.strip()
                    try:
                        float(part)
                    except ValueError:
                        # Not a number - likely a variable name
                        var_names.add(part)

        # If there's a term with two non-numeric parts, it's likely a variable product
        for term in terms:
            term = term.strip()
            if "*" in term:
                parts = term.split("*")
                non_numeric = sum(1 for p in parts if not self._is_numeric(p.strip()))
                if non_numeric >= 2:
                    return True

        return False

    def _is_numeric(self, s: str) -> bool:
        """Check if a string represents a number."""
        try:
            float(s)
            return True
        except ValueError:
            return False

    def _classify(self, has_integer: bool, has_quadratic: bool, has_nonlinear: bool,
                  num_vars: int, num_cons: int, integer_var_count: int) -> tuple:
        """Classify the problem type based on analysis results.

        Returns:
            Tuple of (ProblemType, confidence, reasoning).
        """
        if has_nonlinear:
            if has_integer:
                return (
                    ProblemType.MINLP,
                    0.8,
                    f"Mixed-integer nonlinear problem detected: {integer_var_count} integer variables "
                    f"with nonlinear expressions"
                )
            else:
                return (
                    ProblemType.NLP,
                    0.85,
                    "Nonlinear problem detected: continuous variables with nonlinear expressions"
                )

        if has_quadratic:
            if has_integer:
                return (
                    ProblemType.MIQP,
                    0.85,
                    f"Mixed-integer quadratic problem detected: {integer_var_count} integer variables "
                    f"with quadratic terms"
                )
            else:
                return (
                    ProblemType.QP,
                    0.9,
                    "Quadratic problem detected: continuous variables with quadratic objective"
                )

        if has_integer:
            return (
                ProblemType.MILP,
                0.9,
                f"Mixed-integer linear problem detected: {integer_var_count} integer/binary variables "
                f"with linear structure ({num_vars} variables, {num_cons} constraints)"
            )

        # Pure LP
        confidence = 0.95
        if num_vars == 0 and num_cons == 0:
            confidence = 0.5
            return (ProblemType.LP, confidence, "Empty problem - defaulting to LP")

        return (
            ProblemType.LP,
            confidence,
            f"Linear programming problem detected: {num_vars} continuous variables, "
            f"{num_cons} linear constraints"
        )
