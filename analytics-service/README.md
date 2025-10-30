# Analytics Service

Real-time analytics microservice for the Order Processing System, built with Python, FastAPI, Kafka, and Cassandra.

## Features

- **Real-time Event Processing**: Consumes order events from Kafka
- **Analytics API**: RESTful endpoints for metrics and insights
- **WebSocket Support**: Real-time dashboard updates
- **Data Storage**: Cassandra for analytics data, Redis for caching
- **Scalable Architecture**: Async processing with FastAPI

## API Endpoints

### Health & Status
- `GET /health` - Service health check

### Metrics
- `GET /metrics/orders` - Overall order metrics
- `GET /metrics/realtime` - Real-time statistics
- `GET /metrics/revenue/hourly` - Hourly revenue data
- `GET /metrics/products/top` - Top performing products

### WebSocket
- `WS /ws/realtime` - Real-time updates for dashboard

## Event Processing

The service processes the following Kafka events:
- `OrderCreatedEvent` - New order creation
- `OrderStatusChangedEvent` - Order status updates
- `OrderCancelledEvent` - Order cancellations

## Data Models

### OrderMetrics
```python
{
    "total_orders": int,
    "total_revenue": float,
    "orders_by_status": dict,
    "avg_order_value": float,
    "orders_per_hour": dict
}
```

### RealtimeStats
```python
{
    "current_orders_per_minute": float,
    "revenue_per_minute": float,
    "active_users": int,
    "top_products": list,
    "recent_orders": list
}
```

## Setup & Running

### Local Development
```bash
# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Start the service
python main.py
```

### Docker
```bash
# Build and run with Docker Compose
docker-compose up analytics-service
```

## Configuration

Environment variables:
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker addresses (default: localhost:9092)
- `REDIS_HOST` - Redis host (default: localhost)
- `CASSANDRA_HOSTS` - Cassandra hosts (default: localhost)

## Dependencies

- **FastAPI** - Web framework
- **kafka-python** - Kafka client
- **cassandra-driver** - Cassandra database driver
- **redis** - Redis client
- **uvicorn** - ASGI server

## Architecture

```
Kafka Events → Analytics Service → Cassandra (Historical Data)
                     ↓                ↓
                Redis Cache ← → WebSocket → Frontend Dashboard
```
