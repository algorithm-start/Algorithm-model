"""Example optimization problems API routes.

Provides pre-defined example problems that users can load and solve
directly through the solver engine, enabling quick demonstrations
of different optimization problem types.
"""

from typing import Dict, List, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from models.problem import (
    ProblemDefinition,
    Variable,
    Constraint,
    ObjectiveFunction,
    VariableType,
    ObjectiveSense,
)

router = APIRouter()


# ---------------------------------------------------------------------------
# Response Models
# ---------------------------------------------------------------------------

class ExampleSummary(BaseModel):
    """Summary of an example problem (used in list endpoint)."""
    id: str
    name: str
    description: str
    problem_type: str
    recommended_algorithm: str
    difficulty: str
    tags: List[str]


class ExampleDetail(BaseModel):
    """Full detail of an example problem (includes ProblemDefinition)."""
    id: str
    name: str
    description: str
    problem_type: str
    recommended_algorithm: str
    difficulty: str
    tags: List[str]
    problem: ProblemDefinition
    expected_objective: Optional[float] = None
    explanation: str


# ---------------------------------------------------------------------------
# Helper utilities
# ---------------------------------------------------------------------------

# Large finite number used instead of inf so JSON round-trips work
# (JSON has no Infinity literal; float("inf") serializes to null which
#  then fails Pydantic validation on the /solve endpoint).
_LARGE_UB = 1e9


def _var(
    name: str,
    *,
    lb: float = 0.0,
    ub: float = _LARGE_UB,
    vtype: VariableType = VariableType.CONTINUOUS,
) -> Variable:
    """Shorthand to create a Variable."""
    return Variable(name=name, lower_bound=lb, upper_bound=ub, var_type=vtype)


def _con(
    name: str,
    expression: str,
    *,
    lb: Optional[float] = None,
    ub: Optional[float] = None,
) -> Constraint:
    """Shorthand to create a Constraint."""
    return Constraint(name=name, expression=expression, lower_bound=lb, upper_bound=ub)


# ---------------------------------------------------------------------------
# Example 1: Transportation Problem
# ---------------------------------------------------------------------------

def _build_transportation() -> ExampleDetail:
    supply = [300, 400, 500]
    demand = [250, 350, 200, 400]
    cost = [
        [8, 6, 10, 9],
        [9, 12, 13, 7],
        [14, 9, 16, 5],
    ]
    n_warehouse = len(supply)
    n_customer = len(demand)

    # Variables: x_{i}_{j} = shipment from warehouse i to customer j
    # Upper bound = min(supply[i], demand[j]) — tighter than the default
    variables: List[Variable] = []
    for i in range(n_warehouse):
        for j in range(n_customer):
            variables.append(_var(f"x_{i}_{j}", ub=min(supply[i], demand[j])))

    # Supply constraints: sum_j x_{i}_{j} <= supply[i]
    constraints: List[Constraint] = []
    for i in range(n_warehouse):
        terms = " + ".join(f"x_{i}_{j}" for j in range(n_customer))
        constraints.append(_con(f"supply_{i}", terms, ub=supply[i]))

    # Demand constraints: sum_i x_{i}_{j} >= demand[j]
    for j in range(n_customer):
        terms = " + ".join(f"x_{i}_{j}" for i in range(n_warehouse))
        constraints.append(_con(f"demand_{j}", terms, lb=demand[j]))

    # Objective: minimize total transportation cost
    obj_terms: List[str] = []
    for i in range(n_warehouse):
        for j in range(n_customer):
            if cost[i][j] == 1:
                obj_terms.append(f"x_{i}_{j}")
            else:
                obj_terms.append(f"{cost[i][j]}*x_{i}_{j}")
    objective = ObjectiveFunction(
        sense=ObjectiveSense.MINIMIZE,
        expression=" + ".join(obj_terms),
    )

    problem = ProblemDefinition(
        name="Transportation Problem",
        variables=variables,
        constraints=constraints,
        objective=objective,
        metadata={"example_id": "transportation"},
    )

    return ExampleDetail(
        id="transportation",
        name="运输问题",
        description="3个仓库向4个客户运输货物，最小化总运输成本",
        problem_type="LP",
        recommended_algorithm="SimplexSolver / HiGHS",
        difficulty="easy",
        tags=["transportation", "logistics", "LP"],
        problem=problem,
        expected_objective=9100.0,
        explanation=(
            "运输问题是经典的线性规划问题。在供应链管理中，需要从多个仓库（供应地）"
            "向多个客户（需求地）运送货物，目标是在满足所有供给和需求约束下，"
            "最小化总运输成本。该问题具有整数最优性（在供需平衡时），"
            "因此即使作为连续LP求解，结果也自然满足整数要求。"
        ),
    )


# ---------------------------------------------------------------------------
# Example 2: Knapsack Problem
# ---------------------------------------------------------------------------

def _build_knapsack() -> ExampleDetail:
    weights = [10, 20, 30, 5, 15, 25, 8, 12, 18, 22]
    values = [60, 100, 120, 30, 80, 90, 45, 70, 85, 110]
    capacity = 50
    n = len(weights)

    # Variables: x_i ∈ {0, 1}
    variables: List[Variable] = []
    for i in range(n):
        variables.append(_var(f"x_{i}", lb=0.0, ub=1.0, vtype=VariableType.BINARY))

    # Capacity constraint: sum(weight_i * x_i) <= 50
    weight_terms = " + ".join(
        f"{weights[i]}*x_{i}" if weights[i] != 1 else f"x_{i}"
        for i in range(n)
    )
    constraints = [_con("capacity", weight_terms, ub=capacity)]

    # Objective: maximize total value
    value_terms = " + ".join(
        f"{values[i]}*x_{i}" if values[i] != 1 else f"x_{i}"
        for i in range(n)
    )
    objective = ObjectiveFunction(
        sense=ObjectiveSense.MAXIMIZE,
        expression=value_terms,
    )

    problem = ProblemDefinition(
        name="Knapsack Problem",
        variables=variables,
        constraints=constraints,
        objective=objective,
        metadata={"example_id": "knapsack"},
    )

    return ExampleDetail(
        id="knapsack",
        name="背包问题",
        description="10个物品选择放入背包，在容量限制下最大化总价值",
        problem_type="MILP",
        recommended_algorithm="BranchBoundSolver / OR-Tools",
        difficulty="easy",
        tags=["knapsack", "0-1 programming", "combinatorial"],
        problem=problem,
        expected_objective=285.0,
        explanation=(
            "背包问题是组合优化中最经典的0-1整数规划问题。给定一组物品，"
            "每个物品有重量和价值，选择一个子集装入容量有限的背包中，"
            "使总价值最大。该问题在资源分配、投资决策、货物装载等领域有广泛应用。"
            "本例中最优解为选取物品0,3,4,6,7，总重量恰好等于背包容量50，总价值285。"
        ),
    )


# ---------------------------------------------------------------------------
# Example 3: Production Scheduling
# ---------------------------------------------------------------------------

def _build_production_scheduling() -> ExampleDetail:
    profits = [12, 15, 8, 20, 10]
    machine_time = [
        [2, 3, 1, 4, 2],
        [3, 2, 4, 1, 3],
        [1, 4, 2, 3, 2],
    ]
    machine_available = [100, 120, 80]
    min_prod = [5, 3, 0, 2, 0]
    max_prod = [30, 25, 20, 15, 35]
    n_products = len(profits)
    n_machines = len(machine_available)

    # Variables: x_i (integer production quantity)
    variables: List[Variable] = []
    for i in range(n_products):
        variables.append(
            _var(f"x_{i}", lb=min_prod[i], ub=max_prod[i], vtype=VariableType.INTEGER)
        )

    # Machine time constraints: sum_j(time_ij * x_j) <= available_i
    constraints: List[Constraint] = []
    for m in range(n_machines):
        terms: List[str] = []
        for j in range(n_products):
            t = machine_time[m][j]
            if t == 1:
                terms.append(f"x_{j}")
            elif t != 0:
                terms.append(f"{t}*x_{j}")
        expr = " + ".join(terms) if terms else "0"
        constraints.append(_con(f"machine_{m}", expr, ub=machine_available[m]))

    # Objective: maximize total profit
    profit_terms = " + ".join(
        f"{profits[i]}*x_{i}" if profits[i] != 1 else f"x_{i}"
        for i in range(n_products)
    )
    objective = ObjectiveFunction(
        sense=ObjectiveSense.MAXIMIZE,
        expression=profit_terms,
    )

    problem = ProblemDefinition(
        name="Production Scheduling",
        variables=variables,
        constraints=constraints,
        objective=objective,
        metadata={"example_id": "production_scheduling"},
    )

    return ExampleDetail(
        id="production_scheduling",
        name="生产排程",
        description="5种产品在3台机器上加工，在机器时间约束下最大化总利润",
        problem_type="MILP",
        recommended_algorithm="BranchBoundSolver / PuLP",
        difficulty="medium",
        tags=["production", "scheduling", "MILP", "manufacturing"],
        problem=problem,
        expected_objective=573.0,
        explanation=(
            "生产排程是制造业中常见的混合整数线性规划问题。需要在有限的生产资源"
            "（机器工时）约束下，决定各种产品的生产数量，以最大化总利润。"
            "每种产品在不同机器上有不同的加工时间，同时需要满足最小和最大产量要求。"
            "该模型可扩展到考虑设置时间、顺序约束等更复杂场景。"
        ),
    )


# ---------------------------------------------------------------------------
# Example 4: Vehicle Routing Problem (VRP)
# ---------------------------------------------------------------------------

def _build_vrp() -> ExampleDetail:
    # Simplified VRP: 1 depot (node 0) + 4 customers (nodes 1-4), 2 vehicles
    n_nodes = 5
    n_vehicles = 2
    dist = [
        [0,  12, 18, 20, 15],
        [12,  0, 12,  8, 18],
        [18, 12,  0, 12, 12],
        [20,  8, 12,  0, 22],
        [15, 18, 12, 22,  0],
    ]
    n_customers = n_nodes - 1

    variables: List[Variable] = []
    constraints: List[Constraint] = []

    # Arc variables: y_{i}_{j}_{k}
    for i in range(n_nodes):
        for j in range(n_nodes):
            if i == j:
                continue
            for k in range(n_vehicles):
                variables.append(
                    _var(f"y_{i}_{j}_{k}", lb=0.0, ub=1.0, vtype=VariableType.BINARY)
                )

    # MTZ auxiliary variables: u_{i} for customers
    for i in range(1, n_nodes):
        variables.append(_var(f"u_{i}", lb=1.0, ub=float(n_customers)))

    # 1. Each customer visited exactly once
    for j in range(1, n_nodes):
        terms = []
        for i in range(n_nodes):
            if i == j:
                continue
            for k in range(n_vehicles):
                terms.append(f"y_{i}_{j}_{k}")
        constraints.append(_con(f"visit_{j}", " + ".join(terms), lb=1.0, ub=1.0))

    # 2. Each vehicle departs from depot
    for k in range(n_vehicles):
        terms = [f"y_0_{j}_{k}" for j in range(1, n_nodes)]
        constraints.append(_con(f"depart_{k}", " + ".join(terms), lb=1.0, ub=1.0))

    # 3. Each vehicle returns to depot
    for k in range(n_vehicles):
        terms = [f"y_{i}_0_{k}" for i in range(1, n_nodes)]
        constraints.append(_con(f"return_{k}", " + ".join(terms), lb=1.0, ub=1.0))

    # 4. Flow conservation
    for i in range(n_nodes):
        for k in range(n_vehicles):
            out_terms = [f"y_{i}_{j}_{k}" for j in range(n_nodes) if j != i]
            neg_in_terms = [f"-1*y_{j}_{i}_{k}" for j in range(n_nodes) if j != i]
            expr = " + ".join(out_terms + neg_in_terms)
            constraints.append(_con(f"flow_{i}_{k}", expr, lb=0.0, ub=0.0))

    # 5. MTZ subtour elimination
    for i in range(1, n_nodes):
        for j in range(1, n_nodes):
            if i == j:
                continue
            for k in range(n_vehicles):
                expr = f"u_{i} + -1*u_{j} + {n_customers}*y_{i}_{j}_{k}"
                constraints.append(_con(f"mtz_{i}_{j}_{k}", expr, ub=n_customers - 1))

    # Objective: minimize total distance
    obj_terms: List[str] = []
    for i in range(n_nodes):
        for j in range(n_nodes):
            if i == j:
                continue
            for k in range(n_vehicles):
                d = dist[i][j]
                if d == 1:
                    obj_terms.append(f"y_{i}_{j}_{k}")
                else:
                    obj_terms.append(f"{d}*y_{i}_{j}_{k}")
    objective = ObjectiveFunction(
        sense=ObjectiveSense.MINIMIZE,
        expression=" + ".join(obj_terms),
    )

    problem = ProblemDefinition(
        name="Vehicle Routing Problem",
        variables=variables,
        constraints=constraints,
        objective=objective,
        metadata={"example_id": "vrp"},
    )

    return ExampleDetail(
        id="vrp",
        name="车辆路径规划",
        description="1个配送中心、4个客户点、2辆车，最小化总行驶距离（简化VRP演示）",
        problem_type="MILP",
        recommended_algorithm="OR-Tools / GeneticAlgorithmSolver",
        difficulty="hard",
        tags=["VRP", "routing", "logistics", "MTZ"],
        problem=problem,
        expected_objective=80.0,
        explanation=(
            "车辆路径规划问题（VRP）是物流领域的核心优化问题。目标是为多辆车规划从配送中心"
            "出发并返回的路线，使每个客户恰好被访问一次，同时最小化总行驶距离。"
            "本例使用Miller-Tucker-Zemlin（MTZ）约束消除子回路，是VRP的经典建模方法。"
            "为便于快速演示，此处简化为4个客户点+2辆车的规模。实际应用中可扩展到"
            "数百个客户点，通常需要启发式或元启发式算法求解。"
        ),
    )


# ---------------------------------------------------------------------------
# Example 5: Resource Allocation
# ---------------------------------------------------------------------------

def _build_resource_allocation() -> ExampleDetail:
    resources = [100, 80, 200]
    project_min = [20, 15, 25, 10]
    revenue = [
        [5, 4, 3, 6],
        [8, 7, 5, 9],
        [3, 4, 6, 2],
    ]
    n_resources = len(resources)
    n_projects = len(project_min)

    # Variables: x_{i}_{j} in [0, 1]
    variables: List[Variable] = []
    for i in range(n_resources):
        for j in range(n_projects):
            variables.append(_var(f"x_{i}_{j}", lb=0.0, ub=1.0))

    # Resource constraints: sum_j x_{i}_{j} <= 1
    constraints: List[Constraint] = []
    for i in range(n_resources):
        terms = " + ".join(f"x_{i}_{j}" for j in range(n_projects))
        constraints.append(_con(f"resource_{i}", terms, ub=1.0))

    # Project demand constraints: sum_i resource_i * x_{i}_{j} >= project_min_j
    for j in range(n_projects):
        terms: List[str] = []
        for i in range(n_resources):
            r = resources[i]
            if r == 1:
                terms.append(f"x_{i}_{j}")
            else:
                terms.append(f"{r}*x_{i}_{j}")
        constraints.append(
            _con(f"project_demand_{j}", " + ".join(terms), lb=project_min[j])
        )

    # Objective: maximize total revenue
    obj_terms: List[str] = []
    for i in range(n_resources):
        for j in range(n_projects):
            if revenue[i][j] == 1:
                obj_terms.append(f"x_{i}_{j}")
            else:
                obj_terms.append(f"{revenue[i][j]}*x_{i}_{j}")
    objective = ObjectiveFunction(
        sense=ObjectiveSense.MAXIMIZE,
        expression=" + ".join(obj_terms),
    )

    problem = ProblemDefinition(
        name="Resource Allocation",
        variables=variables,
        constraints=constraints,
        objective=objective,
        metadata={"example_id": "resource_allocation"},
    )

    return ExampleDetail(
        id="resource_allocation",
        name="资源分配",
        description="将3种资源分配到4个项目，在资源约束和项目需求下最大化总收益",
        problem_type="LP",
        recommended_algorithm="InteriorPointSolver / SimplexSolver",
        difficulty="medium",
        tags=["resource allocation", "LP", "project management"],
        problem=problem,
        expected_objective=20.65,
        explanation=(
            "资源分配问题研究如何将有限的人力、设备和资金等资源最优地分配到多个项目中，"
            "以最大化总收益。每种资源有总量限制（不能超额分配），每个项目有最低资源需求"
            "（确保项目可运行）。该模型广泛用于项目管理、预算编制和战略规划。"
            "作为线性规划问题，它保证能找到全局最优解。"
        ),
    )


# ---------------------------------------------------------------------------
# Example 6: Portfolio Optimization
# ---------------------------------------------------------------------------

def _build_portfolio() -> ExampleDetail:
    returns = [0.12, 0.10, 0.07, 0.03, 0.15]
    target_return = 0.08
    n = len(returns)

    # Covariance matrix (symmetric positive definite, via Cholesky construction)
    sigma = [
        [0.0400, 0.0100, 0.0060, 0.0020, 0.0120],
        [0.0100, 0.0250, 0.0090, 0.0035, 0.0090],
        [0.0060, 0.0090, 0.0098, 0.0037, 0.0046],
        [0.0020, 0.0035, 0.0037, 0.0050, 0.0029],
        [0.0120, 0.0090, 0.0046, 0.0029, 0.0457],
    ]

    # Variables: w_i in [0, 1]
    variables: List[Variable] = []
    for i in range(n):
        variables.append(_var(f"w_{i}", lb=0.0, ub=1.0))

    # Constraint 1: sum(w_i) = 1
    sum_terms = " + ".join(f"w_{i}" for i in range(n))
    constraints = [_con("budget", sum_terms, lb=1.0, ub=1.0)]

    # Constraint 2: sum(return_i * w_i) >= target_return
    ret_terms = " + ".join(f"{returns[i]}*w_{i}" for i in range(n))
    constraints.append(_con("target_return", ret_terms, lb=target_return))

    # Objective: minimize w^T * Sigma * w
    # = sum_i sigma[i][i]*w_i^2 + 2*sum_{i<j} sigma[i][j]*w_i*w_j
    obj_terms: List[str] = []
    for i in range(n):
        obj_terms.append(f"{sigma[i][i]}*w_{i}**2")
    for i in range(n):
        for j in range(i + 1, n):
            coeff = 2.0 * sigma[i][j]
            coeff_str = f"{coeff:.10g}"
            obj_terms.append(f"{coeff_str}*w_{i}*w_{j}")
    objective = ObjectiveFunction(
        sense=ObjectiveSense.MINIMIZE,
        expression=" + ".join(obj_terms),
    )

    problem = ProblemDefinition(
        name="Portfolio Optimization",
        variables=variables,
        constraints=constraints,
        objective=objective,
        metadata={"example_id": "portfolio"},
    )

    return ExampleDetail(
        id="portfolio",
        name="投资组合优化",
        description="5只股票，最小化投资组合风险（方差），满足收益约束",
        problem_type="QP",
        recommended_algorithm="InteriorPointSolver / SciPy",
        difficulty="hard",
        tags=["portfolio", "QP", "finance", "risk management"],
        problem=problem,
        expected_objective=0.003688,
        explanation=(
            "投资组合优化是金融工程的核心问题（Markowitz均值-方差模型）。"
            "目标是在满足最低预期收益率的约束下，最小化投资组合的风险（用方差衡量）。"
            "协方差矩阵刻画了资产之间的相关性——分散投资可以降低风险。"
            "这是一个二次规划（QP）问题，目标函数为二次型，约束为线性。"
            "本例中5只股票的期望收益率从3%到15%不等，目标收益率为8%。"
        ),
    )


# ---------------------------------------------------------------------------
# Example Registry
# ---------------------------------------------------------------------------

_EXAMPLES: Dict[str, ExampleDetail] = {}


def _init_examples() -> None:
    """Build and register all example problems."""
    global _EXAMPLES
    builders = [
        _build_transportation,
        _build_knapsack,
        _build_production_scheduling,
        _build_vrp,
        _build_resource_allocation,
        _build_portfolio,
    ]
    for builder in builders:
        example = builder()
        _EXAMPLES[example.id] = example


_init_examples()


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@router.get("/examples", response_model=List[ExampleSummary])
async def list_examples():
    """Return a list of all available example problems (summary only)."""
    return [
        ExampleSummary(
            id=ex.id,
            name=ex.name,
            description=ex.description,
            problem_type=ex.problem_type,
            recommended_algorithm=ex.recommended_algorithm,
            difficulty=ex.difficulty,
            tags=ex.tags,
        )
        for ex in _EXAMPLES.values()
    ]


@router.get("/examples/{example_id}", response_model=ExampleDetail)
async def get_example(example_id: str):
    """Return the full detail of a specific example, including the ProblemDefinition.

    The returned `problem` field can be directly submitted to the /solve endpoint.
    """
    example = _EXAMPLES.get(example_id)
    if example is None:
        raise HTTPException(
            status_code=404,
            detail=f"Example '{example_id}' not found. "
                   f"Available: {list(_EXAMPLES.keys())}",
        )
    return example
