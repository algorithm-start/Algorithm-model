# RecPlatform 启动问题排查与修复手册

> 本文档记录了 RecPlatform 系统启动过程中遇到的所有问题、原因分析及修复方案，供后续参考。

---

## 问题清单

### 1. Spring Boot 循环依赖问题

**问题现象：**
```
APPLICATION FAILED TO START

Description:
The dependencies of some of the beans in the application context form a cycle:

┌─────┐
|  subFlowExecutor defined in URL [...]
↑     ↓
|  flowServiceImpl defined in URL [...]
↑     ↓
|  flowEngine defined in URL [...]
↑     ↓
|  nodeExecutorRegistry defined in URL [...]
└─────┘
```

**问题原因：**

循环依赖链：
- `SubFlowExecutor` 依赖 `FlowService`
- `FlowServiceImpl` 依赖 `FlowEngine`
- `FlowEngine` 依赖 `NodeExecutorRegistry`
- `NodeExecutorRegistry` 依赖所有 `NodeExecutor`（包括 `SubFlowExecutor`）

这是一个典型的构造函数注入导致的循环依赖问题。

**修复方案：**

在 `SubFlowExecutor.java` 中使用 `@Lazy` 注解延迟加载 `FlowService`：

```java
// 文件: recplatform-server/recplatform-orchestrator/src/main/java/com/recplatform/orchestrator/engine/nodes/SubFlowExecutor.java

import org.springframework.context.annotation.Lazy;

@Slf4j
@Component
public class SubFlowExecutor implements NodeExecutor {

    private final FlowService flowService;

    // 添加 @Lazy 注解
    public SubFlowExecutor(@Lazy FlowService flowService) {
        this.flowService = flowService;
    }
    
    // ... 其他代码
}
```

**同时配置允许循环引用（辅助方案）：**

```yaml
# 文件: recplatform-server/recplatform-web/src/main/resources/application.yml
spring:
  main:
    allow-circular-references: true
```

**验证方法：**
```bash
cd recplatform-server
mvn clean install -DskipTests
mvn spring-boot:run -pl recplatform-web
```

---

### 2. 数据库表结构缺失字段问题

#### 2.1 sys_permission 表缺失字段

**问题现象：**
```
Column "DELETED" not found; SQL statement:
SELECT COUNT( * ) AS total FROM sys_permission WHERE deleted=0

Column "CREATE_TIME" not found; SQL statement:
INSERT INTO sys_permission ( id, code, name, module, type, create_time, update_time ) VALUES (...)

Column "CREATE_BY" not found; SQL statement:
SELECT id,code,name,module,description,type,create_time,update_time,deleted,create_by,update_by FROM sys_permission
```

**问题原因：**

MyBatis-Plus 配置了逻辑删除功能（`logic-delete-field: deleted`），并且实体类包含审计字段（createBy, updateBy, createTime, updateTime），但数据库表定义中缺少这些字段。

**修复方案：**

```sql
-- 文件: recplatform-server/recplatform-web/src/main/resources/schema.sql

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY,
    code VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    module VARCHAR(100),
    description VARCHAR(500),
    type VARCHAR(30) DEFAULT 'API',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,      -- 添加
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,      -- 添加
    deleted INT DEFAULT 0,                                -- 添加
    create_by VARCHAR(100),                               -- 添加
    update_by VARCHAR(100)                                -- 添加
);
```

---

#### 2.2 sys_role 表缺失字段

**问题现象：**
```
Column "CREATE_BY" not found; SQL statement:
SELECT id,code,name,description,status,create_time,update_time,deleted,create_by,update_by FROM sys_role
```

**修复方案：**

```sql
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),      -- 添加
    update_by VARCHAR(100)       -- 添加
);
```

---

#### 2.3 sys_user 表缺失字段

**问题现象：**

类似 sys_role，缺少 create_by 和 update_by 字段。

**修复方案：**

```sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(100),
    email VARCHAR(200),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),      -- 添加
    update_by VARCHAR(100)       -- 添加
);
```

---

#### 2.4 sys_user_role 表缺失字段

**问题现象：**
```
Column "CREATE_TIME" not found; SQL statement:
INSERT INTO sys_user_role ( id, user_id, role_id, create_time, update_time ) VALUES (...)
```

**修复方案：**

```sql
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 添加
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 添加
    deleted INT DEFAULT 0                              -- 添加
);
```

---

#### 2.5 sys_role_permission 表缺失字段

**问题现象：**

类似 sys_user_role，缺少时间戳和逻辑删除字段。

**修复方案：**

```sql
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_code VARCHAR(200) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 添加
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 添加
    deleted INT DEFAULT 0                              -- 添加
);
```

---

### 3. Python 依赖版本问题

#### 3.1 highspy 版本不存在

**问题现象：**
```
ERROR: Could not find a version that satisfies the requirement highspy==1.7.0
ERROR: No matching distribution found for highspy==1.7.0
```

**问题原因：**

`highspy` 1.7.0 版本在 PyPI 上不存在。可用版本包括：1.5.0, 1.5.2, 1.5.3, 1.7.1, 1.7.2, 1.8.0, 1.9.0, 1.10.0, 1.11.0, 1.12.0, 1.13.0, 1.13.1, 1.14.0

**修复方案：**

修改 `requirements.txt`，将版本更新为存在的版本：

```txt
# 文件: solver-engine/requirements.txt

fastapi==0.110.0
uvicorn[standard]==0.27.1
grpcio==1.62.2
grpcio-tools==1.62.2
ortools==9.9.3963
highspy==1.7.1          # 从 1.7.0 改为 1.7.1（实际安装时会自动使用最新兼容版本）
scipy==1.12.0
numpy==1.26.4
PuLP==2.8.0
pydantic==2.6.4
python-dotenv==1.0.1
```

**安装命令：**
```bash
cd solver-engine
python3 -m venv venv
source venv/bin/activate

# 使用国内镜像加速
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 或者先安装基础包，再安装 ortools 和 highspy（因为 ortools 包很大）
pip install fastapi uvicorn grpcio grpcio-tools scipy numpy PuLP pydantic python-dotenv -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install ortools highspy -i https://pypi.tuna.tsinghua.edu.cn/simple
```

---

### 4. 端口占用问题

**问题现象：**
```
Web server failed to start. Port 8080 was already in use.
```

**问题原因：**

之前启动的后端进程未正确关闭，导致端口被占用。

**修复方案：**

**方案 1：释放端口**
```bash
# 查找占用端口的进程
lsof -i :8080

# 终止进程
lsof -ti :8080 | xargs kill -9
```

**方案 2：更换端口**
```bash
# 通过环境变量指定新端口
export SERVER_PORT=8081

# 重新启动
cd recplatform-server
mvn spring-boot:run -pl recplatform-web
```

---

## 启动检查清单

### 前置条件检查

```bash
# 1. 检查 JDK 版本
java -version  # 需要 17+

# 2. 检查 Maven 版本
mvn -version   # 需要 3.9+

# 3. 检查 Python 版本
python3 --version  # 需要 3.10+

# 4. 检查 Node.js 版本
node --version     # 需要 20+

# 5. 检查 Docker
docker --version
docker-compose version
```

### 基础设施启动检查

```bash
# 启动基础设施
cd /path/to/recplatform
docker-compose up -d postgres redis rabbitmq minio clickhouse

# 检查服务状态
docker-compose ps

# 检查健康状态
docker exec -it rec-postgres psql -U recplatform -d recplatform -c "SELECT 1"
docker exec -it rec-redis redis-cli -a redis123 ping
curl http://localhost:15672  # RabbitMQ
curl http://localhost:9001   # MinIO
```

### 后端启动检查

```bash
cd recplatform-server

# 1. 编译项目
mvn clean compile

# 2. 安装到本地仓库（修复循环依赖后必须执行）
mvn clean install -DskipTests

# 3. 启动服务
mvn spring-boot:run -pl recplatform-web

# 4. 检查启动日志（应包含以下内容）
# - "Started RecPlatformApplication in X seconds"
# - "Tomcat started on port 8080"
# - "Created default admin user (admin/admin123)"
# - "IAM default data initialization completed"
```

### Solver 引擎启动检查

```bash
cd solver-engine

# 1. 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 2. 安装依赖
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 3. 启动服务
uvicorn api.main:app --reload --port 8000

# 4. 健康检查
curl http://localhost:8000/health
# 预期输出: {"status":"ok"}
```

### 前端启动检查

```bash
cd recplatform-ui

# 1. 安装依赖（如需要，使用国内镜像）
npm config set registry https://registry.npmmirror.com
npm install

# 2. 启动开发服务器
npm run dev

# 3. 检查访问
curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/
# 预期输出: 200
```

---

## 常见错误速查表

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| `UnsatisfiedDependencyException: circular reference` | Spring Bean 循环依赖 | 使用 `@Lazy` 注解或配置 `allow-circular-references: true` |
| `Column "XXX" not found` | 数据库表结构不完整 | 补充缺失的字段（deleted, create_by, update_by 等） |
| `Could not find a version that satisfies the requirement` | Python 包版本不存在 | 修改 requirements.txt 使用可用版本 |
| `Port 8080 was already in use` | 端口被占用 | 使用 `lsof -ti :8080 \| xargs kill -9` 释放端口 |
| `RedisConnectionException` | Redis 未启动或密码错误 | 启动 Redis 并配置正确的密码 |
| `PSQLException: Connection refused` | PostgreSQL 未就绪 | 检查 Docker 容器状态 `docker-compose ps postgres` |
| `npm ERR! ETIMEDOUT` | 网络问题 | 使用淘宝镜像 `npm config set registry https://registry.npmmirror.com` |
| `invalid source release: 17` | JDK 版本不对 | 安装 JDK 17+ 并配置 JAVA_HOME |

---

## 配置文件参考

### application.yml 关键配置

```yaml
server:
  port: 8080

spring:
  main:
    allow-circular-references: true  # 允许循环依赖（临时方案）
  datasource:
    url: jdbc:h2:mem:recplatform;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted        # 逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0

recplatform:
  solver:
    engine-url: ${SOLVER_ENGINE_URL:http://localhost:8000}
    default-timeout: 300
    max-concurrent-jobs: 10
```

### schema.sql IAM 表完整定义

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(100),
    email VARCHAR(200),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- 权限表
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

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_code VARCHAR(200) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);
```

---

## 修复提交记录

如需查看具体的代码修复，请参考以下文件：

1. **循环依赖修复**
   - `recplatform-server/recplatform-orchestrator/src/main/java/com/recplatform/orchestrator/engine/nodes/SubFlowExecutor.java`

2. **数据库表结构修复**
   - `recplatform-server/recplatform-web/src/main/resources/schema.sql`

3. **Python 依赖版本修复**
   - `solver-engine/requirements.txt`

4. **Spring Boot 配置修复**
   - `recplatform-server/recplatform-web/src/main/resources/application.yml`

---

## 总结

本次启动过程中遇到的主要问题包括：

1. **架构设计问题**：循环依赖需要通过 `@Lazy` 注解或重构代码解决
2. **数据库 schema 不完整**：IAM 相关表缺少 MyBatis-Plus 需要的审计字段
3. **依赖版本管理**：Python 包版本需要定期检查更新
4. **开发环境清理**：及时释放占用的端口

**建议改进措施：**

- [ ] 重构 orchestrator 模块，彻底消除循环依赖
- [ ] 完善 schema.sql，确保所有表结构包含完整的审计字段
- [ ] 定期更新 requirements.txt 中的依赖版本
- [ ] 添加启动脚本自动检查端口占用情况
- [ ] 编写集成测试验证启动流程

---

**文档版本**: v1.0  
**最后更新**: 2026-05-29  
**维护者**: RecPlatform Team
