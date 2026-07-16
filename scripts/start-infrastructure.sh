#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
docker compose up --detach --wait --wait-timeout 300 postgres redis zookeeper kafka cassandra
