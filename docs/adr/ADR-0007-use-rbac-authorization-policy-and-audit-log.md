# ADR-0007: Use RBAC, authorization policy and audit log

Date: 2026-04-28

## Status
Accepted

## Context
IRMS includes identity/audit runtime, role-oriented UI behavior, authorization policies and audit log workflows for sensitive actions such as refunds, menu changes and audit follow-up.

Rationale inferred from current implementation and constraints.

## Decision
Use RBAC for role-based access, policy checks for contextual sensitive operations, and audit logs for important operational changes.

## Consequences
Positive:
- Sensitive operations are controlled by roles and business context.
- Audit history supports refund review, configuration traceability and operational accountability.

Negative:
- Requires consistent role/permission mapping across frontend and backend.
- Audit tables and events add storage and maintenance overhead.

Neutral / trade-offs:
- OAuth is not documented as implemented because no matching provider/config evidence was found.

## Alternatives considered
- Frontend-only checks: rejected because API calls could bypass UI restrictions.
- Single admin role: too coarse for manager, cashier, chef, host and server workflows.
- OAuth provider integration: not represented as current runtime due to lack of evidence.

## Evidence from code/config
- `irms_project/frontend/src/lib/auth.tsx`
- `irms_project/frontend/src/lib/permissions.ts`
- `irms_project/backend/src/main/java/SA/irms/identity/infrastructure/messaging/AuditRecordingRequestedConsumer.java`
- `irms_project/backend/src/main/java/SA/irms/identity/persistence/IdentityEventOperationsRepository.java`
- `irms_project/backend/src/main/resources/db/migration`

## Related documentation and diagrams
- `sections/02_system_modeling.tex`
- `sections/03_software_architecture.tex`
- `sections/04_detailed_design.tex`
- `sections/05_appendix.tex`
- `assets/diagrams/source/activity/uc-adm-02-manage-roles-and-permissions.dot`
- `assets/diagrams/source/activity/uc-adm-03-review-audit-log.dot`
