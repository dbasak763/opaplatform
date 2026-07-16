#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

(cd order-service && ./mvnw clean test)

python3 -m venv .venv
.venv/bin/pip install --quiet -r analytics-service/requirements.txt
PYTHONPATH=analytics-service .venv/bin/pytest -q analytics-service/tests

(cd frontend && corepack enable && corepack prepare pnpm@11.9.0 --activate && pnpm install --frozen-lockfile --config.node-linker=hoisted && CI=true node node_modules/react-scripts/bin/react-scripts.js build)

echo "All component tests passed."
