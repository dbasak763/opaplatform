# Order Processing System - Setup Guide

## Prerequisites

1. **Java 17+** - Required for Spring Boot
2. **Docker & Docker Compose** - For running PostgreSQL and Redis
3. **Git** - For version control (optional)

## Quick Start

### 1. Start Infrastructure Services

```bash
# Make sure Docker is running first
docker --version

# Start PostgreSQL, Redis, and pgAdmin
./scripts/start-infrastructure.sh
```

This will start:
- PostgreSQL on port 5432
- Redis on port 6379  
- pgAdmin on port 8081

### 2. Start the Order Service

```bash
# Start the Spring Boot application
./scripts/start-order-service.sh
```

The service will be available at: `http://localhost:8090/api`

### 3. Test the API

```bash
# Run API tests
./scripts/test-api.sh
```

## Manual Setup

### 1. Start Infrastructure

```bash
cd /Users/diwakar/CascadeProjects/order-processing-system
docker-compose -f docker-compose-basic.yml up -d
```

### 2. Build and Run Order Service

```bash
cd order-service
./mvnw clean spring-boot:run
```

## API Endpoints

### Authentication
- **Type**: Basic Auth
- **Username**: `admin`
- **Password**: `admin123`

### Core Endpoints

#### Orders
- `GET /api/orders` - Get all orders (paginated)
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/number/{orderNumber}` - Get order by order number
- `POST /api/orders` - Create new order
- `PUT /api/orders/{id}/status?status=SHIPPED` - Update order status
- `PUT /api/orders/{id}/cancel` - Cancel order

#### Products
- `GET /api/products` - Get all products (paginated, filterable)
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/sku/{sku}` - Get product by SKU
- `GET /api/products/available` - Get available products
- `GET /api/products/category/{category}` - Get products by category

#### Users
- `GET /api/users` - Get all users (paginated, filterable)
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email

#### Analytics
- `GET /api/orders/analytics/revenue` - Get total revenue
- `GET /api/orders/analytics/revenue/period?startDate=2024-01-01&endDate=2024-12-31` - Get revenue for period
- `GET /api/orders/analytics/count/{status}` - Get order count by status

### Sample API Calls

#### Create Order
```bash
curl -u admin:admin123 -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "items": [
      {
        "productId": "660e8400-e29b-41d4-a716-446655440001",
        "quantity": 2
      }
    ],
    "taxAmount": 8.00,
    "shippingAmount": 9.99,
    "notes": "Test order"
  }' \
  http://localhost:8090/api/orders
```

#### Get Orders
```bash
curl -u admin:admin123 \
  "http://localhost:8090/api/orders?page=0&size=5&sortBy=createdAt&sortDir=desc"
```

## Database Access

### pgAdmin Web Interface
- URL: http://localhost:8081
- Email: `admin@orderapp.com`
- Password: `admin123`

### Direct PostgreSQL Connection
- Host: `localhost`
- Port: `5432`
- Database: `orderdb`
- Username: `orderuser`
- Password: `orderpass`

## Sample Data

The system comes pre-loaded with:
- 3 sample users
- 5 sample products
- 2 sample orders

## Monitoring & Health

- Health Check: `GET http://localhost:8090/actuator/health`
- Application Info: `GET http://localhost:8090/actuator/info`

## Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Check what's using the port
   lsof -i :8090
   # Kill the process or change the port in application.yml
   ```

2. **Database Connection Issues**
   ```bash
   # Check if PostgreSQL container is running
   docker ps | grep postgres
   # Check logs
   docker logs order-postgres
   ```

3. **Maven Build Issues**
   ```bash
   # Clean and rebuild
   cd order-service
   ./mvnw clean compile
   ```

### Logs

Application logs are available at:
- Console output when running with `./mvnw spring-boot:run`
- Docker logs: `docker logs order-service` (if running in container)

## Next Steps

After testing the basic order service:

1. **Add Kafka Integration** - Event-driven architecture
2. **Implement Stream Processing** - Real-time analytics with Flink/Spark
3. **Add Cassandra** - Analytics data storage
4. **Build Python Microservice** - Data enrichment
5. **Create React Frontend** - Admin dashboard
6. **Add Comprehensive Tests** - Unit, integration, and e2e tests

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │  Order Service  │    │   PostgreSQL    │
│   (Future)      │◄──►│  (Spring Boot)  │◄──►│   (Database)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
                               ▼
                       ┌─────────────────┐
                       │     Redis       │
                       │    (Cache)      │
                       └─────────────────┘
```

Current implementation focuses on the core order management functionality with PostgreSQL and Redis caching.
