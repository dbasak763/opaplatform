#!/bin/bash

# Start basic infrastructure services
echo "Starting basic infrastructure services..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker first."
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/.."

# Start services using the basic docker-compose file
echo "Starting PostgreSQL, Redis, and pgAdmin..."
docker-compose -f docker-compose-basic.yml up -d

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 10

# Check if PostgreSQL is ready
echo "Checking PostgreSQL connection..."
until docker exec order-postgres pg_isready -U orderuser -d orderdb; do
    echo "Waiting for PostgreSQL to be ready..."
    sleep 2
done

echo "✅ PostgreSQL is ready!"

# Check if Redis is ready
echo "Checking Redis connection..."
until docker exec order-redis redis-cli ping; do
    echo "Waiting for Redis to be ready..."
    sleep 2
done

echo "✅ Redis is ready!"

echo ""
echo "🚀 Infrastructure services are running!"
echo ""
echo "Services:"
echo "  - PostgreSQL: localhost:5432"
echo "  - Redis: localhost:6379"
echo "  - pgAdmin: http://localhost:8081"
echo ""
echo "pgAdmin credentials:"
echo "  - Email: admin@orderapp.com"
echo "  - Password: admin123"
echo ""
echo "PostgreSQL credentials:"
echo "  - Host: localhost"
echo "  - Port: 5432"
echo "  - Database: orderdb"
echo "  - Username: orderuser"
echo "  - Password: orderpass"
echo ""
