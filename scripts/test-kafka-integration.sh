#!/bin/bash

# Test Kafka Integration Script
# This script tests the Kafka event streaming functionality

echo "🚀 Testing Kafka Integration for Order Processing System"
echo "======================================================="

# Base URL for the API
BASE_URL="http://localhost:8090/api"
AUTH="admin:admin123"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo -e "${YELLOW}Prerequisites Check:${NC}"
echo "1. Ensure infrastructure is running: docker-compose up -d"
echo "2. Ensure order service is running: ./scripts/start-order-service.sh"
echo "3. Check Kafka UI at: http://localhost:8080"
echo ""

# Function to check if service is running
check_service() {
    local url=$1
    local name=$2
    
    if curl -s -f "$url" > /dev/null; then
        echo -e "${GREEN}✓ $name is running${NC}"
        return 0
    else
        echo -e "${RED}✗ $name is not accessible${NC}"
        return 1
    fi
}

echo -e "${YELLOW}Step 1: Checking Services Status${NC}"
echo "----------------------------------------"
check_service "$BASE_URL/actuator/health" "Order Service"
check_service "http://localhost:8080" "Kafka UI"

echo ""
echo -e "${YELLOW}Step 2: Testing Order Creation (Triggers OrderCreatedEvent)${NC}"
echo "-----------------------------------------------------------"

# Create a new order to trigger OrderCreatedEvent
echo "Creating a new order..."
ORDER_RESPONSE=$(curl -s -u $AUTH -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "orderItems": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440011",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ],
    "shippingAddress": {
      "street": "123 Test St",
      "city": "Test City",
      "state": "TS",
      "zipCode": "12345",
      "country": "USA"
    },
    "taxAmount": 5.99,
    "shippingAmount": 9.99
  }')

if [ $? -eq 0 ]; then
    ORDER_ID=$(echo $ORDER_RESPONSE | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✓ Order created successfully${NC}"
    echo "Order ID: $ORDER_ID"
    echo "Response: $ORDER_RESPONSE"
else
    echo -e "${RED}✗ Failed to create order${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 3: Testing Order Status Update (Triggers OrderStatusChangedEvent)${NC}"
echo "-----------------------------------------------------------------------"

if [ -n "$ORDER_ID" ]; then
    echo "Updating order status to CONFIRMED..."
    STATUS_RESPONSE=$(curl -s -u $AUTH -X PUT "$BASE_URL/orders/$ORDER_ID/status?status=CONFIRMED")
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Order status updated successfully${NC}"
        echo "Response: $STATUS_RESPONSE"
    else
        echo -e "${RED}✗ Failed to update order status${NC}"
    fi
    
    echo ""
    echo "Updating order status to SHIPPED..."
    curl -s -u $AUTH -X PUT "$BASE_URL/orders/$ORDER_ID/status?status=SHIPPED"
    echo -e "${GREEN}✓ Order status updated to SHIPPED${NC}"
else
    echo -e "${RED}✗ No order ID available for status update${NC}"
fi

echo ""
echo -e "${YELLOW}Step 4: Testing Order Cancellation (Triggers OrderCancelledEvent)${NC}"
echo "------------------------------------------------------------------"

# Create another order for cancellation test
echo "Creating another order for cancellation test..."
CANCEL_ORDER_RESPONSE=$(curl -s -u $AUTH -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440002",
    "orderItems": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440012",
        "quantity": 1,
        "unitPrice": 19.99
      }
    ],
    "shippingAddress": {
      "street": "456 Cancel St",
      "city": "Cancel City",
      "state": "CC",
      "zipCode": "54321",
      "country": "USA"
    },
    "taxAmount": 1.99,
    "shippingAmount": 4.99
  }')

CANCEL_ORDER_ID=$(echo $CANCEL_ORDER_RESPONSE | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -n "$CANCEL_ORDER_ID" ]; then
    echo "Cancelling order..."
    CANCEL_RESPONSE=$(curl -s -u $AUTH -X PUT "$BASE_URL/orders/$CANCEL_ORDER_ID/cancel")
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Order cancelled successfully${NC}"
        echo "Response: $CANCEL_RESPONSE"
    else
        echo -e "${RED}✗ Failed to cancel order${NC}"
    fi
else
    echo -e "${RED}✗ Failed to create order for cancellation test${NC}"
fi

echo ""
echo -e "${YELLOW}Step 5: Checking Kafka Topics and Messages${NC}"
echo "-------------------------------------------"
echo "To verify Kafka events were published:"
echo "1. Open Kafka UI: http://localhost:8080"
echo "2. Check topics: 'order-events' and 'order-analytics'"
echo "3. View messages in each topic to see the events"
echo ""
echo "Expected events:"
echo "- OrderCreatedEvent (2 instances)"
echo "- OrderStatusChangedEvent (2 instances for first order)"
echo "- OrderCancelledEvent (1 instance for second order)"

echo ""
echo -e "${YELLOW}Step 6: Checking Application Logs${NC}"
echo "----------------------------------"
echo "Check the order service logs for Kafka event publishing messages:"
echo "Look for log entries like:"
echo "- 'Publishing order created event...'"
echo "- 'Order created event published successfully...'"
echo "- 'Received order event...'"
echo "- 'Processing order created event...'"

echo ""
echo -e "${GREEN}🎉 Kafka Integration Test Complete!${NC}"
echo "======================================="
echo ""
echo "Next steps:"
echo "1. Verify events in Kafka UI"
echo "2. Check application logs for event processing"
echo "3. Monitor consumer group offsets"
echo "4. Test real-time analytics processing"
