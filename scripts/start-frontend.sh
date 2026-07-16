#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
docker compose up --build --detach --wait --wait-timeout 420 frontend
