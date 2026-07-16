#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
docker compose up --build --detach --wait --wait-timeout 420

echo "Order API: http://localhost:8090/api"
echo "Analytics API: http://localhost:8091"
echo "Dashboard: http://localhost:3000"
echo "Run ./scripts/test-e2e.sh to verify the complete event flow."
