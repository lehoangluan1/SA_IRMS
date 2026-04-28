# ADR-0002: Use Nitro proxy and API gateway

Date: 2026-04-28

## Status
Accepted

## Context
Frontend production container runs the TanStack Start/Nitro server. Nitro proxies `/api/**` to `api-gateway:8080`, and the Spring Boot API gateway routes requests to internal service runtimes.

Rationale inferred from current implementation and constraints.

## Decision
Use the frontend Nitro server as the frontend HTTP runtime and `/api` proxy, then use `ApiGatewayApplication` and `GatewayProxyController` as the backend gateway boundary.

## Consequences
Positive:
- Frontend does not need to know individual backend service URLs.
- Deployment diagram can show a concrete frontend container and backend gateway container.

Negative:
- Adds one proxy hop before backend services.
- Gateway route configuration must stay synchronized with service runtime URLs.

Neutral / trade-offs:
- No Nginx runtime is documented because no Nginx config exists in the repo.

## Alternatives considered
- Direct browser calls to every backend service: exposes internal topology and couples UI to service ports.
- Nginx reverse proxy: not chosen as current documentation because there is no repo evidence.

## Evidence from code/config
- `irms_project/frontend/nitro.config.ts`
- `irms_project/frontend/Dockerfile`
- `irms_project/docker-compose.yml`
- `irms_project/backend/src/main/java/SA/irms/gateway/ApiGatewayApplication.java`
- `irms_project/backend/src/main/java/SA/irms/gateway/GatewayProxyController.java`

## Related documentation and diagrams
- `sections/03_software_architecture.tex`
- `sections/05_appendix.tex`
- `assets/diagrams/source/architecture/23_allocation_view_overview_software_to_runtime_mapping.dot`
- `assets/diagrams/source/architecture/33_architecture_overview_react_nitro_spring_postgresql.dot`
