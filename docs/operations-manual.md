# RecPlatform 运维手册

> 本手册面向运维人员，涵盖 RecPlatform 算法平台的架构说明、日常监控、日志管理、备份恢复、扩缩容、安全运维、版本升级及故障处理等内容。

---

## 1. 系统架构

### 1.1 服务拓扑

```
                        ┌─────────────────────┐
                        │     用户浏览器       │
                        └──────────┬──────────┘
                                   │ HTTP
                        ┌──────────▼──────────┐
                        │   Nginx(:80)        │
                        │   rec-frontend      │
                        └───┬─────────────┬───┘
                   /api/    │             │  /
              ┌─────────────▼───┐    ┌────▼───────────┐
              │  Backend(:8080) │    │  静态资源/SPA   │
              │  rec-backend    │    └────────────────┘
              └──┬──┬──┬──┬────┘
                 │  │  │  │
        ┌────────┘  │  │  └──────────────┐
        ▼           ▼  ▼                 ▼
┌───────────────┐ ┌────────────┐ ┌──────────────────┐
│ PostgreSQL    │ │ Redis      │ │ Solver-Engine    │
│ (:5432)       │ │ (:6379)    │ │ (:8000)          │
│ rec-postgres  │ │ rec-redis  │ │ rec-solver-engine│
└───────────────┘ └────────────┘ └──┬──┬──┬─────────┘
                                     │  │  │
                          ┌──────────┘  │  └──────┐
                          ▼             ▼         ▼
                   ┌────────────┐ ┌─────────┐ ┌───────────┐
                   │ RabbitMQ   │ │ MinIO   │ │ ClickHouse│
                   │ :5672/15672│ │ :9000   │ │ :8123     │
                   │ rec-rabbitmq│ │ rec-minio│ │ rec-clickhouse│
                   └────────────┘ └─────────┘ └───────────┘
```

### 1.2 服务清单

| 服务名称 | 容器名 | 端口映射 | 镜像 | 职责 | 依赖 |
|---------|--------|---------|------|------|------|
| frontend | rec-frontend | 80:80 | 自建 (Nginx) | 前端静态资源托管、API 反向代理 | backend |
| backend | rec-backend | 8080:8080 | 自建 (Java/Spring Boot) | 核心业务 API、认证授权、任务调度 | postgres, redis |
| solver-engine | rec-solver-engine | 8000:8000 | 自建 (Python/FastAPI) | 算法求解引擎、异步任务执行 | rabbitmq, minio, clickhouse |
| postgres | rec-postgres | 5432:5432 | postgres:15-alpine | 主数据库 | - |
| redis | rec-redis | 6379:6379 | redis:7-alpine | 会话管理、分布式缓存与锁 | - |
| rabbitmq | rec-rabbitmq | 5672:5672, 15672:15672 | rabbitmq:3-management-alpine | 异步任务消息队列 | - |
| minio | rec-minio | 9000:9000, 9001:9001 | minio/minio | 对象存储（数据文件、导出结果） | - |
| clickhouse | rec-clickhouse | 8123:8123, 9009:9000 | clickhouse/clickhouse-server:23.8 | OLAP 分析查询引擎 | - |

**Docker 网络：** 所有服务通过 `rec-network`（bridge 驱动）互联，服务间通过容器名解析。

**数据卷：** `postgres_data`、`redis_data`、`rabbitmq_data`、`minio_data`、`clickhouse_data`

### 1.3 中间件说明

- **PostgreSQL 15**：主数据库，存储用户、问题、管道（pipeline）、流程等核心业务数据。H2 内存数据库仅用于本地开发，Docker 环境下自动切换为 PostgreSQL。
- **Redis 7**：Sa-Token 会话管理（Token 存储于 Redis）、分布式缓存（查询结果缓存）、分布式锁（Redisson 实现，防止管道重复执行）。
- **RabbitMQ 3**：异步任务队列，承载管道执行任务与求解任务的异步分发与消费，支持死信队列与延迟消息。
- **MinIO**：兼容 S3 协议的对象存储，存储上传的数据文件、算法模型文件及导出结果。控制台端口 9001。
- **ClickHouse 23.8**：OLAP 列式数据库，用于海量数据分析查询和报表统计，HTTP 接口端口 8123，原生协议端口 9009（映射为容器内 9000）。

---

## 2. 日常监控

### 2.1 服务健康检查

**应用层健康检查：**

```bash
# 后端平台健康接口
curl -s http://localhost:8080/api/v1/platform/health

# Solver 引擎健康检查
curl -s http://localhost:8000/health

# 前端页面可访问性
curl -s -o /dev/null -w "%{http_code}" http://localhost:80/
```

**Docker 容器状态：**

```bash
# 查看所有容器状态
docker-compose -f /path/to/docker-compose.yml ps

# 查看容器健康状态（含健康检查结果）
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**中间件健康检查：**

```bash
# PostgreSQL
docker exec rec-postgres pg_isready -U recplatform

# Redis
docker exec rec-redis redis-cli -a redis123 ping

# RabbitMQ
docker exec rec-rabbitmq rabbitmq-diagnostics check_running

# ClickHouse
docker exec rec-clickhouse clickhouse-client --query "SELECT 1"

# MinIO（HTTP 接口）
curl -s http://localhost:9000/minio/health/live
```

### 2.2 日志查看

```bash
# 后端日志（实时跟踪最近 100 行）
docker logs rec-backend --tail 100 -f

# Solver 引擎日志
docker logs rec-solver-engine --tail 100 -f

# Nginx 访问日志
docker logs rec-frontend --tail 100 -f

# PostgreSQL 慢查询日志
docker logs rec-postgres --tail 200 | grep "duration"

# Redis 日志
docker logs rec-redis --tail 100

# RabbitMQ 日志
docker logs rec-rabbitmq --tail 100

# ClickHouse 日志
docker logs rec-clickhouse --tail 100
```

### 2.3 关键监控指标

| 指标类别 | 监控项 | 获取方式 |
|---------|--------|---------|
| JVM | 堆内存使用 / GC 频率 / 线程数 | `jstat -gc <pid>`, JMX, Prometheus JMX Exporter |
| 连接池 | 活跃连接数 / 等待队列 | HikariCP metrics, `/actuator/metrics` |
| Redis | 内存使用 / 连接数 / 命中率 | `docker exec rec-redis redis-cli -a redis123 info memory` |
| PostgreSQL | 活跃连接 / 慢查询 | `pg_stat_activity`, `pg_stat_statements` |
| RabbitMQ | 队列深度 / 消费速率 | 管理面板 http://localhost:15672 |
| 磁盘 | Volume 使用率 | `docker system df`, `df -h` |

### 2.4 监控命令速查表

| 用途 | 命令 |
|------|------|
| 查看容器状态 | `docker-compose ps` |
| 查看容器资源占用 | `docker stats --no-stream` |
| 查看磁盘使用 | `df -h` |
| 查看 Docker 磁盘占用 | `docker system df` |
| 查看 Redis 内存 | `docker exec rec-redis redis-cli -a redis123 info memory \| grep used_memory_human` |
| 查看 PG 活跃连接 | `docker exec rec-postgres psql -U recplatform -c "SELECT count(*) FROM pg_stat_activity;"` |
| 查看 RabbitMQ 队列 | `curl -s -u recplatform:recplatform123 http://localhost:15672/api/queues \| python3 -m json.tool` |
| 查看 JVM 堆内存 | `docker exec rec-backend jcmd 1 GC.heap_info 2>/dev/null \|\| docker exec rec-backend jstat -gc 1` |

---

## 3. 日志管理

### 3.1 日志级别配置

**当前配置（application-docker.yml）：**

```yaml
logging:
  level:
    com.recplatform: INFO
    org.springframework: WARN
```

**各环境日志级别建议：**

| 模块 | 开发环境 | 测试环境 | 生产环境 |
|------|---------|---------|---------|
| com.recplatform | DEBUG | INFO | INFO |
| org.springframework | INFO | WARN | WARN |
| com.baomidou | DEBUG | INFO | WARN |

**动态调整日志级别（Spring Boot Actuator）：**

```bash
# 查看当前日志级别
curl http://localhost:8080/actuator/loggers/com.recplatform

# 动态修改为 DEBUG（无需重启）
curl -X POST http://localhost:8080/actuator/loggers/com.recplatform \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

> 注意：需确保 `spring-boot-starter-actuator` 已引入且 `management.endpoints.web.exposure.include` 包含 `loggers`。

### 3.2 日志轮转

**Docker 日志驱动配置**（在 docker-compose.yml 中为每个服务添加）：

```yaml
services:
  backend:
    logging:
      driver: json-file
      options:
        max-size: "50m"
        max-file: "5"
```

**应用 Logback 配置建议**（`logback-spring.xml`）：

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/recplatform/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>/var/log/recplatform/application.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
        <totalSizeCap>2GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

### 3.3 审计日志

- 审计日志存储在 `audit_log` 表中，记录用户操作行为
- **保留策略建议**：在线保留 90 天，超期数据归档至 MinIO 对象存储
- **清理脚本示例**：

```bash
#!/bin/bash
# audit_log_cleanup.sh — 归档并清理 90 天前的审计日志
BACKUP_FILE="audit_log_$(date +%Y%m%d).csv"

# 导出 90 天前数据
docker exec rec-postgres psql -U recplatform -c \
  "COPY (SELECT * FROM audit_log WHERE created_at < NOW() - INTERVAL '90 days') TO STDOUT WITH CSV HEADER" \
  > /tmp/$BACKUP_FILE

# 上传至 MinIO
docker exec rec-minio mc alias set local http://localhost:9000 recplatform recplatform123
docker cp /tmp/$BACKUP_FILE rec-minio:/data/audit-archive/$BACKUP_FILE

# 删除已归档数据
docker exec rec-postgres psql -U recplatform -c \
  "DELETE FROM audit_log WHERE created_at < NOW() - INTERVAL '90 days';"

echo "Audit log cleanup completed: $BACKUP_FILE"
```

---

## 4. 备份与恢复

### 4.1 PostgreSQL 备份

**全量备份：**

```bash
docker exec rec-postgres pg_dump -U recplatform recplatform > backup_$(date +%Y%m%d_%H%M%S).sql
```

**压缩备份：**

```bash
docker exec rec-postgres pg_dump -U recplatform -F c recplatform > backup_$(date +%Y%m%d).dump
```

**定时备份脚本（crontab）：**

```bash
# 每天凌晨 2:00 全量备份，保留最近 7 天
0 2 * * * docker exec rec-postgres pg_dump -U recplatform recplatform | gzip > /backup/pg_backup_$(date +\%Y\%m\%d).sql.gz && find /backup -name "pg_backup_*.sql.gz" -mtime +7 -delete
```

**恢复：**

```bash
# SQL 文件恢复
cat backup.sql | docker exec -i rec-postgres psql -U recplatform recplatform

# 压缩格式恢复
docker exec -i rec-postgres pg_restore -U recplatform -d recplatform < backup.dump
```

### 4.2 Redis 备份

**RDB 快照：**

```bash
# 触发后台保存
docker exec rec-redis redis-cli -a redis123 BGSAVE

# 复制 RDB 文件到宿主机
docker cp rec-redis:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb
```

**AOF 持久化配置**（推荐生产环境开启）：

```bash
# 在 redis 启动命令中追加 --appendonly yes
# docker-compose.yml 修改：
command: redis-server --requirepass redis123 --appendonly yes
```

**数据恢复流程：**

1. 停止 Redis 容器：`docker stop rec-redis`
2. 替换数据文件：将备份的 `dump.rdb` 复制到 volume 目录
3. 启动容器：`docker start rec-redis`
4. 验证数据：`docker exec rec-redis redis-cli -a redis123 DBSIZE`

### 4.3 MinIO 备份

**mc mirror 同步：**

```bash
# 安装 mc 客户端并配置
mc alias set recplatform http://localhost:9000 recplatform recplatform123

# 全量镜像到本地备份目录
mc mirror recplatform/ /backup/minio/$(date +%Y%m%d)/

# 跨节点复制
mc mirror recplatform/ remote-backup/recplatform/
```

### 4.4 ClickHouse 备份

**表级备份：**

```bash
# 导出表数据为压缩格式
docker exec rec-clickhouse clickhouse-client \
  --query "SELECT * FROM recplatform.analytics_table FORMAT Native" | gzip > ch_backup_$(date +%Y%m%d).native.gz

# 或使用 TabSeparated 格式
docker exec rec-clickhouse clickhouse-client \
  --query "SELECT * FROM recplatform.analytics_table FORMAT TabSeparated" > ch_backup_$(date +%Y%m%d).tsv
```

**数据目录冷备份：**

```bash
# 停止 ClickHouse 后直接复制数据目录
docker stop rec-clickhouse
cp -r /var/lib/docker/volumes/recplatform_clickhouse_data/_data ./ch_data_backup/
docker start rec-clickhouse
```

### 4.5 配置文件备份

需备份的配置文件清单：

| 文件 | 路径 | 说明 |
|------|------|------|
| docker-compose.yml | 项目根目录 | 编排配置 |
| application.yml | recplatform-server/recplatform-web/src/main/resources/ | 主配置 |
| application-docker.yml | 同上 | Docker 环境配置 |
| nginx.conf | recplatform-ui/ | Nginx 配置 |

**建议使用 Git 管理配置变更**，确保每次修改可追溯。

### 4.6 备份策略建议

| 备份对象 | 频率 | 方式 | 保留周期 | 异地备份 |
|---------|------|------|---------|---------|
| PostgreSQL | 每日全量 | pg_dump + gzip | 30 天 | 每周同步至远程 |
| Redis | 每日 | RDB 快照 | 7 天 | 每周同步至远程 |
| MinIO | 每日 | mc mirror | 30 天 | 实时跨区域复制 |
| ClickHouse | 每周 | 数据目录冷备 | 4 周 | 每月同步至远程 |
| 配置文件 | 变更时 | Git 版本控制 | 永久 | Git 远程仓库 |

---

## 5. 扩缩容

### 5.1 后端服务扩展

**水平扩展（增加 backend 副本）：**

修改 `docker-compose.yml`：

```yaml
backend:
  # ... 现有配置 ...
  deploy:
    replicas: 2
```

**Nginx 负载均衡配置**（修改 `nginx.conf`）：

```nginx
upstream backend_pool {
    server backend:8080;
    # 扩展后添加更多后端
    # server backend-2:8080;
    keepalive 32;
}

server {
    # ... 其他配置 ...
    location /api/ {
        proxy_pass http://backend_pool/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

**会话一致性：** Sa-Token 已配置 Redis 存储（`token-style: uuid`），天然支持多副本部署，无需 sticky session。

### 5.2 Solver 引擎扩展

**增加 workers 数量：**

```yaml
solver-engine:
  # ... 现有配置 ...
  command: uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

**多副本部署：**

```yaml
solver-engine:
  deploy:
    replicas: 2
  # 去除固定容器名，使用动态命名
  # container_name: rec-solver-engine  # 注释掉
```

**按问题规模分流：** 可部署不同配置的 solver-engine 实例（大/中/小规格），通过 RabbitMQ 不同队列分发任务。

### 5.3 数据库扩展

**连接池调优**（application-docker.yml）：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**读写分离配置建议：**

- 使用 PostgreSQL 流复制（Streaming Replication）搭建从库
- 后端配置多数据源，写操作指向主库，读操作指向从库
- 可通过 MyBatis-Plus 的动态数据源插件实现

**PostgreSQL 主从搭建概述：**

1. 主库配置 `postgresql.conf`：`wal_level=replica`，`max_wal_senders=3`
2. 主库配置 `pg_hba.conf`：允许从库 replication 连接
3. 从库执行 `pg_basebackup` 从主库拉取基础备份
4. 从库配置 `standby.signal` 及 `primary_conninfo`

### 5.4 Redis 扩展

**演进路径：单机 → 哨兵 → 集群**

| 阶段 | 适用场景 | 说明 |
|------|---------|------|
| 单机 | 开发/小规模 | 当前配置，数据量 < 内存 60% |
| 哨兵 | 生产高可用 | 1 主 2 从 + 3 哨兵，自动故障转移 |
| 集群 | 大数据量 | 数据分片，支持 TB 级数据 |

**Redisson 哨兵配置：**

```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes: sentinel1:26379,sentinel2:26379,sentinel3:26379
      password: redis123
```

---

## 6. 安全运维

### 6.1 账号安全

**首次部署必须修改默认密码：**

| 服务 | 默认用户 | 默认密码 | 环境变量 |
|------|---------|---------|---------|
| PostgreSQL | recplatform | recplatform123 | `POSTGRES_PASSWORD` |
| Redis | - | redis123 | `command: redis-server --requirepass` |
| RabbitMQ | recplatform | recplatform123 | `RABBITMQ_DEFAULT_PASS` |
| MinIO | recplatform | recplatform123 | `MINIO_ROOT_PASSWORD` |

**密码复杂度建议：** 至少 12 位，包含大小写字母、数字、特殊字符。

**定期密码轮换：** 建议每 90 天轮换一次中间件密码，同步更新 `docker-compose.yml` 环境变量及 `application-docker.yml` 配置。

### 6.2 网络安全

**核心原则：仅 Nginx :80 对外暴露，其余服务绑定内网。**

**修改端口绑定（生产环境推荐）：**

```yaml
postgres:
  ports:
    - "127.0.0.1:5432:5432"  # 仅本机可访问
redis:
  ports:
    - "127.0.0.1:6379:6379"
# 其他中间件同理
```

**防火墙规则建议（iptables/ufw）：**

```bash
# 仅允许 80 端口对外
ufw default deny incoming
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 22/tcp     # SSH 管理
ufw enable
```

### 6.3 SSL/TLS 配置

**Nginx HTTPS 证书配置：**

```nginx
server {
    listen 443 ssl http2;
    server_name recplatform.example.com;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    # HTTP 重定向
    # 其他配置与原 nginx.conf 一致
}

server {
    listen 80;
    server_name recplatform.example.com;
    return 301 https://$host$request_uri;
}
```

**证书续期提醒：** 使用 Let's Encrypt + certbot 自动续期，或设置日历提醒在证书到期前 30 天续期。

### 6.4 数据安全

- **数据库密码**：生产环境建议使用 Docker Secrets 或 Vault 管理，避免明文写入 `docker-compose.yml`
- **审计日志脱敏**：用户密码、Token 等敏感字段在写入 `audit_log` 前进行掩码处理
- **API 传输加密**：生产环境强制 HTTPS，禁止 HTTP 明文传输

### 6.5 权限最小化

**数据库用户权限限制：**

```sql
-- 创建只读用户（报表查询用）
CREATE USER recplatform_readonly WITH PASSWORD 'secure_password';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO recplatform_readonly;

-- 限制应用用户权限（禁止 DDL）
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  REVOKE CREATE ON SCHEMA public FROM PUBLIC;
```

**容器非 root 运行建议：**

```dockerfile
# Dockerfile 中添加
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

---

## 7. 版本升级

### 7.1 升级流程

```
1. 通知用户 → 2. 备份数据 → 3. 拉取新镜像 → 4. 停止旧服务
      ↓                                            ↓
7. 确认/回滚 ← 6. 验证功能 ← 5. 执行迁移并启动新服务
```

**详细步骤：**

```bash
# 1. 备份数据库
docker exec rec-postgres pg_dump -U recplatform recplatform | gzip > pre_upgrade_$(date +%Y%m%d).sql.gz

# 2. 拉取新镜像
docker-compose pull

# 3. 停止应用服务（保留中间件）
docker-compose stop backend solver-engine frontend

# 4. 启动新版本
docker-compose up -d backend solver-engine frontend

# 5. 验证
curl http://localhost:8080/api/v1/platform/health
curl http://localhost:8000/health
```

### 7.2 数据库迁移

**建议引入 Flyway 管理 Schema 变更：**

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**手动迁移脚本执行：**

```bash
# 执行迁移 SQL
cat migration_V2.sql | docker exec -i rec-postgres psql -U recplatform recplatform
```

### 7.3 回滚方案

```bash
# 回滚到指定镜像版本
docker-compose down
# 修改 docker-compose.yml 中镜像 tag 为旧版本
docker-compose up -d

# 数据库回滚（需提前准备回滚脚本）
cat rollback_V2.sql | docker exec -i rec-postgres psql -U recplatform recplatform
```

**关键原则：** 保留旧镜像 tag，数据库迁移必须配套回滚脚本。

### 7.4 灰度升级

**蓝绿部署方案：**

1. 部署新版本到"绿色"环境（不同端口如 8081）
2. 通过 Nginx 切换 upstream 指向绿色环境
3. 观察指标正常后，下线蓝色环境

**金丝雀发布建议：**

1. 新版本仅对 10% 流量开放
2. Nginx 配置按 IP 或 Header 分流
3. 监控错误率与性能指标
4. 逐步扩大流量比例至 100%

---

## 8. 故障处理

### 8.1 服务无法启动

| 现象 | 检查方式 | 解决方案 |
|------|---------|---------|
| backend 启动后立即退出 | `docker logs rec-backend` 查看错误日志 | 检查数据库连接、Redis 连接是否正常 |
| 数据库连接拒绝 | `docker exec rec-postgres pg_isready -U recplatform` | 确认 postgres 容器已 healthy，检查 `depends_on` 条件 |
| Redis 连接失败 | `docker exec rec-redis redis-cli -a redis123 ping` | 检查密码是否正确，端口是否可达 |
| Nginx 502 Bad Gateway | 检查 backend 容器是否运行 | 重启 backend：`docker-compose restart backend` |
| 端口冲突 | `lsof -i :<端口>` | 停止占用端口的进程或修改映射端口 |

### 8.2 接口响应超时

**排查流程：**

```
网络层 → 应用层 → 数据库层 → 外部依赖
```

```bash
# 1. 网络层：检查 Nginx 到后端连通性
docker exec rec-frontend curl -s -o /dev/null -w "%{time_total}" http://backend:8080/api/v1/platform/health

# 2. 应用层：查看后端线程池与慢请求
docker logs rec-backend --tail 500 | grep -i "timeout\|slow\|blocked"

# 3. 数据库层：检查慢查询
docker exec rec-postgres psql -U recplatform -c \
  "SELECT query, calls, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# 4. Solver 引擎：检查求解超时
docker logs rec-solver-engine --tail 200 | grep -i "timeout\|error"
```

**Nginx 当前超时配置：** `proxy_connect_timeout 60s`，`proxy_read_timeout 300s`（适配求解任务长耗时场景）。

### 8.3 数据库连接池耗尽

**现象：** 日志中出现 `Connection pool exhausted` 或 `HikariPool-1 - Connection is not available`。

**排查：**

```bash
# 查看当前活跃连接数
docker exec rec-postgres psql -U recplatform -c \
  "SELECT state, count(*) FROM pg_stat_activity GROUP BY state;"

# 查看长时间运行的事务
docker exec rec-postgres psql -U recplatform -c \
  "SELECT pid, now() - xact_start AS duration, query FROM pg_stat_activity WHERE state IN ('idle in transaction', 'active') ORDER BY duration DESC;"
```

**解决：**

1. 调整 HikariCP 连接池大小：`spring.datasource.hikari.maximum-pool-size=20`
2. 排查未关闭事务的代码，确保 `@Transactional` 方法正常退出
3. 终止空闲事务：`SELECT pg_terminate_backend(<pid>);`

### 8.4 Redis 内存溢出

**现象：** Redis 返回 OOM 错误，或 Sa-Token 认证异常。

**排查：**

```bash
# 查看内存使用详情
docker exec rec-redis redis-cli -a redis123 info memory

# 查看大 Key
docker exec rec-redis redis-cli -a redis123 --bigkeys

# 查看 Key 数量与过期情况
docker exec rec-redis redis-cli -a redis123 DBSIZE
```

**解决：**

```bash
# 设置最大内存限制（推荐物理内存的 60%）
# 在 docker-compose.yml command 中追加：
command: redis-server --requirepass redis123 --maxmemory 512mb --maxmemory-policy allkeys-lru

# 手动清理过期 Key
docker exec rec-redis redis-cli -a redis123 SCAN 0 COUNT 1000
```

### 8.5 Solver 引擎无响应

**现象：** 求解任务超时，前端显示执行失败。

**排查：**

```bash
# 检查容器状态与资源占用
docker stats rec-solver-engine --no-stream

# 检查进程状态
docker exec rec-solver-engine ps aux

# 检查日志中的异常
docker logs rec-solver-engine --tail 500 | grep -iE "error|exception|timeout|oom|killed"

# 检查求解超时配置（当前默认 300 秒）
grep -r "default-timeout\|max-concurrent" recplatform-server/
```

**解决：**

1. 重启 worker 进程：`docker-compose restart solver-engine`
2. 增加 workers 数量（见 5.2 节）
3. 调整求解超时：修改 `application.yml` 中 `recplatform.solver.default-timeout`
4. 设置容器资源限制：`deploy.resources.limits.memory: 2G`

### 8.6 磁盘空间不足

**排查：**

```bash
# 宿主机磁盘使用
df -h

# Docker 各组件磁盘占用
docker system df

# 各 Volume 大小
docker volume ls -q | xargs -I {} docker volume inspect {} --format '{{ .Name }}: {{ .Mountpoint }}'
```

**清理：**

```bash
# 清理无用镜像、容器、网络
docker system prune -a --volumes

# 清理 Docker 日志
truncate -s 0 /var/lib/docker/containers/*/*-json.log

# 清理临时文件
docker exec rec-minio mc alias set local http://localhost:9000 recplatform recplatform123
docker exec rec-minio mc rm --recursive --force local/temp/
```

**预防：** 设置监控告警阈值 80%，日志轮转（见 3.2 节）。

### 8.7 RabbitMQ 队列堆积

**现象：** 异步任务消费延迟，管道执行结果迟迟不返回。

**排查：**

```bash
# 访问管理面板
# 浏览器打开 http://localhost:15672 （账号：recplatform / recplatform123）

# 命令行查看队列状态
curl -s -u recplatform:recplatform123 http://localhost:15672/api/queues | python3 -m json.tool

# 查看消费者连接数
curl -s -u recplatform:recplatform123 http://localhost:15672/api/consumers | python3 -m json.tool
```

**解决：**

1. 增加 solver-engine 消费者副本（见 5.2 节）
2. 清理死信队列中的无效消息
3. 检查消费者是否有异常导致 ack 失败

```bash
# 清空指定队列（慎用！仅限无效消息）
docker exec rec-rabbitmq rabbitmqctl purge_queue <queue_name>
```

---

> **文档版本：** v1.0 | **最后更新：** 2026-05-29 | **维护团队：** RecPlatform 运维组
