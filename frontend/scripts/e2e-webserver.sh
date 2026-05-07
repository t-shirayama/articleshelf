#!/bin/sh
set -eu

PROJECT_NAME="${E2E_COMPOSE_PROJECT_NAME:-readstack-e2e}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

cd "$ROOT_DIR"
docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml down -v --remove-orphans >/dev/null 2>&1 || true
exec docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml up --build
