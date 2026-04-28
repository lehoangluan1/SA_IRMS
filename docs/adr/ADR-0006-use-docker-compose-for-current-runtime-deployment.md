# ADR-0006: Use Docker Compose for current runtime deployment

Date: 2026-04-28

## Status
Accepted

## Context
The current repository provides Dockerfiles and Docker Compose configuration for local/container runtime. Compose defines PostgreSQL, RabbitMQ, migration, API gateway, domain service containers and frontend container.

Rationale inferred from current implementation and constraints.

## Decision
Document Docker Compose as the current deployment/runtime model for the report and deployment diagrams.

## Consequences
Positive:
- Deployment diagrams can be checked directly against `docker-compose.yml`.
- Keeps current runtime understandable for development and demo operation.

Negative:
- Does not provide built-in load balancing, Kubernetes scheduling, read replicas or warm standby.
- Scaling and failover must be described as future direction unless config is added.

Neutral / trade-offs:
- Docker Compose is a practical runtime boundary for this repo, not a claim of production-grade high availability.

## Alternatives considered
- Kubernetes: not present in the repo and would overstate current deployment.
- Nginx/load balancer: not present in current config.
- Bare-metal/manual Java processes: less aligned with current Compose file.

## Evidence from code/config
- `irms_project/docker-compose.yml`
- `irms_project/backend/Dockerfile`
- `irms_project/frontend/Dockerfile`
- `irms_project/frontend/nitro.config.ts`

## Related documentation and diagrams
- `sections/03_software_architecture.tex`
- `sections/05_appendix.tex`
- `assets/diagrams/source/architecture/15_deployment_diagram_physical_deployment_overview.dot`
- `assets/diagrams/source/architecture/32_deployment_diagram_high_availability_and_failure_isolation.dot`
