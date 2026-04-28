# ADR-0004: Use PostgreSQL shared runtime database and reporting read models

Date: 2026-04-28

## Status
Accepted

## Context
Docker Compose defines one PostgreSQL container for the current runtime. Migrations create operational tables, outbox/inbox tables and reporting projection tables. Reporting code reads from typed projection/read-model structures where available.

Rationale inferred from current implementation and constraints.

## Decision
Use PostgreSQL as the current shared runtime database, while keeping domain-owned data areas and reporting read models/projections separate inside that runtime.

## Consequences
Positive:
- Keeps deployment simple and consistent with Docker Compose.
- Reporting queries can use projection tables instead of only transactional hot paths.

Negative:
- Shared database runtime requires discipline to avoid cross-domain table coupling.
- Reporting projections may lag behind operational writes.

Neutral / trade-offs:
- Database-per-service, read replica and warm standby are not documented as implemented because no matching Compose/config evidence was found.

## Alternatives considered
- Database-per-service: stronger isolation but higher operational and migration complexity.
- Read replica/warm standby: useful for scale/availability, but not present in current runtime config.
- Single transactional schema for both write and reporting: simpler but can overload operational tables.

## Evidence from code/config
- `irms_project/docker-compose.yml`
- `irms_project/backend/src/main/resources/db/migration/V100__final_polish_combo_and_typed_reporting.sql`
- `irms_project/backend/src/main/resources/db/migration/V103__expand_demo_seed_data.sql`
- `irms_project/backend/src/main/resources/db/migration/V15__rabbitmq_outbox_inbox_workflow.sql`
- `irms_project/backend/src/main/java/SA/irms/reporting/infrastructure/messaging/ReportingProjectionConsumer.java`

## Related documentation and diagrams
- `sections/03_software_architecture.tex`
- `sections/05_appendix.tex`
- `assets/diagrams/source/architecture/17_deployment_diagram_data_and_execution_mapping.dot`
- `assets/diagrams/source/architecture/23_allocation_view_overview_software_to_runtime_mapping.dot`
