#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
LOG_DIR="$ROOT_DIR/.logs/local"
mkdir -p "$LOG_DIR"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-irms}"
DB_USER="${DB_USER:-${DB_USERNAME:-postgres}}"
DB_USERNAME="$DB_USER"
DB_PASSWORD="${DB_PASSWORD:-123456}"
JDBC_DATABASE_URL="${JDBC_DATABASE_URL:-jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}}"
RABBITMQ_HOST="${RABBITMQ_HOST:-localhost}"
RABBITMQ_PORT="${RABBITMQ_PORT:-5672}"
RABBITMQ_USERNAME="${RABBITMQ_USERNAME:-postgre}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-123456}"
VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://localhost:8080}"
IRMS_INTERNAL_SERVICE_TOKEN="${IRMS_INTERNAL_SERVICE_TOKEN:-dev-internal-service-token-change-me}"
IRMS_ACCESS_TOKEN_SIGNING_SECRET="${IRMS_ACCESS_TOKEN_SIGNING_SECRET:-dev-access-token-signing-secret-change-me}"

SERVICES=(
  "api-gateway:8080"
  "ordering-service:8081"
  "kitchen-service:8082"
  "billing-service:8083"
  "reservation-service:8084"
  "inventory-service:8085"
  "notification-service:8086"
  "reporting-service:8087"
  "identity-audit-service:8088"
)
PIDS=()

cleanup() {
  for pid in "${PIDS[@]:-}"; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
    fi
  done
}
trap cleanup EXIT INT TERM

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

check_java() {
  local version
  version="$(java -version 2>&1 | head -n 1)"
  if [[ "$version" != *"17."* && "$version" != *"version \"17"* ]]; then
    echo "Java 17 is required. Current: $version" >&2
    exit 1
  fi
}

ensure_port_free() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    if lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      echo "Port $port is already in use." >&2
      exit 1
    fi
    return
  fi

  if command -v ss >/dev/null 2>&1; then
    if ss -ltn "( sport = :$port )" | grep -q LISTEN; then
      echo "Port $port is already in use." >&2
      exit 1
    fi
  fi
}

ensure_database() {
  if ! command -v psql >/dev/null 2>&1; then
    echo "psql not found in PATH. Install PostgreSQL client or create database '$DB_NAME' manually." >&2
    exit 1
  fi

  export PGPASSWORD="$DB_PASSWORD"

  if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -tAc "select 1" >/dev/null 2>&1; then
    echo "Cannot connect to PostgreSQL at ${DB_HOST}:${DB_PORT} with user ${DB_USER}." >&2
    exit 1
  fi

  if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -tAc "select 1 from pg_database where datname='${DB_NAME}'" | grep -q 1; then
    echo "Creating database '${DB_NAME}'..."
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "create database ${DB_NAME}" >/dev/null
  fi
}

start_backend_service() {
  local service="$1"
  local port="$2"
  local log_file="$LOG_DIR/${service}.log"

  (
    cd "$BACKEND_DIR"
    DB_HOST="$DB_HOST" \
    DB_PORT="$DB_PORT" \
    DB_NAME="$DB_NAME" \
    DB_USER="$DB_USER" \
    DB_USERNAME="$DB_USERNAME" \
    DB_PASSWORD="$DB_PASSWORD" \
    JDBC_DATABASE_URL="$JDBC_DATABASE_URL" \
    RABBITMQ_HOST="$RABBITMQ_HOST" \
    RABBITMQ_PORT="$RABBITMQ_PORT" \
    RABBITMQ_USERNAME="$RABBITMQ_USERNAME" \
    RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
    IRMS_INTERNAL_SERVICE_TOKEN="$IRMS_INTERNAL_SERVICE_TOKEN" \
    IRMS_ACCESS_TOKEN_SIGNING_SECRET="$IRMS_ACCESS_TOKEN_SIGNING_SECRET" \
    IRMS_RABBITMQ_ENABLED=false \
    IRMS_FLYWAY_ENABLED=false \
    IRMS_RUNTIME_MODE="$service" \
    IRMS_RUNTIME_SERVICE="$service" \
    SERVER_PORT="$port" \
    ./mvnw spring-boot:run -Dspring-boot.run.profiles="local,$service" >"$log_file" 2>&1
  ) &

  PIDS+=("$!")
}

start_frontend() {
  local log_file="$LOG_DIR/frontend.log"
  (
    cd "$FRONTEND_DIR"
    VITE_API_BASE_URL="$VITE_API_BASE_URL" npm run dev -- --host 0.0.0.0 >"$log_file" 2>&1
  ) &

  PIDS+=("$!")
}

require_command java
require_command npm
require_command bash
check_java

for mapping in "${SERVICES[@]}"; do
  ensure_port_free "${mapping##*:}"
done
ensure_port_free 5173

ensure_database

echo "Compiling backend sources..."
(
  cd "$BACKEND_DIR"
  DB_HOST="$DB_HOST" \
  DB_PORT="$DB_PORT" \
  DB_NAME="$DB_NAME" \
  DB_USER="$DB_USER" \
  DB_USERNAME="$DB_USERNAME" \
  DB_PASSWORD="$DB_PASSWORD" \
  JDBC_DATABASE_URL="$JDBC_DATABASE_URL" \
  IRMS_RUNTIME_MODE=migration \
  IRMS_RUNTIME_SERVICE=migration \
  IRMS_RABBITMQ_ENABLED=false \
  ./mvnw -q compile
)

echo "Running Flyway migration..."
(
  cd "$BACKEND_DIR"
  DB_HOST="$DB_HOST" \
  DB_PORT="$DB_PORT" \
  DB_NAME="$DB_NAME" \
  DB_USER="$DB_USER" \
  DB_USERNAME="$DB_USERNAME" \
  DB_PASSWORD="$DB_PASSWORD" \
  JDBC_DATABASE_URL="$JDBC_DATABASE_URL" \
  IRMS_RABBITMQ_ENABLED=false \
  IRMS_FLYWAY_ENABLED=true \
  IRMS_RUNTIME_MODE=migration \
  IRMS_RUNTIME_SERVICE=migration \
  SERVER_PORT=0 \
  ./mvnw -q spring-boot:run -Dspring-boot.run.profiles="local,migration" >"$LOG_DIR/migration.log" 2>&1
)

echo "Installing frontend dependencies if needed..."
(
  cd "$FRONTEND_DIR"
  npm install
)

for mapping in "${SERVICES[@]}"; do
  start_backend_service "${mapping%%:*}" "${mapping##*:}"
done

start_frontend

cat <<EOF
IRMS local stack is starting.

Frontend:   http://localhost:5173
API Gateway: http://localhost:8080
Health:     http://localhost:8080/api/health

Logs:
  Backend + migration: $LOG_DIR
  Frontend:            $LOG_DIR/frontend.log

Press Ctrl+C to stop all spawned processes.
EOF

wait
