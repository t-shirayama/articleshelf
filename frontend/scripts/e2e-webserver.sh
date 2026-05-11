#!/bin/sh
set -eu

PROJECT_NAME="${E2E_COMPOSE_PROJECT_NAME:-articleshelf-e2e}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

cd "$ROOT_DIR"
docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml down -v --remove-orphans >/dev/null 2>&1 || true
docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml up --build -d

for attempt in $(seq 1 90); do
  backend_container="$(docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml ps -q backend)"
  frontend_container="$(docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml ps -q frontend)"
  backend_health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$backend_container" 2>/dev/null || true)"
  frontend_status="$(docker inspect -f '{{.State.Status}}' "$frontend_container" 2>/dev/null || true)"

  if [ "$backend_health" = "healthy" ] && [ "$frontend_status" = "running" ]; then
    break
  fi

  if [ "$attempt" = "90" ]; then
    docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml ps
    docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml logs --tail 200
    exit 1
  fi

  sleep 2
done

exec docker compose -p "$PROJECT_NAME" -f docker-compose.e2e.yml logs -f
