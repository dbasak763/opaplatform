#!/bin/bash

# Start the Order Service
echo "Starting Order Service..."

# Navigate to project root
cd "$(dirname "$0")/.."

# Check if infrastructure is running
if ! docker ps | grep -q "order-postgres"; then
    echo "Infrastructure services are not running. Starting them first..."
    ./scripts/start-infrastructure.sh
fi

# Navigate to order service directory
cd order-service

# Build and run the Spring Boot application
echo "Building and starting Order Service..."
./mvnw clean spring-boot:run

echo "Order Service started successfully!"
echo "API available at: http://localhost:8090/api"
