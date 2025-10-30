#!/bin/bash

# Start All Services
echo "Starting Order Processing System..."

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo "Port $port is already in use"
        return 1
    fi
    return 0
}

# Start infrastructure services
echo "Starting infrastructure services..."
docker-compose up -d postgres redis kafka zookeeper cassandra

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 30

# Start Order Service
echo "Starting Order Service..."
./start-order-service.sh &
ORDER_PID=$!

# Wait a bit for order service to start
sleep 10

# Start Analytics Service
echo "Starting Analytics Service..."
./start-analytics-service.sh &
ANALYTICS_PID=$!

# Wait a bit for analytics service to start
sleep 10

# Start Frontend
echo "Starting Frontend Dashboard..."
./start-frontend.sh &
FRONTEND_PID=$!

echo "All services started!"
echo "Order Service: http://localhost:8090/api"
echo "Analytics Service: http://localhost:8091"
echo "Frontend Dashboard: http://localhost:3000"
echo "Kafka UI: http://localhost:8080"
echo "pgAdmin: http://localhost:8081"

# Wait for user input to stop services
echo "Press Ctrl+C to stop all services..."
trap 'echo "Stopping services..."; kill $ORDER_PID $ANALYTICS_PID $FRONTEND_PID 2>/dev/null; docker-compose down; exit' INT

# Keep script running
wait
