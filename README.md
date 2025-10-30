# Real-Time Order Processing and Analytics System

A comprehensive enterprise-grade system for order management with real-time analytics, built using modern microservices architecture.

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Order Service │    │  Analytics      │
│   (React)       │◄──►│   (Spring Boot) │◄──►│  (Flink/Spark)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   PostgreSQL    │    │     Kafka       │
                       │   (Orders DB)   │    │  (Event Stream) │
                       └─────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────┐
                                              │   Cassandra     │
                                              │ (Analytics DB)  │
                                              └─────────────────┘
```

## Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.x** - Core order service
- **Spring Data JPA** + **Hibernate** - ORM layer
- **PostgreSQL** - Primary database
- **Python** - Analytics microservice
- **Shell Scripts** - Automation and deployment

### Messaging & Analytics
- **Apache Kafka** - Event streaming
- **Apache Flink** - Real-time stream processing
- **Cassandra** - Analytics data storage

### Frontend
- **React** - Admin dashboard
- **Chart.js** - Analytics visualization

### Testing
- **JUnit 5** + **Mockito** - Java testing
- **TestContainers** - Integration testing
- **PyTest** - Python testing

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Python 3.10+
- Maven 3.9+

### One-Command Bootstrap
```bash
git clone https://github.com/dbasak763/opaplatform.git
cd opaplatform
./scripts/start-all.sh
```

This script will:
- Bring up PostgreSQL, Redis, Kafka, Cassandra and supporting services
- Build and launch the Spring Boot order service
- Create a Python virtual environment and start the analytics FastAPI service
- Run the React dashboard in development mode

Visit the admin dashboard at [http://localhost:3000](http://localhost:3000) once the script completes.

### Manual Setup

If you prefer to run each component independently, follow the sequence below.

#### 1. Start Infrastructure
```bash
./scripts/start-infrastructure.sh
```
This provisions:
- PostgreSQL (`localhost:5432`, credentials `orderuser` / `orderpass`)
- Redis (`localhost:6379`)
- Kafka broker (`localhost:9092`)
- Cassandra (`localhost:9042`)
- pgAdmin (`http://localhost:8081`, login `admin@orderapp.com` / `admin123`)

#### 2. Start Order Service
```bash
./scripts/start-order-service.sh
```
The REST API will be available at [http://localhost:8090/api](http://localhost:8090/api).

#### 3. Start Analytics Service
```bash
./scripts/start-analytics-service.sh
```
This script will install Python dependencies into `.venv/` (if missing) and launch the FastAPI server at [http://localhost:8091](http://localhost:8091).

#### 4. Start Frontend Dashboard
```bash
./scripts/start-frontend.sh
```
The React development server runs on [http://localhost:3000](http://localhost:3000).

### Useful Scripts
- `./scripts/test-api.sh` – Smoke tests against the order-service REST endpoints
- `./scripts/test-kafka-integration.sh` – End-to-end Kafka + analytics validation
- `./scripts/stop-all.sh` – Gracefully shut down every component

### Credentials & Default Users
- Basic Auth for order service: `admin` / `admin123`
- Postgres: `orderuser` / `orderpass`
- pgAdmin: `admin@orderapp.com` / `admin123`

Sample data loaded at startup includes users, products, and orders to simplify manual testing.

## Project Structure

```
order-processing-system/
├── order-service/          # Spring Boot order management
├── analytics-service/      # Python analytics microservice
├── stream-processor/       # Flink streaming jobs
├── frontend/              # React dashboard
├── scripts/               # Shell automation scripts
├── docker/                # Docker configurations
└── docs/                  # Documentation
```

## Features

### Order Management
- Create, read, update orders
- User management
- Order status tracking
- Event-driven architecture

### Real-Time Analytics
- Live order metrics
- Product performance tracking
- Revenue analytics
- Customer insights

### Admin Dashboard
- Order history and search
- Real-time analytics charts
- System monitoring
- User management

## Testing & QA

### Automated Test Suites
```bash
# Run the full test matrix
./scripts/run-tests.sh

# Backend unit & integration tests
mvn clean verify -f order-service/pom.xml

# Frontend unit tests
cd frontend && npm test

# Analytics service tests
cd analytics-service && source .venv/bin/activate && pytest
```

### End-to-End Workflows

* **API regression:** `./scripts/test-api.sh`
* **Kafka pipeline:** `./scripts/test-kafka-integration.sh`
* **Dashboard manual QA:**
  1. Create a new order from the UI or via `POST /api/orders`
  2. Confirm order status transitions in the React dashboard
  3. Validate real-time metrics (orders/minute, revenue trends) update without page refresh

### Monitoring & Troubleshooting
- Order service health: `GET http://localhost:8090/actuator/health`
- Analytics service health: `GET http://localhost:8091/health`
- Check Docker containers: `docker ps`
- View Kafka topics: `docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092`

## 📊 Monitoring

- Application metrics via Micrometer
- Kafka monitoring via Kafka Manager
- Database monitoring via pgAdmin
- Custom dashboards in React frontend

## Development

See individual service README files for detailed development instructions:
- [Order Service](order-service/README.md)
- [Analytics Service](analytics-service/README.md)
- [Stream Processor](stream-processor/README.md)
- [Frontend](frontend/README.md)
