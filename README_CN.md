# RecPlatform

**[English](README.md)** | **[中文文档](README_CN.md)**

RecPlatform 是一款开源的算法优化与流程编排平台，提供从定义优化问题、配置求解算法、构建数据管道到通过可视化 DAG 编辑器实现端到端自动化的完整解决方案。

## 功能特性

- **求解引擎** - 定义优化问题（LP/MILP/QP），支持变量、约束和目标函数。内置 10+ 求解器后端，包括 HiGHS、OR-Tools、PuLP、SciPy、遗传算法、模拟退火等。
- **流程编排** - 基于可视化 DAG 的流程编辑器，提供 10+ 节点类型：服务调用、求解器调用、数据转换、脚本、决策网关、循环、并行分叉/汇合、子流程。
- **数据管理** - 对接多种数据源（PostgreSQL、MySQL、ClickHouse、REST API、CSV），构建 ETL 管道、发布数据 API、执行即席 SQL 查询。
- **系统管理** - 用户管理、基于角色的访问控制（RBAC）、工作空间隔离、全面的审计日志。
- **系统监控** - 实时服务健康检查、JVM 指标、CPU/内存/磁盘使用率监控。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + Sa-Token |
| 求解引擎 | Python 3 + FastAPI + HiGHS/OR-Tools/PuLP/SciPy |
| 数据库 | PostgreSQL 15 + Redis 7 |
| 消息队列 | RabbitMQ 3 |
| 对象存储 | MinIO |
| 部署方式 | Docker Compose + Nginx |

## 项目结构

```
recplatform/
├── recplatform-server/          # Java 后端（Spring Boot）
│   ├── recplatform-common/      # 公共工具类、枚举、异常
│   ├── recplatform-iam/         # 身份与访问管理
│   ├── recplatform-audit/       # 审计日志与告警
│   ├── recplatform-orchestrator/# 流程编排引擎
│   ├── recplatform-solver/      # 求解服务集成
│   ├── recplatform-data/        # 数据源、管道、API 管理
│   └── recplatform-web/         # 应用入口、配置、系统信息
├── recplatform-ui/              # Vue 3 前端应用
├── solver-engine/               # Python 求解引擎（FastAPI）
├── offline-deploy/              # 离线部署脚本
├── docker-compose.yml           # Docker Compose 配置
└── docs/                        # 文档目录
```

## 快速开始

### 前置条件

- Docker & Docker Compose v2+
- （本地开发）JDK 17+、Node.js 18+、Python 3.10+

### Docker Compose 部署

```bash
# 克隆仓库
git clone https://github.com/algorithm-start/Algorithm-model.git
cd Algorithm-model

# 启动所有服务
docker compose up -d

# 查看服务状态
docker compose ps
```

所有服务启动后，打开浏览器访问：

- **前端界面**: http://localhost
- **后端 API**: http://localhost:8080
- **求解引擎**: http://localhost:8000
- **RabbitMQ 控制台**: http://localhost:15672 (recplatform / recplatform123)
- **MinIO 控制台**: http://localhost:9001 (recplatform / recplatform123)

默认登录账号：`admin` / `admin123`

### 本地开发

**后端：**

```bash
cd recplatform-server
./mvnw spring-boot:run -pl recplatform-web -Dspring-boot.run.profiles=dev
```

**前端：**

```bash
cd recplatform-ui
npm install
npm run dev
```

**求解引擎：**

```bash
cd solver-engine
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
```

## 系统架构

```
                    +-----------+
                    |  Nginx    |
                    | (端口 80)  |
                    +-----+-----+
                          |
              +-----------+-----------+
              |                       |
       +------+------+       +-------+-------+
       |    前端      |       |     后端      |
       |   (Vue 3)   |       | (Spring Boot) |
       +--------------+       +---+---+---+---+
                                  |   |   |
                    +-------------+   |   +-------------+
                    |                 |                  |
             +------+------+  +------+------+   +------+------+
             |   求解引擎    |  | PostgreSQL  |   |    Redis    |
             |  (FastAPI)   |  |             |   |             |
             +--------------+  +-------------+   +-------------+
```

## 环境变量配置（Docker）

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://postgres:5432/recplatform` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `recplatform` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `recplatform123` |
| `REDIS_HOST` | Redis 主机 | `redis` |
| `REDIS_PASSWORD` | Redis 密码 | `redis123` |
| `SOLVER_ENGINE_URL` | 求解引擎地址 | `http://solver-engine:8000` |

## 详细文档

| 文档 | 说明 |
|------|------|
| [操作手册](docs/user-operation-guide.md) | 系统功能操作指南，包含求解器、数据集成、流程编排、系统管理等模块的详细使用说明 |
| [启动指南](docs/startup-guide.md) | Docker Compose 部署和本地开发的详细启动步骤 |
| [启动故障排查](docs/startup-troubleshooting-guide.md) | 常见启动问题排查与解决方案 |
| [运维手册](docs/operations-manual.md) | 生产环境运维操作手册 |

## 参与贡献

欢迎提交 Pull Request 参与贡献！

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交你的修改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

## 开源协议

本项目基于 MIT 开源协议 - 详见 [LICENSE](LICENSE) 文件。
