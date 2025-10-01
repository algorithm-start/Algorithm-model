"""Expression parsing utilities for built-in solvers.

Parses linear and quadratic expression strings (e.g., "3*x1 + 2*x2 - x3")
into structured coefficient dictionaries and numpy arrays.
"""

import re
import logging
from typing import Dict, List, Optional, Tuple

import numpy as np

logger = logging.getLogger(__name__)


def parse_linear_expression(expression: str, var_names: List[str]) -> Dict[str, float]:
    """Parse a linear expression string into a {variable_name: coefficient} dict.

    Handles formats like:
        "3*x1 + 2*x2 - x3"
        "x1 + x2"
        "-x1 + 3*x2"
        "3*x1 - 2*x2 + x3"

    Args:
        expression: The expression string to parse.
        var_names: List of known variable names for validation.

    Returns:
        Dictionary mapping variable names to their coefficients.
    """
    result: Dict[str, float] = {}
    var_name_set = set(var_names)

    if not expression or not expression.strip():
        return result

    expr = expression.strip()

    # Insert '+' before '-' to simplify splitting (except after 'e' for sci notation)
    normalized = ""
    for i, ch in enumerate(expr):
        if ch == '-' and i > 0 and expr[i - 1].lower() != 'e':
            normalized += "+-"
        else:
            normalized += ch

    terms = normalized.split("+")
    for term in terms:
        term = term.strip()
        if not term:
            continue

        coeff, var_name = _parse_single_term(term, var_name_set)
        if var_name:
            result[var_name] = result.get(var_name, 0.0) + coeff

    return result


def parse_linear_expression_to_array(
    expression: str,
    var_name_to_idx: Dict[str, int],
    num_vars: int,
) -> np.ndarray:
    """Parse a linear expression into a numpy coefficient array.

    Args:
        expression: The expression string to parse.
        var_name_to_idx: Mapping from variable names to indices.
        num_vars: Number of variables (length of output array).

    Returns:
        numpy array of coefficients.
    """
    coeffs = np.zeros(num_vars)
    if not expression or not expression.strip():
        return coeffs

    expr = expression.strip()
    normalized = ""
    for i, ch in enumerate(expr):
        if ch == '-' and i > 0 and expr[i - 1].lower() != 'e':
            normalized += "+-"
        else:
            normalized += ch

    terms = normalized.split("+")
    for term in terms:
        term = term.strip()
        if not term:
            continue

        coeff, var_name = _parse_single_term(term, set(var_name_to_idx.keys()))
        if var_name and var_name in var_name_to_idx:
            coeffs[var_name_to_idx[var_name]] += coeff
        elif var_name:
            logger.warning("Variable '%s' not found in variable map", var_name)

    return coeffs


def parse_quadratic_expression(
    expression: str,
    var_names: List[str],
) -> Tuple[np.ndarray, np.ndarray]:
    """Parse a quadratic expression into Q matrix and c vector.

    Supports formats like:
        "x1**2 + 2*x1*x2 + x2**2 + 3*x1 + 2*x2"
        "x1^2 + 2*x1*x2 + x2^2 + 3*x1"

    Args:
        expression: The expression string to parse.
        var_names: List of variable names.

    Returns:
        Tuple of (Q, c) where Q is the quadratic coefficient matrix (n x n)
        and c is the linear coefficient vector (n,).
    """
    n = len(var_names)
    Q = np.zeros((n, n))
    c = np.zeros(n)
    var_name_to_idx = {name: i for i, name in enumerate(var_names)}

    if not expression or not expression.strip():
        return Q, c

    expr = expression.strip()
    expr = expr.replace("^", "**")

    normalized = ""
    for i, ch in enumerate(expr):
        if ch == '-' and i > 0 and expr[i - 1].lower() != 'e':
            normalized += "+-"
        else:
            normalized += ch

    terms = normalized.split("+")
    for term in terms:
        term = term.strip()
        if not term:
            continue

        quad_result = _parse_quadratic_term(term, var_name_to_idx, n)
        if quad_result is not None:
            q_type, i, j, val = quad_result
            if q_type == "diag":
                Q[i, i] += val
            elif q_type == "cross":
                Q[i, j] += val / 2.0
                Q[j, i] += val / 2.0
            continue

        coeff, var_name = _parse_single_term(term, set(var_names))
        if var_name and var_name in var_name_to_idx:
            c[var_name_to_idx[var_name]] += coeff

    return Q, c


def evaluate_expression(
    expression: str,
    var_name_to_idx: Dict[str, int],
    x: np.ndarray,
) -> float:
    """Evaluate an expression with given variable values.

    Supports simple linear and nonlinear expressions via eval.

    Args:
        expression: The expression string.
        var_name_to_idx: Mapping from variable names to indices.
        x: Variable values array.

    Returns:
        Evaluated expression value.
    """
    if not expression or not expression.strip():
        return 0.0

    namespace = {name: x[idx] for name, idx in var_name_to_idx.items()}
    namespace["__builtins__"] = {}

    import math
    for func_name in ["sin", "cos", "exp", "log", "sqrt", "pow"]:
        namespace[func_name] = getattr(math, func_name)
    namespace["abs"] = abs

    try:
        result = eval(expression, namespace)
        return float(result)
    except Exception as e:
        logger.warning("Failed to evaluate expression '%s': %s", expression, str(e))
        return 0.0


def _parse_single_term(term: str, var_name_set: set) -> Tuple[float, Optional[str]]:
    """Parse a single term like '3*x1', '-x2', 'x3', or '5'.

    Returns:
        Tuple of (coefficient, variable_name). If the term is a constant,
        variable_name is None.
    """
    term = term.strip()
    # Normalize: remove spaces between sign and variable/coefficient
    # e.g. '- x3' -> '-x3', '+ 3*x1' -> '+3*x1'
    term = re.sub(r'([+-])\s+', r'\1', term)
    if not term:
        return 0.0, None

    if "*" in term:
        parts = term.split("*")
        if len(parts) == 2:
            try:
                coeff = float(parts[0].strip())
            except ValueError:
                logger.warning("Cannot parse coefficient from term: '%s'", term)
                return 0.0, None
            var_name = parts[1].strip()
            if var_name in var_name_set:
                return coeff, var_name
            return coeff, var_name
        elif len(parts) == 3:
            return 0.0, None
        else:
            logger.warning("Cannot parse term with multiple *: '%s'", term)
            return 0.0, None

    stripped = term.strip()

    if stripped.startswith("-") and stripped[1:] in var_name_set:
        return -1.0, stripped[1:]

    if stripped.startswith("+") and stripped[1:] in var_name_set:
        return 1.0, stripped[1:]

    if stripped in var_name_set:
        return 1.0, stripped

    try:
        float(stripped)
        return 0.0, None
    except ValueError:
        pass

    if stripped and (stripped[0].isalpha() or stripped[0] == '_'):
        return 1.0, stripped

    logger.warning("Unparseable term: '%s'", term)
    return 0.0, None


def _parse_quadratic_term(
    term: str,
    var_name_to_idx: Dict[str, int],
    n: int,
) -> Optional[Tuple[str, int, int, float]]:
    """Try to parse a quadratic term.

    Returns:
        Tuple of (type, i, j, value) where type is "diag" or "cross",
        or None if not a quadratic term.
    """
    term = term.strip()
    if not term:
        return None

    for var_name, idx in var_name_to_idx.items():
        pattern = re.compile(
            r'^([+-]?\d*\.?\d*)\s*\*\s*' + re.escape(var_name) + r'\s*\*\*\s*2$'
        )
        m = pattern.match(term)
        if m:
            coeff_str = m.group(1).strip()
            if coeff_str in ("", "+"):
                coeff = 1.0
            elif coeff_str == "-":
                coeff = -1.0
            else:
                coeff = float(coeff_str)
            return ("diag", idx, idx, coeff)

        if term == f"{var_name}**2" or term == f"{var_name}^2":
            return ("diag", idx, idx, 1.0)

    if "*" in term:
        parts = term.split("*")
        if len(parts) == 3:
            try:
                coeff = float(parts[0].strip())
                var1 = parts[1].strip()
                var2 = parts[2].strip()
                if var1 in var_name_to_idx and var2 in var_name_to_idx:
                    i = var_name_to_idx[var1]
                    j = var_name_to_idx[var2]
                    return ("cross", i, j, coeff)
            except ValueError:
                pass
        elif len(parts) == 2:
            var1 = parts[0].strip()
            var2 = parts[1].strip()
            if var1 in var_name_to_idx and var2 in var_name_to_idx:
                i = var_name_to_idx[var1]
                j = var_name_to_idx[var2]
                return ("cross", i, j, 1.0)

    return None
