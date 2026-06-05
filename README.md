# RecPlatform

**[English](README.md)** | **[中文文档](README_CN.md)**

RecPlatform is an open-source algorithm optimization and workflow orchestration platform. It provides a complete solution for defining optimization problems, configuring solver algorithms, building data pipelines, and automating end-to-end workflows through a visual DAG editor.

## Features

- **Solver Engine** - Define optimization problems (LP/MILP/QP) with variables, constraints, and objective functions. Supports 10+ solver backends including HiGHS, OR-Tools, PuLP, SciPy, Genetic Algorithm, Simulated Annealing, and more.
- **Workflow Orchestration** - Visual DAG-based flow editor with 10+ node types: Service Call, Solver Invoke, Data Transform, Script, Decision Gate, Loop, Parallel Fork/Join, and Sub-Flow.
- **Data Management** - Connect to multiple data sources (PostgreSQL, MySQL, ClickHouse, REST API, CSV), build ETL pipelines, publish data APIs, and run ad-hoc SQL queries.
- **System Administration** - User management, role-based access control (RBAC), workspace isolation, and comprehensive audit logging.
- **System Monitoring** - Real-time service health checks, JVM metrics, CPU/memory/disk usage monitoring.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| Backend | Spring Boot 3.2 + MyBatis-Plus + Sa-Token |
| Solver Engine | Python 3 + FastAPI + HiGHS/OR-Tools/PuLP/SciPy |
| Database | PostgreSQL 15 + Redis 7 |
| Message Queue | RabbitMQ 3 |
| Object Storage | MinIO |
| Deployment | Docker Compose + Nginx |

## Project Structure

```
recplatform/
├── recplatform-server/          # Java backend (Spring Boot)
│   ├── recplatform-common/      # Common utilities, enums, exceptions
│   ├── recplatform-iam/         # Identity & access management
│   ├── recplatform-audit/       # Audit logging & alerting
│   ├── recplatform-orchestrator/# Workflow orchestration engine
│   ├── recplatform-solver/      # Solver service integration
│   ├── recplatform-data/        # Data source, pipeline, API management
│   └── recplatform-web/         # Application entry, configs, system info
├── recplatform-ui/              # Vue 3 frontend application
├── solver-engine/               # Python solver engine (FastAPI)
├── docker-compose.yml           # Docker Compose configuration
└── docs/                        # Documentation
```

## Quick Start

### Prerequisites

- Docker & Docker Compose v2+
- (For local development) JDK 17+, Node.js 18+, Python 3.10+

### Deploy with Docker Compose

```bash
# Clone the repository
git clone https://github.com/your-org/recplatform.git
cd recplatform

# Start all services
docker compose up -d

# Check service status
docker compose ps
```

After all services are running, open your browser:

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **Solver Engine**: http://localhost:8000
- **RabbitMQ Console**: http://localhost:15672 (recplatform / recplatform123)
- **MinIO Console**: http://localhost:9001 (recplatform / recplatform123)

Default login credentials: `admin` / `admin123`

### Local Development

**Backend:**

```bash
cd recplatform-server
mvn spring-boot:run -pl recplatform-web -Dspring-boot.run.profiles=dev
```

**Frontend:**

```bash
cd recplatform-ui
npm install
npm run dev
```

**Solver Engine:**

```bash
cd solver-engine
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
```

## Architecture

```
                    +-----------+
                    |  Nginx    |
                    |  (Port 80)|
                    +-----+-----+
                          |
              +-----------+-----------+
              |                       |
       +------+------+       +-------+-------+
       |   Frontend   |       |    Backend    |
       |   (Vue 3)    |       | (Spring Boot) |
       +--------------+       +---+---+---+---+
                                  |   |   |
                    +-------------+   |   +-------------+
                    |                 |                  |
             +------+------+  +------+------+   +------+------+
             |    Solver    |  |  PostgreSQL |   |    Redis    |
             |   Engine     |  |             |   |             |
             |  (FastAPI)   |  +-------------+   +-------------+
             +--------------+
```

## Configuration

### Environment Variables (Docker)

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://postgres:5432/recplatform` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `recplatform` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `recplatform123` |
| `REDIS_HOST` | Redis host | `redis` |
| `REDIS_PASSWORD` | Redis password | `redis123` |
| `SOLVER_ENGINE_URL` | Solver engine URL | `http://solver-engine:8000` |

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
