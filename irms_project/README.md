# IRMS

IRMS is a multi-runtime restaurant management system with a React/TanStack frontend and a Spring Boot backend split into gateway, ordering, kitchen, billing, reservation, inventory, notification, reporting, and identity/audit runtimes.

## Architecture at a glance

- `frontend/`: TanStack Start / React app; local dev uses Vite and the production container runs the Nitro server that proxies `/api` to the API gateway.
- `backend/`: Spring Boot Java 17 application started in different runtime modes.
- `docs/section1-test-matrix.md`: test matrix derived from Section 1 plus backend additions such as peak-hour and combo-sales reporting.
- `scripts/run-local.sh` and `scripts/run-local.ps1`: start the full local stack outside Docker.

## Environment requirements

- Java 17
- Maven Wrapper from `backend/mvnw` or local Maven
- Node.js with npm for the frontend
- PostgreSQL local instance for local mode
- RabbitMQ if you want local broker-backed runtimes instead of the default local script mode
- Docker Desktop or Docker Engine with Docker Compose for container mode

## Local environment values

Local PostgreSQL defaults are aligned to this machine setup:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=irms
DB_USER=postgres
DB_USERNAME=postgres
DB_PASSWORD=123456
JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/irms
```

The root `.env.example`, `backend/.env.example`, and `frontend/.env.example` all reflect that local configuration. `VITE_API_BASE_URL` should point to `http://localhost:8080`.

## Run locally with the script

Linux/macOS:

```bash
cd irms_project
chmod +x scripts/run-local.sh
./scripts/run-local.sh
```

Windows PowerShell:

```powershell
cd irms_project
./scripts/run-local.ps1
```

The scripts do the following:

- verify Java 17, npm, PostgreSQL client access, and required ports
- ensure the `irms` database exists when the connected PostgreSQL user can create it
- compile backend sources
- run Flyway migration in `local,migration`
- start the gateway plus all backend runtimes in local mode
- start the frontend with `VITE_API_BASE_URL=http://localhost:8080`
- write logs to `.logs/local`

## Run locally by hand

1. Start PostgreSQL locally and make sure database `irms` exists with user `postgres` and password `123456`.
2. From `backend/`, run the migration runtime:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,migration
```

3. Start each backend runtime in a separate terminal:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,api-gateway
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,ordering-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,kitchen-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,billing-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,reservation-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,inventory-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,notification-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,reporting-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,identity-audit-service
```

4. From `frontend/`, install dependencies and start Vite:

```bash
npm install
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

## Run with Docker Compose

The main compose file is [`docker-compose.yml`](/c:/Users/LUAN/Downloads/SA_BTL/irms_project/docker-compose.yml). The backend-only compose file remains in `backend/docker-compose.yml` for backend-focused workflows.

```bash
cd irms_project
docker compose config
docker compose build
docker compose up -d
```

The root compose starts:

- `postgres`
- `rabbitmq`
- `irms-migration`
- `api-gateway`
- `ordering-service`
- `kitchen-service`
- `billing-service`
- `reservation-service`
- `inventory-service`
- `notification-service`
- `reporting-service`
- `identity-audit-service`
- `frontend`

## Default URLs

- Frontend: `http://localhost:5173`
- API gateway: `http://localhost:8080`
- Health endpoint: `http://localhost:8080/api/health`
- RabbitMQ AMQP: `amqp://localhost:5672`
- RabbitMQ management: `http://localhost:15672`

## Test workflow

Section 1 requirements and derived test coverage live in [docs/section1-test-matrix.md](/c:/Users/LUAN/Downloads/SA_BTL/irms_project/docs/section1-test-matrix.md).

Recommended commands:

```bash
cd backend
./mvnw clean test

cd ../frontend
npm install
npm run build
npm run lint

cd ..
docker compose config
docker compose build
```

The backend suite covers functional contracts and non-functional checks such as concurrency, observability, and security/session behavior. Full test execution should happen after all test files are in place.

## Troubleshooting

- DB credential mismatch: confirm `postgres / 123456` is what your local PostgreSQL instance accepts, or export `DB_USER` and `DB_PASSWORD` before running the scripts.
- Database missing: the scripts can create `irms` only if the connected PostgreSQL user has `CREATE DATABASE` privilege.
- Flyway failures: clear partially created schemas only if you understand the data impact, then rerun the `migration` runtime.
- RabbitMQ disabled locally: the local scripts run with `IRMS_RABBITMQ_ENABLED=false` unless you override it.
- Port conflict: free `5173`, `8080` through `8088`, `5432`, `5672`, and `15672`, or override ports with environment variables.
- CORS issues: keep `VITE_API_BASE_URL=http://localhost:8080` and use the allowed origins from backend properties.
- Auth/token issues: `/api/auth/login` is public, but the rest of the API expects a valid bearer token. Idle timeout is configured for 15 minutes by default.