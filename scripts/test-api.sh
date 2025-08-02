#!/bin/bash

# Test the Order Service API
echo "Testing Order Service API..."

BASE_URL="http://localhost:8090/api"
AUTH="admin:admin123"

echo ""
echo "🧪 Testing API endpoints..."
echo ""

# Test health endpoint
echo "1. Testing health endpoint..."
curl -s -u $AUTH "$BASE_URL/../actuator/health" | jq '.' || echo "Health check failed"
echo ""

# Test get all users
echo "2. Testing get all users..."
curl -s -u $AUTH "$BASE_URL/users" | jq '.content[0:2]' || echo "Get users failed"
echo ""

# Test get all products
echo "3. Testing get all products..."
curl -s -u $AUTH "$BASE_URL/products" | jq '.content[0:2]' || echo "Get products failed"
echo ""

# Test get all orders
echo "4. Testing get all orders..."
curl -s -u $AUTH "$BASE_URL/orders" | jq '.content[0:2]' || echo "Get orders failed"
echo ""

# Test create order
echo "5. Testing create order..."
ORDER_DATA='{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "items": [
    {
      "productId": "660e8400-e29b-41d4-a716-446655440001",
      "quantity": 1
    }
  ],
  "taxAmount": 8.00,
  "shippingAmount": 9.99,
  "notes": "Test order from API"
}'

curl -s -u $AUTH -X POST \
  -H "Content-Type: application/json" \
  -d "$ORDER_DATA" \
  "$BASE_URL/orders" | jq '.' || echo "Create order failed"
echo ""

# Test analytics
echo "6. Testing analytics - total revenue..."
curl -s -u $AUTH "$BASE_URL/orders/analytics/revenue" || echo "Analytics failed"
echo ""

echo ""
echo "✅ API testing completed!"
echo ""
echo "Available endpoints:"
echo "  - Users: GET $BASE_URL/users"
echo "  - Products: GET $BASE_URL/products"
echo "  - Orders: GET $BASE_URL/orders"
echo "  - Create Order: POST $BASE_URL/orders"
echo "  - Health: GET $BASE_URL/../actuator/health"
echo ""
echo "Authentication: Basic Auth (admin:admin123)"
