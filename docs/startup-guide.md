# RecPlatform 启动手册

> 本手册指导你在开发环境和生产环境中启动 RecPlatform 算法平台，并涵盖国产化部署与常见问题排查。
> 
> **📌 首次启动必读**：如果在启动过程中遇到问题，请查看 [启动问题排查与修复手册](./startup-troubleshooting-guide.md)，其中包含了详细的错误信息、原因分析和修复方案。

---

## 1. 环境要求

### 1.1 硬件要求

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发环境（最低） | 4 核 | 8 GB | 50 GB |
| 生产环境（推荐） | 8 核 | 16 GB | 200 GB SSD |

> 运行 Solver 引擎求解任务时，内存建议不低于 16 GB，OR-Tools 等求解器对内存需求较高。

### 1.2 软件要求

| 软件 | 最低版本 | 用途 |
|------|----------|------|
| JDK | 17+ | 后端编译与运行 |
| Maven | 3.9+ | 后端项目构建 |
| Python | 3.10+ | Solver 求解引擎 |
| Node.js | 20+ | 前端开发与构建 |
| Docker | 24+ | 容器化部署 |
| Docker Compose | V2 | 多服务编排 |

### 1.3 端口清单

| 端口 | 服务 | 说明 |
|------|------|------|
| 8080 | rec-backend | Spring Boot API 服务 |
| 8000 | rec-solver-engine | FastAPI 求解引擎 |
| 80 | rec-frontend | Nginx 生产环境前端 |
| 5173 | Vite Dev Server | 前端开发服务器 |
| 5432 | PostgreSQL | 关系型数据库 |
| 6379 | Redis | 缓存与分布式锁 |
| 5672 | RabbitMQ | 消息队列 AMQP |
| 15672 | RabbitMQ Management | 管理控制台 |
| 9000 | MinIO API | 对象存储 API |
| 9001 | MinIO Console | 对象存储控制台 |
| 8123 | ClickHouse HTTP | 列式数据库 HTTP 接口 |

---

## 2. 开发环境启动

开发模式下基础设施通过 Docker Compose 启动，后端、前端和 Solver 引擎在本地运行。

### 2.1 启动基础设施

```bash
cd /path/to/recplatform
docker-compose up -d postgres redis rabbitmq minio clickhouse
```

等待健康检查通过后确认状态：`docker-compose ps`

各服务默认凭据：

| 服务 | 用户名 | 密码 |
|------|--------|------|
| PostgreSQL | `recplatform` | `recplatform123` |
| Redis | — | `redis123` |
| RabbitMQ | `recplatform` | `recplatform123` |
| MinIO | `recplatform` | `recplatform123` |

> 开发模式默认使用 H2 内存数据库，无需 PostgreSQL 亦可启动。如需连接真实 PostgreSQL，参见 2.2 节。

### 2.2 启动后端服务

```bash
cd recplatform-server
mvn clean compile
mvn clean install -DskipTests  # 必须执行，特别是修改了 orchestrator 模块后
mvn spring-boot:run -pl recplatform-web
```

> **重要提示**：
> 1. 执行 `mvn clean install` 是必须的，因为 orchestrator 模块的修改（如循环依赖修复）需要重新安装到本地 Maven 仓库
> 2. 如果遇到循环依赖错误（`UnsatisfiedDependencyException: circular reference`），代码已修复：
>    - 在 `SubFlowExecutor.java` 中添加了 `@Lazy` 注解
>    - 在 `application.yml` 中配置了 `spring.main.allow-circular-references: true`
> 3. 首次启动时会自动执行 `schema.sql` 初始化数据库表，如遇字段缺失错误，请检查 schema.sql 是否已更新

**默认配置（H2 内存数据库）：**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:recplatform;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
```

**连接 PostgreSQL 时通过环境变量覆盖：**

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/recplatform
export SPRING_DATASOURCE_USERNAME=recplatform
export SPRING_DATASOURCE_PASSWORD=recplatform123
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
export REDIS_HOST=localhost
export REDIS_PASSWORD=redis123
```

**Solver 引擎地址配置：**

```yaml
recplatform:
  solver:
    engine-url: ${SOLVER_ENGINE_URL:http://localhost:8000}
    default-timeout: 300
    max-concurrent-jobs: 10
```

### 2.3 启动 Solver 引擎

```bash
cd solver-engine
python3 -m venv venv
source venv/bin/activate
# 使用国内镜像加速安装（ortools 约 200MB，安装时间较长）
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
uvicorn api.main:app --reload --port 8000
```

主要依赖：fastapi 0.110.0、uvicorn 0.27.1、ortools 9.9.3963、highspy 1.7.1+、scipy 1.12.0、numpy 1.26.4、PuLP 2.8.0、grpcio 1.62.2、pydantic 2.6.4

> **安装提示**：
> 1. `ortools` 包约 200MB，安装时间较长，请确保网络通畅
> 2. 如果 ortools 安装失败，可先安装其他依赖：
>    ```bash
>    pip install fastapi uvicorn grpcio grpcio-tools scipy numpy PuLP pydantic python-dotenv -i https://pypi.tuna.tsinghua.edu.cn/simple
>    pip install ortools highspy -i https://pypi.tuna.tsinghua.edu.cn/simple
>    ```
> 3. highspy 版本必须 >= 1.7.1，1.7.0 版本不存在

### 2.4 启动前端

```bash
cd recplatform-ui
npm install
npm run dev
```

前端基于 Vue 3 + TypeScript + Vite，主要依赖：vue ^3.4.21、element-plus ^2.6.1、@vue-flow/core ^1.33.5、axios ^1.6.7、echarts ^5.5.0

### 2.5 启动验证

| 检查项 | 地址 | 说明 |
|--------|------|------|
| 后端服务 | 查看启动日志 | 应显示 "Started RecPlatformApplication in X seconds" |
| API 文档 | http://localhost:8080/doc.html | Knife4j API 文档 |
| 前端页面 | http://localhost:5173 | Vue 3 前端 |
| Solver 引擎 | `curl http://localhost:8000/health` | 应返回 `{"status":"ok"}` |
| H2 控制台 | http://localhost:8080/h2-console | JDBC URL: `jdbc:h2:mem:recplatform` |
| RabbitMQ 控制台 | http://localhost:15672 | `recplatform / recplatform123` |
| MinIO 控制台 | http://localhost:9001 | `recplatform / recplatform123` |

> **注意**：
> - 后端健康检查端点 `/api/v1/platform/health` 在某些版本中可能返回 500，但服务已正常启动
> - 判断后端是否启动成功应以启动日志为准：查找 "Started RecPlatformApplication" 和 "Tomcat started on port 8080"
> - 默认管理员账号：`admin / admin123`，首次登录后请立即修改密码

---

## 3. 生产环境部署

### 3.1 Docker Compose 一键启动

```bash
cd /path/to/recplatform
docker-compose up -d
```

**服务启动顺序：**

```
基础设施层（无依赖）：postgres → redis → rabbitmq → minio → clickhouse
应用层：solver-engine（无依赖）→ backend（依赖 postgres、redis healthy）→ frontend（依赖 backend）
```

查看状态：`docker-compose ps`，查看日志：`docker-compose logs -f backend`

### 3.2 环境变量配置

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `SPRING_PROFILES_ACTIVE` | — | Docker 环境设为 `docker` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/recplatform` | 数据库连接 URL |
| `SPRING_DATASOURCE_USERNAME` | `recplatform` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `recplatform123` | 数据库密码 |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` | 数据库驱动 |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 / `redis123` | Redis 密码（Docker 为 `redis123`） |
| `SOLVER_ENGINE_URL` | `http://localhost:8000` | Solver 引擎地址 |
| `PYTHONUNBUFFERED` | `1` | Python 输出缓冲（Solver 引擎） |
| `LOG_LEVEL` | `info` | 日志级别（Solver 引擎） |

> Docker 环境中服务间通过容器名通信（如 `postgres`、`redis`、`solver-engine`），而非 `localhost`。

### 3.3 数据持久化

| 卷名 | 容器路径 | 用途 |
|------|----------|------|
| `postgres_data` | `/var/lib/postgresql/data` | PostgreSQL 数据 |
| `redis_data` | `/data` | Redis 持久化 |
| `rabbitmq_data` | `/var/lib/rabbitmq` | RabbitMQ 数据 |
| `minio_data` | `/data` | MinIO 对象存储 |
| `clickhouse_data` | `/var/lib/clickhouse` | ClickHouse 数据 |

备份：`docker exec rec-postgres pg_dump -U recplatform recplatform > backup.sql`

清理所有数据：`docker-compose down -v`（危险操作）

### 3.4 首次启动初始化

- **Schema 初始化**：Spring Boot 通过 `schema.sql` 自动建表（`spring.sql.init.mode=always`）
- **默认管理员**：`admin / admin123`，首次登录后请立即修改密码
- **默认角色**：`ADMIN`、`USER`、`VIEWER`

---

## 4. 国产化部署

### 4.1 ARM64 架构支持

所有 Docker 镜像均支持 `linux/arm64`，已验证鲲鹏 920 与海光 DCU。

```bash
# 直接启动（自动拉取 ARM64 镜像）
docker-compose up -d

# 自定义构建 ARM64 镜像
docker buildx create --name arm64-builder --use
docker buildx build --platform linux/arm64 -t recplatform-backend:arm64 ./recplatform-server
docker buildx build --platform linux/arm64 -t recplatform-solver:arm64 ./solver-engine
docker buildx build --platform linux/arm64 -t recplatform-frontend:arm64 ./recplatform-ui
```

> `ortools` 在 ARM64 上可能需从源码编译，参见 5.6 节。

### 4.2 国产操作系统兼容

| 操作系统 | 版本 | 架构 | 兼容性 |
|----------|------|------|--------|
| 统信 UOS | V20 | x86_64 / ARM64 | 完全兼容 |
| 麒麟 V10 | SP1/SP2 | x86_64 / ARM64 | 完全兼容 |

部署步骤与标准 Linux 一致。注意：部分国产 OS 防火墙策略较严，需开放相关端口（见 1.3）。

---

## 5. 常见启动问题

### 5.1 端口被占用

**现象**：`Bind for 0.0.0.0:8080 failed: port is already allocated`

**原因**：本地已有进程占用端口。

**解决**：

```bash
lsof -i :8080          # 查找占用进程
kill -9 <PID>           # 终止进程
# 或修改端口
export SERVER_PORT=8081
mvn spring-boot:run -pl recplatform-web
```

### 5.2 数据库连接失败

**现象**：`PSQLException: Connection refused`

**原因**：PostgreSQL 未就绪或配置错误。

**解决**：

```bash
docker-compose ps postgres                    # 检查状态
docker-compose logs postgres                  # 查看日志
docker exec -it rec-postgres psql -U recplatform -d recplatform -c "SELECT 1"  # 验证连接
```

开发模式可使用默认 H2 内存数据库，无需配置 PostgreSQL。

### 5.3 Redis 连接拒绝

**现象**：`RedisConnectionException: Unable to connect to localhost:6379`

**原因**：Redis 未启动或未配置密码。

**解决**：

```bash
docker-compose up -d redis
docker exec -it rec-redis redis-cli -a redis123 ping  # 应返回 PONG
export REDIS_PASSWORD=redis123
```

### 5.4 npm 安装失败（网络问题）

**现象**：`npm ERR! ETIMEDOUT`

**原因**：国内网络访问 npmjs.org 超时。

**解决**：

```bash
npm config set registry https://registry.npmmirror.com
npm install
```

### 5.5 Maven 编译失败

**现象**：`invalid source release: 17`

**原因**：JDK 版本低于 17 或依赖下载失败。

**解决**：

```bash
java -version                              # 确认 JDK 17+
echo $JAVA_HOME                            # 确认路径正确
# 配置阿里云 Maven 镜像（~/.m2/settings.xml）
mvn clean compile -U                       # 清理重编译
```

### 5.6 Python 依赖安装失败（OR-Tools）

**现象**：`Could not find a version that satisfies the requirement ortools`

**原因**：平台无预编译 wheel 或网络问题。

**解决**：

```bash
# 使用国内镜像
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# ARM64 平台可能需要源码编译
sudo apt-get install cmake build-essential
pip install ortools --no-binary ortools

# 若仍失败，可先跳过 ortools，其他求解器仍可运行
pip install fastapi uvicorn highspy scipy numpy PuLP pydantic python-dotenv
```

### 5.7 Python 依赖安装失败（highspy）

**现象**：`Could not find a version that satisfies the requirement highspy==1.7.0`

**原因**：highspy 1.7.0 版本不存在于 PyPI。

**解决**：

```bash
# 修改 requirements.txt，将 highspy==1.7.0 改为 highspy==1.7.1 或更高版本
# 然后重新安装
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 可用版本：1.5.0, 1.5.2, 1.5.3, 1.7.1, 1.7.2, 1.8.0, 1.9.0, 1.10.0+
# 推荐直接安装最新稳定版
pip install highspy -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 5.8 数据库表结构缺失字段

**现象**：
```
Column "DELETED" not found; SQL statement:
SELECT COUNT( * ) AS total FROM sys_permission WHERE deleted=0

Column "CREATE_TIME" not found; SQL statement:
INSERT INTO sys_permission ( id, code, name, module, type, create_time, update_time ) VALUES (...)

Column "CREATE_BY" not found; SQL statement:
SELECT id,code,name,module,description,type,create_time,update_time,deleted,create_by,update_by FROM sys_permission
```

**原因**：`schema.sql` 中 IAM 相关表缺少 MyBatis-Plus 需要的审计字段（deleted, create_by, update_by, create_time, update_time）。

**解决**：

检查并更新 `recplatform-server/recplatform-web/src/main/resources/schema.sql`，确保以下表包含完整字段：

- `sys_user` - 需要 `create_by`, `update_by` 字段
- `sys_role` - 需要 `create_by`, `update_by` 字段
- `sys_permission` - 需要 `create_by`, `update_by`, `create_time`, `update_time`, `deleted` 字段
- `sys_user_role` - 需要 `create_time`, `update_time`, `deleted` 字段
- `sys_role_permission` - 需要 `create_time`, `update_time`, `deleted` 字段

完整表结构示例：
```sql
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY,
    code VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    module VARCHAR(100),
    description VARCHAR(500),
    type VARCHAR(30) DEFAULT 'API',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);
```

> **注意**：修复 schema.sql 后，如果是 H2 内存数据库，需要重启服务重新初始化。如果是 PostgreSQL，需要手动执行 ALTER TABLE 添加缺失字段或删除数据库让 Spring Boot 重新建表。

### 5.9 Spring Boot 循环依赖错误

**现象**：
```
APPLICATION FAILED TO START

The dependencies of some of the beans in the application context form a cycle:
┌─────┐
|  subFlowExecutor
↑     ↓
|  flowServiceImpl
↑     ↓
|  flowEngine
↑     ↓
|  nodeExecutorRegistry
└─────┘
```

**原因**：orchestrator 模块中存在循环依赖链：SubFlowExecutor → FlowService → FlowEngine → NodeExecutorRegistry → SubFlowExecutor

**解决**：

代码已修复，请确保：

1. **SubFlowExecutor.java 使用了 @Lazy 注解**：
```java
// 文件: recplatform-server/recplatform-orchestrator/src/main/java/com/recplatform/orchestrator/engine/nodes/SubFlowExecutor.java
public SubFlowExecutor(@Lazy FlowService flowService) {
    this.flowService = flowService;
}
```

2. **application.yml 中允许循环引用**：
```yaml
spring:
  main:
    allow-circular-references: true
```

3. **重新编译并安装**：
```bash
cd recplatform-server
mvn clean install -DskipTests  # 必须执行此命令
mvn spring-boot:run -pl recplatform-web
```

> **重要**：修改 orchestrator 模块后，必须执行 `mvn clean install` 重新安装到本地 Maven 仓库，否则修改不会生效。

---

## 附录：项目模块结构

```
recplatform/
├── docker-compose.yml                # Docker Compose 编排
├── recplatform-server/               # Java 后端（Maven 多模块）
│   ├── pom.xml                       # 父 POM
│   ├── recplatform-common/           # 公共模块
│   ├── recplatform-solver/           # 求解器集成
│   ├── recplatform-data/             # 数据管理
│   ├── recplatform-orchestrator/     # 编排调度
│   ├── recplatform-iam/              # 身份与访问管理
│   ├── recplatform-audit/            # 审计日志
│   └── recplatform-web/              # Web 启动模块
│       └── src/main/resources/
│           ├── application.yml           # 默认配置（H2 开发模式）
│           └── application-docker.yml    # Docker 环境配置
├── solver-engine/                    # Python 求解引擎
│   ├── requirements.txt
│   └── api/
└── recplatform-ui/                   # Vue 3 前端
    ├── package.json
    └── src/
```
