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

Host ports use a non-default range (`1xxxx`/`13xxx`) to avoid collisions with common local development servers. Internal service-to-service communication is unaffected.

Only the endpoints you access from your browser are published to the host. The microservices themselves are reached through the API Gateway, so they do not expose ports.

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend (production build) | http://localhost:13003 | - |
| Frontend (dev server, HMR) | http://localhost:13004 | - |
| API Gateway | http://localhost:18080 | OAuth2 |
| API Docs (Swagger) | http://localhost:18080/swagger-ui.html | OAuth2 |
| Keycloak Admin | http://localhost:18180 | `admin` / from `.env` |
| Grafana | http://localhost:13007 | from `.env` |
| Prometheus | http://localhost:19090 | - |
| Kibana | http://localhost:15601 | - |

#### Optional direct database access

SQL Server, MongoDB and Pulsar are **not exposed** on the host by default (they
stay on the Docker network). To reach them directly from your machine
(e.g. SSMS, MongoDB Compass), set the corresponding port in your `.env`:

```bash
SQLSERVER_HOST_PORT=11433
MONGODB_HOST_PORT=17017
PULSAR_BROKER_HOST_PORT=16650
PULSAR_ADMIN_HOST_PORT=18090
```

### Demo User (Keycloak)

After the stack is up, create the demo user so the platform works out of the box:

```bash
docker-compose exec keycloak bash /opt/keycloak/data/import/setup-demo-user.sh
```

The demo user (`demouser`) password comes from `KEYCLOAK_DEMO_PASSWORD` in your `.env`. Roles `CUSTOMER` and `OPERATOR` are assigned automatically.

### First login & self-registration

- Open **http://localhost:13003** (or **:13004** for the dev server).
- The login screen shows the **demo credentials** (baked from your `.env` into the local build) — just type them into the Keycloak form after clicking *Sign in with Keycloak*.
- Visitors can also click **Create account** to self-register. New accounts are created in Keycloak with the `CUSTOMER` role (orders and inventory views). Saga/Payment pages require the `OPERATOR` role, which only the demo account has — a nice live demonstration of role-based access control.

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

### Frontend development (hot reload)

For frontend work, use the **dev server** instead of rebuilding the static image:

```bash
docker-compose up frontend-dev    # http://localhost:13004
```

It mounts `./frontend` as a volume and runs the CRA dev server with HMR — save a
source file and the browser updates instantly (no image rebuild, no manual
refresh). API calls are proxied to the gateway, and Keycloak already accepts the
`13004` redirect URI, so login works exactly like production.

Add a dark/light theme toggle in the top bar (persisted in `localStorage`).

Stop it with:

```bash
docker-compose stop frontend-dev
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
