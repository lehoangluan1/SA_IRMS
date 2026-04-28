# ADR-0003: Use RabbitMQ, outbox, inbox and manual ack for domain events

Date: 2026-04-28

## Status
Accepted

## Context
Backend services publish and consume domain events through RabbitMQ. The code also persists outgoing and processed events through `outbox_events` and `processed_events`, and consumers use manual acknowledgment support.

Rationale inferred from current implementation and constraints.

## Decision
Use RabbitMQ with Outbox/Inbox-style tables and `ManualAckConsumerSupport` for asynchronous domain event delivery.

## Consequences
Positive:
- Core transactions can persist state before background effects are dispatched.
- Consumers can retry or record processed events without blocking the hot path.

Negative:
- Event freshness becomes eventually consistent.
- Operations must monitor broker, outbox backlog and consumer failures.

Neutral / trade-offs:
- Kafka, Redis Streams and in-memory queues are not represented as current runtime choices because no matching config/code evidence was found.

## Alternatives considered
- In-memory async execution: simpler but loses events on process failure.
- Kafka: stronger streaming ecosystem but not present in code/config.
- Pure synchronous calls: easier to trace but couples reporting, notification and audit to user-facing transactions.

## Evidence from code/config
- `irms_project/docker-compose.yml`
- `irms_project/backend/src/main/java/SA/irms/common/messaging/RabbitMqEventPublisher.java`
- `irms_project/backend/src/main/java/SA/irms/common/outbox/RabbitMqOutboxRelay.java`
- `irms_project/backend/src/main/java/SA/irms/common/messaging/ManualAckConsumerSupport.java`
- `irms_project/backend/src/main/java/SA/irms/common/inbox/ProcessedEventRepository.java`
- `irms_project/backend/src/main/resources/db/migration/V15__rabbitmq_outbox_inbox_workflow.sql`

## Related documentation and diagrams
- `sections/03_software_architecture.tex`
- `sections/05_appendix.tex`
- `assets/diagrams/source/architecture/25_component_diagram_supporting_async_and_governance.dot`
- `assets/diagrams/source/architecture/32_deployment_diagram_high_availability_and_failure_isolation.dot`
