# IRMS Non-Functional Test Plan

## Scope

This plan operationalizes the Section 1 commitments captured in [docs/section1-test-matrix.md](../section1-test-matrix.md). It is intentionally backend-first and assumes the new demo seed from `V103__expand_demo_seed_data.sql` is present.

## Test Objectives

- Validate single-request latency targets for critical operational flows.
- Validate concurrency and integrity guarantees for orders, waitlist, payments, refunds, and stock.
- Validate observability promises such as correlation IDs, auditability, alert latency, and projection freshness.
- Validate recoverability of migrations, health recovery, and projection refresh behavior.
- Validate frontend resilience for charts, viewports, accessibility basics, and degraded backend responses.

## Environments

### Local smoke

- Backend started from `backend`
- Frontend started from `frontend`
- Shared PostgreSQL populated with Flyway + demo seed
- Used for developer smoke, contract regression, and quick NFR checks

### Docker compose demo

- Full stack from project compose file
- Used for migration timing, health recovery, projection freshness, and browser-based checks

## Required Seed / Preconditions

- Flyway migrations through `V103__expand_demo_seed_data.sql`
- Reporting projections present for sales, peak hours, best-selling items, revenue, kitchen bottlenecks, staff efficiency, inventory usage, and combo sales
- At least one manager user with `reports.view`, `dashboard.view`, and billing permissions

## Tools

- `curl` or Postman for endpoint timing and header checks
- browser DevTools for frontend timing, viewport, and accessibility spot checks
- Docker logs and `docker compose ps` for recovery checks
- Maven test suite for controller/service concurrency regression
- database query tool for projection freshness and audit evidence

## Assumptions

- Where Section 1 states qualitative expectations without a precise threshold, this plan uses the thresholds from `docs/section1-test-matrix.md`.
- Projection freshness assumes the configured background refresh remains at or under five minutes.
- Frontend render checks assume a standard Chromium browser on a developer workstation.

## Exit Criteria

- All release-frequency cases in `non-functional-test-cases.md` pass.
- No critical failures remain in latency, integrity, security boundary, recoverability, or observability groups.
- Evidence bundle is completed using `non-functional-evidence-template.md`.
