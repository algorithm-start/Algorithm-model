# RecPlatform 操作手册

## 1. 平台概述

### 1.1 系统简介

RecPlatform 是一款面向数学规划求解与数据集成的算法平台，提供以下核心能力：

- **优化求解**：支持线性规划、整数规划、二次规划等多种数学优化问题的高效求解
- **数据集成**：对接多种数据源，提供 ETL 管道设计与数据 API 发布能力
- **服务编排**：通过可视化流程编辑器组合求解、数据转换、外部服务调用等节点，构建自动化业务流程
- **可视化操作**：基于 Web 的图形化界面，无需编程即可完成建模、求解、数据流转等操作

### 1.2 登录与认证

**访问地址：**

| 环境 | 地址 |
|------|------|
| 生产环境 | http://localhost |
| 开发环境 | http://localhost:5173 |

**登录流程：**

1. 打开浏览器，访问上述地址
2. 进入登录页面，输入用户名和密码
3. 默认账号：`admin`，默认密码：`admin123`
4. 点击「登录」按钮，系统返回认证 Token

**Token 说明：**

- Token 有效期为 **24 小时**
- 空闲超过 **1 小时** 自动过期
- Token 过期后需重新登录或调用刷新接口续期

**登录 API：**

```bash
# 登录获取 Token
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# 刷新 Token
curl -X POST http://localhost/api/v1/auth/refresh \
  -H "Authorization: Bearer <token>"

# 登出
curl -X POST http://localhost/api/v1/auth/logout \
  -H "Authorization: Bearer <token>"
```

**密码修改：**

管理员可通过用户管理页面修改密码，也可调用 API：

```bash
curl -X PUT http://localhost/api/v1/iam/users/{id}/password \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"newPassword": "newPassword123"}'
```

### 1.3 界面布局

平台界面由三个主要区域组成：

- **左侧导航菜单**：包含 Dashboard、Solver、Orchestrator、Data、Admin 五大模块入口
- **顶部面包屑**：显示当前页面路径，方便快速定位
- **内容区域**：展示当前功能模块的操作界面

> 登录后默认进入 Dashboard 首页，展示平台概览信息。

---

## 2. 求解器模块

导航路径：**Solver → Problems**

### 2.1 问题列表

在「Problems」页面可查看所有已创建的优化问题，支持以下操作：

- **状态筛选**：通过下拉菜单按状态过滤问题
- **搜索**：输入关键词搜索问题名称
- **分页浏览**：底部翻页控件切换页码

**问题状态说明：**

| 状态 | 含义 |
|------|------|
| PENDING | 已提交，等待求解 |
| QUEUED | 已排入队列 |
| RUNNING | 正在求解中 |
| OPTIMAL | 已找到最优解 |
| FEASIBLE | 找到可行解（非最优） |
| INFEASIBLE | 问题不可行 |
| UNBOUNDED | 问题无界 |
| TIMEOUT | 求解超时 |
| ERROR | 求解出错 |
| CANCELLED | 已取消 |

### 2.2 创建优化问题

导航路径：**Solver → Create Problem**

通过 5 步向导创建优化问题：

**第 1 步：基本信息**

填写问题名称（必填）和描述信息。

**第 2 步：定义变量**

为问题添加决策变量，每个变量包含：

| 字段 | 说明 | 示例 |
|------|------|------|
| 名称 | 变量标识 | `x1` |
| 类型 | CONTINUOUS（连续）/ INTEGER（整数）/ BINARY（二进制） | `CONTINUOUS` |
| 下界 | 变量最小值，默认无限制 | `0` |
| 上界 | 变量最大值，默认无限制 | `100` |

**第 3 步：定义约束**

添加约束条件，每条约束包含：

| 字段 | 说明 | 示例 |
|------|------|------|
| 名称 | 约束标识 | `capacity_constraint` |
| 表达式 | 线性/非线性表达式 | `2*x1 + 3*x2` |
| 类型 | LEQ（≤）/ GEQ（≥）/ EQ（=） | `LEQ` |
| 右端值 | 约束右端常数 | `100` |

**第 4 步：定义目标函数**

设置优化目标：

| 字段 | 说明 | 示例 |
|------|------|------|
| 表达式 | 目标函数表达式 | `5*x1 + 4*x2` |
| 方向 | MINIMIZE（最小化）/ MAXIMIZE（最大化） | `MAXIMIZE` |

**第 5 步：选择算法**

可选择「自动推荐」或手动指定算法及参数（详见 2.3 节）。

### 2.3 算法选择与参数调优

**支持的算法：**

| 算法类型 | 代码 | 适用场景 |
|---------|------|---------|
| 线性规划 | LP | 连续变量线性目标与约束 |
| 混合整数线性规划 | MILP | 含整数/二进制变量的线性问题 |
| 混合整数非线性规划 | MINLP | 含整数变量与非线性约束 |
| 二次规划 | QP | 目标函数含二次项 |
| 二阶锥规划 | SOCP | 包含二阶锥约束 |
| 混合整数二次规划 | MIQP | 整数变量 + 二次目标 |
| 单纯形法 | SIMPLEX | 中小规模线性规划 |
| 分支定界法 | BB | 整数规划精确求解 |
| 内点法 | IPM | 大规模线性/二次规划 |
| 遗传算法 | GA | 复杂组合优化，全局搜索 |
| 模拟退火 | SA | 组合优化，避免局部最优 |
| 粒子群优化 | PSO | 连续空间全局优化 |
| 蚁群优化 | ACO | 路径/调度类组合优化 |

**自动推荐逻辑：**

系统根据问题定义中的变量类型、约束类型和目标函数特征自动判断问题类别，推荐最合适的算法。调用自动推荐 API 即可获取建议。

**关键调优参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| timeLimit | 求解时间上限（秒） | 无限制 |
| gapTolerance | MIP 间隙容忍度 | 1e-4 |
| maxIterations | 最大迭代次数 | 无限制 |
| threads | 并行线程数（0=自动） | 0 |
| customParams | 算法专属参数（键值对） | - |

### 2.4 执行求解与查看结果

**触发求解：**

1. 在问题详情页点击「Solve」按钮
2. 可在弹窗中调整求解参数
3. 确认后提交，问题状态变为 RUNNING

**状态监控：**

问题列表页和详情页实时显示求解状态，状态流转为：PENDING → QUEUED → RUNNING → OPTIMAL/FEASIBLE/INFEASIBLE/UNBOUNDED/TIMEOUT/ERROR

**查看结果：**

求解完成后，详情页展示：

- **目标值**：最优/可行目标函数值（objectiveValue）
- **变量值**：各决策变量的最优取值（result 中 JSON）
- **求解耗时**：格式化显示，如 "1.23s"
- **错误信息**：若求解失败，显示错误原因

### 2.5 API 调用示例

**创建优化问题：**

```bash
curl -X POST http://localhost/api/v1/solver/problems \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "problemName": "生产计划优化",
    "description": "最大化利润的生产计划",
    "problemDefinition": {
      "variables": [
        {"name": "x1", "type": "CONTINUOUS", "lowerBound": 0, "upperBound": 100},
        {"name": "x2", "type": "CONTINUOUS", "lowerBound": 0, "upperBound": 80}
      ],
      "constraints": [
        {"name": "c1", "expression": "2*x1 + 3*x2", "type": "LEQ", "rhs": 120},
        {"name": "c2", "expression": "x1 + x2", "type": "LEQ", "rhs": 50}
      ],
      "objective": {
        "expression": "5*x1 + 4*x2",
        "sense": "MAXIMIZE"
      }
    }
  }'
```

**提交求解：**

```bash
curl -X POST http://localhost/api/v1/solver/problems/{id}/solve \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "algorithmType": "SIMPLEX",
    "timeLimit": 300,
    "gapTolerance": 0.0001,
    "threads": 4
  }'
```

**查询结果：**

```bash
# 查询单个问题
curl http://localhost/api/v1/solver/problems/{id} \
  -H "Authorization: Bearer <token>"

# 列表查询（分页）
curl "http://localhost/api/v1/solver/problems?page=1&size=20" \
  -H "Authorization: Bearer <token>"

# 自动推荐算法
curl -X POST http://localhost/api/v1/solver/auto-select \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "variables": [{"name": "x1", "type": "CONTINUOUS"}],
    "constraints": [{"expression": "2*x1", "type": "LEQ", "rhs": 10}],
    "objective": {"expression": "x1", "sense": "MAXIMIZE"}
  }'

# 取消问题
curl -X DELETE http://localhost/api/v1/solver/problems/{id} \
  -H "Authorization: Bearer <token>"
```

---

## 3. 数据集成模块

导航路径：**Data → Sources / Pipelines / APIs / Query Explorer**

### 3.1 数据源管理

**支持的数据源类型：**

| 类型 | 标识 | 说明 |
|------|------|------|
| PostgreSQL | POSTGRESQL | 关系型数据库 |
| MySQL | MYSQL | 关系型数据库 |
| Oracle | ORACLE | 关系型数据库 |
| GaussDB | GAUSSDB | 华为关系型数据库 |
| ClickHouse | CLICKHOUSE | 列式分析数据库 |
| REST API | REST_API | HTTP 接口数据源 |
| Kafka | KAFKA | 消息流数据源 |
| MQTT | MQTT | IoT 消息协议 |
| CSV 文件 | FILE_CSV | 逗号分隔文本文件 |
| JSON 文件 | FILE_JSON | JSON 格式文件 |
| Parquet 文件 | FILE_PARQUET | 列式存储文件 |

**创建数据源：**

1. 进入 **Data → Sources** 页面
2. 点击「Create Source」按钮
3. 填写以下信息：

| 字段 | 说明 | 必填 |
|------|------|------|
| 名称 | 数据源标识 | 是 |
| 描述 | 数据源用途说明 | 否 |
| 类型 | 从下拉列表选择数据源类型 | 是 |
| 连接配置 | 根据类型不同，填写主机、端口、数据库名、用户名、密码等 | 是 |

4. 点击「保存」

**测试连接：**

在数据源详情页点击「Test Connection」，系统尝试连接并返回成功/失败结果。

```bash
curl -X POST http://localhost/api/v1/data/sources/{id}/test \
  -H "Authorization: Bearer <token>"
```

**查看元数据：**

查看数据源的表、列等结构信息：

```bash
curl http://localhost/api/v1/data/sources/{id}/metadata \
  -H "Authorization: Bearer <token>"
```

### 3.2 ETL 管道设计

导航路径：**Data → Pipelines → Pipeline Editor**

**可视化编辑器使用：**

1. 进入 **Data → Pipelines** 页面，点击「新建」或选择已有管道进入编辑器
2. 编辑器画布提供拖拽式操作，从左侧节点面板拖入节点到画布

**节点类型：**

| 分类 | 节点 | 说明 |
|------|------|------|
| 抽取 | 数据库查询 | 从关系型数据库执行 SQL 查询读取数据 |
| 抽取 | API 调用 | 调用 REST 接口获取数据 |
| 抽取 | 文件读取 | 读取 CSV/JSON/Parquet 文件 |
| 转换 | 过滤 | 按条件筛选行 |
| 转换 | 映射 | 字段重命名、类型转换、计算列 |
| 转换 | 连接 | 多数据源 JOIN 操作 |
| 转换 | 聚合 | 分组统计（SUM/AVG/COUNT 等） |
| 转换 | 自定义脚本 | 编写转换逻辑 |
| 加载 | 数据库写入 | 将结果写入目标数据库表 |
| 加载 | API 推送 | 将结果推送至外部接口 |
| 加载 | 文件导出 | 导出为 CSV/JSON/Parquet 文件 |

**连线规则：**

- 从上游节点的输出端口拖线至下游节点的输入端口
- 数据流向为从左到右，不可形成环路
- 一个节点可连接多个下游节点（扇出）
- 多个节点可连接同一下游节点（合并）

**执行模式：**

- **批处理**：一次性读取全部数据，处理后写入目标
- **流处理**：持续监听数据源，实时处理增量数据

### 3.3 管道执行与监控

**手动执行：**

```bash
curl -X POST http://localhost/api/v1/data/pipelines/{id}/run \
  -H "Authorization: Bearer <token>"
```

**停止执行：**

```bash
curl -X POST "http://localhost/api/v1/data/pipelines/{id}/stop?executionId={execId}" \
  -H "Authorization: Bearer <token>"
```

**查看执行状态：**

```bash
curl "http://localhost/api/v1/data/pipelines/{id}/status?executionId={execId}" \
  -H "Authorization: Bearer <token>"
```

**执行历史：**

```bash
curl http://localhost/api/v1/data/pipelines/{id}/history \
  -H "Authorization: Bearer <token>"
```

在前端界面中，Pipelines 列表页展示各管道的运行状态，点击进入可查看节点级别执行状态和完整执行历史。

### 3.4 数据 API 发布

导航路径：**Data → APIs**

**创建数据 API：**

1. 点击「新建 API」
2. 填写配置信息：

| 字段 | 说明 |
|------|------|
| 路径 | API 访问路径 |
| 方法 | HTTP 方法（GET/POST） |
| 关联数据源 | 选择已注册的数据源 |
| SQL 查询 | 绑定到该 API 的 SQL 语句 |
| 参数定义 | SQL 中的参数化占位符及类型 |

3. 保存后 API 状态为「草稿」

**测试 API：**

```bash
curl -X POST http://localhost/api/v1/data/apis/{id}/test \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"param1": "value1"}'
```

**发布/下线：**

```bash
# 发布 API
curl -X POST http://localhost/api/v1/data/apis/{id}/publish \
  -H "Authorization: Bearer <token>"
```

API 生命周期：草稿 → 已发布 → （下线）→ 草稿

**调用统计：**

```bash
curl http://localhost/api/v1/data/apis/{id}/stats \
  -H "Authorization: Bearer <token>"
```

### 3.5 OLAP 查询

导航路径：**Data → Query Explorer**

**操作步骤：**

1. 在 Query Explorer 页面选择目标数据源
2. 在 SQL 编辑器中编写查询语句
3. 点击「执行」按钮运行查询
4. 查询结果以表格形式展示，支持导出

**API 调用：**

```bash
# 执行查询
curl -X POST http://localhost/api/v1/data/query \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceId": 1, "sql": "SELECT * FROM orders LIMIT 10"}'

# 预览查询（限制返回行数）
curl -X POST http://localhost/api/v1/data/query/preview \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"sourceId": 1, "sql": "SELECT * FROM orders"}'
```

---

## 4. 编排引擎模块

导航路径：**Orchestrator → Flows / Executions**

### 4.1 流程列表

在 **Orchestrator → Flows** 页面查看所有已创建的流程，支持按分类和状态筛选：

| 状态 | 含义 |
|------|------|
| DRAFT | 草稿，可编辑 |
| PUBLISHED | 已发布，可执行 |

### 4.2 可视化流程设计

导航路径：**Orchestrator → Flow Editor**

**画布操作：**

- **拖拽**：从左侧节点面板拖拽节点到画布
- **缩放**：使用鼠标滚轮或工具栏缩放按钮
- **连线**：从节点输出端口拖拽到另一节点输入端口，建立数据/控制流向
- **删除**：选中节点或连线后按 Delete 键删除

**节点类型：**

| 节点类型 | 说明 | 配置项 |
|---------|------|--------|
| 开始节点 | 流程入口，定义输入参数 | 输入参数名称及类型 |
| 服务调用 | 调用外部 REST/gRPC 服务 | URL、HTTP 方法、请求头、请求体 |
| 求解器调用 | 调用优化求解器 | 问题 ID、算法配置 |
| 数据转换 | 数据映射、过滤、聚合 | 操作类型、表达式 |
| 决策网关 | 条件分支路由 | SpEL 条件表达式 |
| 循环 | 迭代执行子流程 | 集合变量、终止条件、最大迭代次数 |
| 脚本 | 执行自定义脚本 | 脚本语言（Groovy/JS）、脚本内容 |
| 子流程 | 嵌套调用其他已发布流程 | 子流程 ID、输入参数映射 |
| 并行分叉 | 并发执行多个分支 | - |
| 并行汇合 | 等待所有并行分支完成 | - |
| 结束节点 | 流程出口，定义输出映射 | 输出参数映射 |

**流程定义结构示例：**

```json
{
  "name": "生产优化流程",
  "description": "从数据源读取参数，调用求解器，推送结果",
  "definition": {
    "nodes": [],
    "edges": []
  },
  "category": "optimization",
  "tags": ["生产", "优化"]
}
```

### 4.3 流程发布与执行

**发布前校验：**

系统在发布时自动校验流程 DAG 合法性（无环路、有且仅有一个开始节点和结束节点、连线完整），校验通过后方可发布。

```bash
# 发布流程
curl -X POST http://localhost/api/v1/orchestrator/flows/{id}/publish \
  -H "Authorization: Bearer <token>"
```

**手动执行：**

```bash
curl -X POST http://localhost/api/v1/orchestrator/flows/{id}/run \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"inputParams": {"demand": 100, "capacity": 50}}'
```

**停止执行：**

```bash
curl -X POST "http://localhost/api/v1/orchestrator/flows/{id}/stop?executionId={execId}" \
  -H "Authorization: Bearer <token>"
```

**执行监控：**

```bash
# 查看流程执行列表
curl "http://localhost/api/v1/orchestrator/flows/{id}/executions?page=1&size=20" \
  -H "Authorization: Bearer <token>"

# 查看单次执行详情
curl http://localhost/api/v1/orchestrator/flows/executions/{executionId} \
  -H "Authorization: Bearer <token>"
```

前端 Executions 页面提供可视化执行追踪，可查看每个节点的运行状态、耗时和输出。

### 4.4 服务注册

导航路径：**Orchestrator → Services**

将外部 REST/gRPC 服务注册到平台，供流程中的「服务调用」节点引用。

**操作步骤：**

1. 点击「注册服务」
2. 填写服务名称、类型（REST/gRPC）、地址、认证信息等
3. 保存后可点击「测试」验证服务可用性

```bash
# 注册服务
curl -X POST http://localhost/api/v1/orchestrator/services \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "ERP服务", "type": "REST", "url": "http://erp.local/api", "status": "ACTIVE"}'

# 测试服务连通性
curl -X POST http://localhost/api/v1/orchestrator/services/{id}/test \
  -H "Authorization: Bearer <token>"

# 列表查询
curl "http://localhost/api/v1/orchestrator/services?page=1&size=20" \
  -H "Authorization: Bearer <token>"
```

### 4.5 模型注册

导航路径：**Orchestrator → Models**

将求解器模型注册到平台，供流程中的「求解器调用」节点引用，支持版本管理。

```bash
# 注册模型
curl -X POST http://localhost/api/v1/orchestrator/models \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "排产模型v2", "algorithmType": "MILP", "status": "ACTIVE"}'

# 列表查询（可按算法类型和状态筛选）
curl "http://localhost/api/v1/orchestrator/models?page=1&size=20&algorithmType=MILP&status=ACTIVE" \
  -H "Authorization: Bearer <token>"
```

---

## 5. 系统管理

导航路径：**Admin → Users / Roles / Audit Log / System**

### 5.1 用户管理

导航路径：**Admin → Users**

**操作说明：**

| 操作 | 方法 | 说明 |
|------|------|------|
| 创建用户 | 点击「新建用户」 | 填写用户名、密码、邮箱等 |
| 编辑用户 | 选中用户 → 编辑 | 修改基本信息 |
| 禁用用户 | 选中用户 → 删除 | 软删除，用户被禁用 |
| 分配角色 | 选中用户 → 分配角色 | 从已有角色列表中选择 |
| 修改密码 | 选中用户 → 修改密码 | 输入新密码 |

**API 示例：**

```bash
# 创建用户
curl -X POST http://localhost/api/v1/iam/users \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"username": "zhangsan", "password": "Pass123!", "email": "zhangsan@example.com"}'

# 用户列表（支持按用户名和状态筛选）
curl "http://localhost/api/v1/iam/users?username=zhang&page=1&size=20" \
  -H "Authorization: Bearer <token>"

# 分配角色
curl -X PUT http://localhost/api/v1/iam/users/{id}/roles \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"roleIds": [1, 2]}'

# 修改密码
curl -X PUT http://localhost/api/v1/iam/users/{id}/password \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"newPassword": "NewPass456!"}'
```

### 5.2 角色与权限

导航路径：**Admin → Roles**

**内置角色：**

| 角色 | 权限范围 |
|------|---------|
| ADMIN | 全部模块的完整操作权限 |
| USER | 各模块的操作权限（不含系统管理） |
| VIEWER | 所有模块的只读权限 |

**自定义角色：**

1. 点击「新建角色」
2. 输入角色名称和描述
3. 在权限树中勾选所需权限
4. 保存

**权限粒度：** 采用 `模块:资源:操作` 格式，如：

- `solver:problem:create` — 创建优化问题
- `solver:problem:delete` — 删除优化问题
- `data:source:read` — 查看数据源
- `orchestrator:flow:execute` — 执行流程

**API 示例：**

```bash
# 创建角色
curl -X POST http://localhost/api/v1/iam/roles \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "数据分析师", "description": "数据模块读写权限"}'

# 为角色分配权限
curl -X PUT http://localhost/api/v1/iam/roles/{id}/permissions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"permissionCodes": ["data:source:read", "data:source:create", "data:query:execute"]}'

# 查看权限树
curl http://localhost/api/v1/iam/permissions/tree \
  -H "Authorization: Bearer <token>"
```

### 5.3 审计日志

导航路径：**Admin → Audit Log**

**筛选条件：**

| 条件 | 说明 |
|------|------|
| 时间范围 | 起止时间筛选 |
| 用户 | 按操作人筛选 |
| 操作类型 | CREATE/UPDATE/DELETE/LOGIN 等 |
| 资源 | 按操作对象筛选 |
| 结果 | 成功/失败 |

**查看日志详情：** 点击日志条目可查看完整操作详情，包括请求参数、响应结果等。

**导出为 CSV：**

```bash
curl "http://localhost/api/v1/audit/logs/export?startTime=2026-01-01T00:00:00" \
  -H "Authorization: Bearer <token>" \
  -o audit_logs.csv
```

**查询审计统计：**

```bash
curl "http://localhost/api/v1/audit/logs/stats?startTime=2026-01-01T00:00:00&endTime=2026-05-29T23:59:59" \
  -H "Authorization: Bearer <token>"
```

### 5.4 告警规则

**创建告警规则：**

1. 定义告警条件（如求解超时次数超过阈值）
2. 设置严重级别（INFO/WARNING/CRITICAL）
3. 保存后规则默认启用

**启用/禁用规则：**

```bash
# 启用
curl -X PUT "http://localhost/api/v1/audit/alerts/{id}/enable?enabled=true" \
  -H "Authorization: Bearer <token>"

# 禁用
curl -X PUT "http://localhost/api/v1/audit/alerts/{id}/enable?enabled=false" \
  -H "Authorization: Bearer <token>"
```

**查看已触发告警：**

```bash
curl "http://localhost/api/v1/audit/alerts/triggered?page=1&size=20" \
  -H "Authorization: Bearer <token>"
```

**管理告警规则 API：**

```bash
# 创建规则
curl -X POST http://localhost/api/v1/audit/alerts \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "求解超时告警", "condition": "solver_timeout_count > 5", "severity": "WARNING"}'

# 列表查询
curl "http://localhost/api/v1/audit/alerts?page=1&size=20" \
  -H "Authorization: Bearer <token>"
```

---

## 附录：API 路径速查表

| 模块 | 基础路径 | 主要操作 |
|------|---------|---------|
| 认证 | /api/v1/auth | 登录、登出、刷新Token、获取用户信息 |
| 求解器 | /api/v1/solver | 问题CRUD、求解、算法列表、自动推荐 |
| 数据源 | /api/v1/data/sources | 数据源CRUD、测试连接、元数据查询 |
| 管道 | /api/v1/data/pipelines | 管道CRUD、执行、停止、状态、历史 |
| 数据API | /api/v1/data/apis | API CRUD、发布、测试、统计 |
| OLAP查询 | /api/v1/data/query | 执行查询、预览 |
| 流程编排 | /api/v1/orchestrator/flows | 流程CRUD、发布、执行、停止、执行历史 |
| 服务注册 | /api/v1/orchestrator/services | 服务CRUD、测试连通性 |
| 模型注册 | /api/v1/orchestrator/models | 模型CRUD |
| 用户管理 | /api/v1/iam/users | 用户CRUD、密码修改、角色分配 |
| 角色管理 | /api/v1/iam/roles | 角色CRUD、权限分配 |
| 权限管理 | /api/v1/iam/permissions | 权限列表、权限树 |
| 审计日志 | /api/v1/audit/logs | 日志查询、导出CSV、统计 |
| 告警规则 | /api/v1/audit/alerts | 规则CRUD、启用/禁用、触发记录 |
