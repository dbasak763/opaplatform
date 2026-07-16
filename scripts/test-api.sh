#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
curl --fail --silent --user admin:admin123 http://localhost:8090/api/actuator/health
curl --fail --silent --user admin:admin123 http://localhost:8090/api/users?page=0\&size=2
curl --fail --silent --user admin:admin123 http://localhost:8090/api/products?page=0\&size=2
curl --fail --silent --user admin:admin123 http://localhost:8090/api/orders?page=0\&size=2
echo
echo "Order API smoke tests passed."
