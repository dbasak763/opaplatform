# Real-Time Order Processing and Analytics System

A comprehensive enterprise-grade system for order management with real-time analytics, built using modern microservices architecture.

## 🏗️ Architecture Overview

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

## 🛠️ Tech Stack

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

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 16+
- Docker & Docker Compose
- Python 3.9+
- Maven 3.8+

### Setup
1. Clone the repository
2. Start infrastructure: `docker-compose up -d`
3. Build and run services: `./scripts/start-all.sh`
4. Access dashboard: http://localhost:3000

## 📁 Project Structure

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

## 🎯 Features

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

## 🧪 Testing

Run all tests:
```bash
./scripts/run-tests.sh
```

Individual test suites:
```bash
# Java tests
mvn test -f order-service/pom.xml

# Python tests
pytest analytics-service/

# Integration tests
mvn verify -f order-service/pom.xml
```

## 📊 Monitoring

- Application metrics via Micrometer
- Kafka monitoring via Kafka Manager
- Database monitoring via pgAdmin
- Custom dashboards in React frontend

## 🔧 Development

See individual service README files for detailed development instructions:
- [Order Service](order-service/README.md)
- [Analytics Service](analytics-service/README.md)
- [Stream Processor](stream-processor/README.md)
- [Frontend](frontend/README.md)
