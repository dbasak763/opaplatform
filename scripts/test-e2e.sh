#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
for command in curl jq docker; do
  command -v "$command" >/dev/null || { echo "Missing required command: $command" >&2; exit 1; }
done

auth=(--user admin:admin123)
order_api=http://localhost:8090/api
analytics_api=http://localhost:8091

curl --fail --silent "${auth[@]}" "$order_api/actuator/health" | jq -e '.status == "UP"' >/dev/null
curl --fail --silent "$analytics_api/health" | jq -e '.status == "healthy"' >/dev/null

before=$(curl --fail --silent "$analytics_api/metrics/orders" | jq -r '.total_orders')
payload='{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "items": [{"productId": "660e8400-e29b-41d4-a716-446655440001", "quantity": 1}],
  "shippingAddress": {
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA"
  },
  "taxAmount": 8.00,
  "shippingAmount": 9.99,
  "notes": "Automated end-to-end validation"
}'

created=$(curl --fail --silent "${auth[@]}" -H 'Content-Type: application/json' -d "$payload" "$order_api/orders")
order_id=$(jq -er '.id' <<<"$created")

for _ in $(seq 1 60); do
  current=$(curl --fail --silent "$analytics_api/metrics/orders" | jq -r '.total_orders')
  if (( current > before )); then
    break
  fi
  sleep 2
done

current=$(curl --fail --silent "$analytics_api/metrics/orders" | jq -r '.total_orders')
(( current > before )) || { echo "Kafka event did not reach analytics within 120 seconds" >&2; exit 1; }

curl --fail --silent "$analytics_api/metrics/realtime" | jq -e --arg id "$order_id" '.recent_orders | any(.orderId == $id)' >/dev/null
docker compose exec -T redis redis-cli EXISTS analytics:metrics:snapshot | grep -qx '1'
docker compose exec -T cassandra cqlsh -e 'SELECT event_id FROM analytics.order_events LIMIT 1;' | grep -q '(1 rows)'
docker compose exec -T cassandra cqlsh -e "SELECT metric_key FROM analytics.analytics_metrics WHERE metric_key = 'current';" | grep -q 'current'
curl --fail --silent http://localhost:3000/ >/dev/null

echo "End-to-end flow passed for order $order_id: Spring Boot -> PostgreSQL/Kafka -> Python -> Cassandra/Redis -> React APIs."
