# Enterprise Order Processing Platform

A distributed order processing platform built with Java 21, Spring Boot 3.x, microservices, and event-driven architecture. Designed to demonstrate enterprise-level patterns including Saga orchestration, compensating transactions, and legacy system integration.

## Business Problem

An e-commerce platform needs to process orders across multiple services: order management, payment processing, inventory control, and notifications. The system must handle partial failures gracefully using distributed transactions (Saga pattern) rather than a single ACID transaction.

**Order Flow:**
```
Customer Places Order
    → Order Service: Create order (PENDING)
    → Payment Service: Authorize payment
    → Inventory Service: Reserve stock
    → Order Service: Confirm order (CONFIRMED)
    → Notification Service: Send confirmation email
```

**Failure Scenario (Inventory out of stock):**
```
Order Created
    → Payment Authorized
    → Inventory Reservation FAILED
    → Payment Refund Requested
    → Payment Refunded
    → Order Cancelled
    → Notification: Order cancelled
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│                   (Order Dashboard)                          │
└───────────────────────┬─────────────────────────────────────┘
                        │ REST
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway                               │
│              (Spring Cloud Gateway)                          │
│              (OAuth2 / JWT Validation)                       │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Order      │ │   Payment    │ │  Inventory   │
│   Service    │ │   Service    │ │  Service     │
│              │ │              │ │              │
│ [SQL Server] │ │ [SQL Server] │ │  [MongoDB]   │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       └────────────────┼────────────────┘
                        │ Events
                        ▼
              ┌──────────────────┐
              │  Apache Pulsar   │
              │  (Event Broker)  │
              └────────┬─────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│    Saga      │ │ Notification │ │   Legacy     │
│  Orchestrator│ │   Service    │ │ Integration  │
│              │ │              │ │  (TIBCO)     │
│ [SQL Server] │ │              │ │              │
└──────────────┘ └──────────────┘ └──────────────┘
```

## Services

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| Order Service | 8081 | SQL Server | Order lifecycle, state machine |
| Payment Service | 8082 | SQL Server | Payment authorization, refund |
| Inventory Service | 8083 | MongoDB | Product catalog, stock reservation |
| Notification Service | 8084 | - | Event consumer, notifications |
| Legacy Integration | 8085 | - | TIBCO EMS adapter |
| Saga Orchestrator | 8086 | SQL Server | Coordinates distributed transactions |
| API Gateway | 8080 | - | Routing, authentication |

## Data Architecture

**SQL Server** (transactional, ACID):
- Order Service: Orders, order items, order history
- Payment Service: Payments, transactions, refunds
- Saga Orchestrator: Saga state, step history

**MongoDB** (flexible schema):
- Inventory Service: Product catalog with varying attributes per product type

Each service owns its data. No shared databases between services.

## Messaging Architecture

**Apache Pulsar** is the primary event broker:

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| order-events | Order Service | Saga Orchestrator | Order lifecycle events |
| payment-events | Payment Service | Saga Orchestrator | Payment status events |
| inventory-events | Inventory Service | Saga Orchestrator | Stock events |
| notification-events | Multiple | Notification Service | Send notifications |
| legacy-events | Saga Orchestrator | Legacy Integration | Legacy system sync |

## Saga Flow

The Saga Orchestrator coordinates the distributed transaction:

**Happy Path:**
1. OrderCreated → StartSaga
2. AuthorizePayment → PaymentAuthorized
3. ReserveInventory → InventoryReserved
4. ConfirmOrder → OrderConfirmed
5. NotifyCustomer → DONE

**Failure Path (Inventory fails):**
1. OrderCreated → StartSaga
2. AuthorizePayment → PaymentAuthorized
3. ReserveInventory → InventoryReservationFailed
4. CompensatePayment → PaymentRefunded
5. CancelOrder → OrderCancelled
6. NotifyCustomer → DONE

## OAuth2

- **Identity Provider:** Keycloak
- **Flow:** Authorization Code + PKCE (frontend), Client Credentials (service-to-service)
- **Roles:** CUSTOMER, OPERATOR, ADMIN
- **Token:** JWT validated at API Gateway level

## Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose
- Node.js 18+ (for frontend)

### Quick Start

```bash
# Build all services
mvn clean install

# Start infrastructure
docker-compose -f docker/docker-compose.yml up -d

# Start services (each in a separate terminal)
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl payment-service
mvn spring-boot:run -pl inventory-service
# ... etc
```

### Using Docker Compose

```bash
# Start everything
docker-compose -f docker/docker-compose.yml up -d

# View logs
docker-compose -f docker/docker-compose.yml logs -f

# Stop
docker-compose -f docker/docker-compose.yml down
```

## Kubernetes

Manifests are in `k8s/` with overlays for different environments:

```bash
# Deploy to local k8s (minikube/kind)
kubectl apply -k k8s/overlays/dev

# Deploy to staging
kubectl apply -k k8s/overlays/staging
```

## CI/CD

GitHub Actions pipeline in `.github/workflows/`:

```
git push → Build → Test → Quality → Docker Build → Push to Registry → Deploy
```

- **CI:** `.github/workflows/ci.yml`
- **CD:** `.github/workflows/cd.yml`

## Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify -Pintegration

# Full build with tests
mvn clean verify
```

## Architecture Decisions

See `docs/` folder for detailed Architecture Decision Records (ADRs).

Key decisions documented:
- ADR-001: Multi-module Maven
- ADR-002: SQL Server for transactional data
- ADR-003: MongoDB for product catalog
- ADR-004: Saga Orchestrator pattern
- ADR-005: Apache Pulsar as event broker
- ADR-006: Anti-corruption layer for legacy
- ADR-007: Keycloak for OAuth2

## Trade-offs

| Decision | Chose | Alternative | Trade-off |
|----------|-------|-------------|-----------|
| Saga Pattern | Orchestrator | Choreography | Easier to debug vs. tighter coupling |
| SQL Server | Yes | PostgreSQL | Enterprise compatibility vs. cost |
| MongoDB | Inventory only | All services | Flexibility vs. consistency |
| Pulsar | Primary broker | Kafka | Multi-tenancy vs. ecosystem size |
| Maven | Multi-module | Gradle | Enterprise standard vs. modern syntax |

## License

This project is for educational and portfolio purposes.
