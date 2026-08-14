# Enterprise Order Processing Platform

A distributed order processing platform built with **Java 21**, **Spring Boot 3.x**, microservices, and event-driven architecture. Demonstrates enterprise-grade patterns including **Saga orchestration**, **compensating transactions**, and **legacy system integration** through an anti-corruption layer.

## Overview

An e-commerce platform that processes orders across multiple services: order management, payment authorization, inventory reservation, and notifications. The system handles partial failures gracefully using distributed transactions (Saga pattern) instead of a single ACID transaction.

**Happy path:**

```
Customer Places Order
  -> Order Service: Create order (PENDING)
  -> Payment Service: Authorize payment
  -> Inventory Service: Reserve stock
  -> Order Service: Confirm order (CONFIRMED)
  -> Notification Service: Send confirmation
```

**Failure scenario (inventory out of stock):**

```
Order Created
  -> Payment Authorized
  -> Inventory Reservation FAILED
  -> Payment Refund Requested
  -> Order Cancelled
  -> Notification: Order cancelled
```

## Architecture

| Layer | Technology |
|-------|------------|
| Frontend | React 18 + TypeScript |
| API Gateway | Spring Cloud Gateway (OAuth2 / JWT validation) |
| Microservices | Spring Boot 3.x (Order, Payment, Inventory, Notification, Legacy, Saga) |
| Databases | SQL Server (Order/Payment/Saga), MongoDB (Inventory) |
| Messaging | Apache Pulsar (event broker, DLQ support) |
| Identity | Keycloak (OAuth2 / OpenID Connect) |
| Observability | Prometheus, Grafana, ELK (Elasticsearch, Logstash, Kibana) |
| Deployment | Docker Compose, Kubernetes (Kustomize), GitHub Actions, Azure |

## Services

| Service | Internal Port | Database | Responsibility |
|---------|---------------|----------|----------------|
| Order Service | 8081 | SQL Server | Order lifecycle, state machine |
| Payment Service | 8082 | SQL Server | Payment authorization, refund |
| Inventory Service | 8083 | MongoDB | Product catalog, stock reservation |
| Notification Service | 8084 | - | Event consumer, notifications |
| Legacy Integration | 8085 | - | TIBCO EMS / Mainframe adapters |
| Saga Orchestrator | 8086 | SQL Server | Coordinates distributed transactions |
| API Gateway | 8080 | - | Routing, authentication |

Each service owns its data. There are no shared databases between services.

## Running with Docker

### Prerequisites

- Docker 24+ (Docker Desktop recommended)
- Docker Compose v2 (`docker compose`) or v1 (`docker-compose`)
- At least **8 GB RAM** available to Docker (16 GB recommended for the full stack)

### Setup

```bash
# 1. Create your environment file
cp .env.example .env

# 2. Fill in the values in .env (SQL Server, MongoDB, Keycloak, Grafana passwords)

# 3. Start the core stack
docker-compose up -d

# 4. Full stack (adds Elasticsearch, Kibana, Logstash)
docker-compose --profile full up -d
```

> Note: `docker compose` (v2) and `docker-compose` (v1) are both supported.

### Access Points

Host ports use a non-default `1xxxx` range to avoid collisions with common local development servers. Internal service-to-service communication is unaffected.

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3003 | - |
| API Gateway | http://localhost:18080 | OAuth2 |
| API Docs (Swagger) | http://localhost:18080/swagger-ui.html | OAuth2 |
| Keycloak Admin | http://localhost:18180 | `admin` / from `.env` |
| Grafana | http://localhost:3007 | from `.env` |
| Prometheus | http://localhost:19090 | - |
| Kibana | http://localhost:15601 | - |
| SQL Server | localhost:11433 | `sa` / from `.env` |
| MongoDB | localhost:17017 | from `.env` |

### Demo User (Keycloak)

After the stack is up, create the demo user so the platform works out of the box:

```bash
docker-compose exec keycloak bash /opt/keycloak/data/import/setup-demo-user.sh
```

The demo user (`demouser`) password comes from `KEYCLOAK_DEMO_PASSWORD` in your `.env`. Roles `CUSTOMER` and `OPERATOR` are assigned automatically.

### Managing the stack

```bash
# View logs
docker-compose logs -f

# Stop (keep data)
docker-compose down

# Stop and remove volumes (fresh start)
docker-compose down -v

# Rebuild after code changes
docker-compose build
docker-compose up -d
```

### Hardware Recommendations

| Profile | Services | Minimum | Recommended |
|---------|----------|---------|-------------|
| Core (default) | Infra + 7 services + frontend + Prometheus + Grafana | 8 GB RAM, 4 cores | 12 GB RAM, 6 cores |
| Full (`--profile full`) | Core + Elasticsearch + Kibana + Logstash | 16 GB RAM, 8 cores | 24 GB RAM, 8+ cores |

Disk usage: approximately **8 GB** for core, **15 GB** for the full stack (images + volumes).

## Local Development (without Docker)

```bash
# Prerequisites: Java 21, Maven 3.9+, Node.js 18+

# Build all services
mvn clean install

# Start infrastructure (SQL Server, MongoDB, Pulsar, Keycloak)
docker-compose up -d sqlserver mongodb pulsar keycloak

# Start each service in a separate terminal
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl payment-service
mvn spring-boot:run -pl inventory-service
mvn spring-boot:run -pl notification-service
mvn spring-boot:run -pl legacy-integration-service
mvn spring-boot:run -pl saga-orchestrator
mvn spring-boot:run -pl api-gateway
```

## Testing

```bash
# Unit tests (business rules, state machines)
mvn test

# Full build with tests
mvn clean verify
```

## Kubernetes

Manifests in `k8s/` use Kustomize with environment overlays:

```bash
# Development environment
kubectl apply -k k8s/overlays/dev

# Production environment
kubectl apply -k k8s/overlays/prod
```

Each overlay points to the images published to GitHub Container Registry
(`ghcr.io/kazuyabr/demonstracao-habilidade-squadra/<service>`). If you fork the
repository, update the image references to match your own registry path.

## CI/CD

GitHub Actions in `.github/workflows/`:

- `ci-cd.yml`: test -> build -> Docker images -> deploy (develop and main)
- `pr-validation.yml`: compile + test on pull requests

Pipeline: `push -> Test -> Build -> Docker Build -> Push to Registry -> Deploy`

### Environment flow

| Branch | Environment | Deploy |
|--------|-------------|--------|
| `develop` | Dev | `k8s/overlays/dev` (image tag `develop`) |
| `main` | Production | `k8s/overlays/prod` (image tag `main`) |

`main` is the protected default branch. Changes reach production through a
pull request from `develop`.

### Enabling real deployments

By default the deploy jobs are no-ops (the pipeline stays green without
credentials). To enable real Kubernetes deployments, add repository secrets:

- `KUBECONFIG_DEV`: kubeconfig for the dev cluster
- `KUBECONFIG_PROD`: kubeconfig for the production cluster

The deploy job detects the secret and runs `kubectl apply -k k8s/overlays/<env>`
when present; otherwise it skips gracefully.

## Azure Deployment

Infrastructure as Code for a production deployment on Azure:

- **Terraform** (`azure/terraform/`): AKS, Azure SQL, Cosmos DB, Service Bus, Key Vault, ACR, Application Insights
- **Bicep** (`azure/bicep/`): alternative IaC template

## Architecture Decisions

- Multi-module Maven project with a shared `events` module
- SQL Server for transactional (ACID) data
- MongoDB for the flexible product catalog schema
- **Saga Orchestrator** (not choreography) for easier debugging and centralized coordination
- Apache Pulsar as the primary event broker (multi-tenancy, DLQ)
- Anti-corruption layer for legacy integration (TIBCO EMS / Mainframe simulation)
- Keycloak for OAuth2 / OpenID Connect with role-based access

## License

This project is for educational and portfolio purposes.
