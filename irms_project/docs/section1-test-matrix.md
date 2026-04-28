# IRMS Section 1 Test Matrix

## Functional scope

| Area | Section 1 expectation | Backend/API coverage to verify |
| --- | --- | --- |
| Authentication and session | Staff log in, inspect their session, and sign out safely | `/api/auth/login`, `/api/auth/me`, `/api/auth/logout` |
| Reservations and tables | Front-of-house manages reservations, walk-ins, table readiness, and waitlist | `/api/reservations/**`, `/api/tables/**`, `/api/waitlist/**`, reservation notification endpoint |
| Ordering and menu | Staff build orders from menu items, modifiers, promotions, and combo bundles | `/api/orders/**`, `/api/menu/**`, `/api/menu/categories/**`, `/api/menu/items/**`, `/api/menu/promotions/**`, `/api/menu/combos` |
| Kitchen operations | Kitchen sees tickets and controls preparation/serve/return/cancel flows | `/api/kitchen/**` |
| Billing and refunds | Bills, payments, receipts, splits, and refunds are traceable | `/api/billing/**`, `/api/bills/**`, `/api/payments/**`, `/api/refunds/**` |
| Inventory | Operators manage stock, alerts, and ingredient lifecycle | `/api/inventory/**` |
| Reporting and dashboard | Managers inspect operations, dashboard KPIs, exports, peak hours, and combo sales | `/api/dashboard`, `/api/reports/**` |
| Staff, shifts, settings, audit | Managers maintain staff, shifts, settings, and audit follow-up | `/api/staff`, `/api/shifts`, `/api/settings`, `/api/audit/**` |
| Notifications and health | Inbox messages and service health remain visible | `/api/notifications/**`, `/api/health` |

## Non-functional scope

| Requirement | Section 1 target | Verification approach |
| --- | --- | --- |
| Single-request latency | Menu/table lookup under 1s, order confirm/send under 2s normal and under 3s at peak, kitchen display within 2s | Controller/service timing smoke tests around representative endpoints using deterministic fixtures |
| Reliability and integrity | No lost confirmed orders, duplicate ticket/payment under 0.1% | Idempotency and concurrency tests around order confirm, waitlist seat, payment/refund, inventory updates |
| Scalability under peak load | Sustain 1.5x peak burst for 10–15 minutes without redesign | Parallelized request/service tests that stress order creation, reporting, and table transitions with bounded assertions |
| Availability and recoverability | Recover failed service runtime/container in under 10 minutes, repeat migration safely | Health endpoint assertions and migration/idempotency tests |
| Security and session control | Protected endpoints require auth, idle timeout 15 minutes, sensitive actions audited | Unauthorized/forbidden contract tests, current-user/session tests, audit/correlation checks |
| Observability | Correlation ID on 100% of requests, low-stock alert in 60s, dashboard lag within 5 minutes | Correlation filter tests, inventory alert tests, reporting freshness assertions |
| Maintainability/extensibility | Channel-specific logic isolated behind adapters/config | Focused unit tests on exported documents/reports to ensure contracts stay stable when implementations change |

## Backend additions beyond the original Section 1 text

| Capability | Why it was added to the matrix |
| --- | --- |
| Peak-hour reporting | `ReportingQueryService` and `ReportingController` expose peak-hour data used for operational insights |
| Combo sales reporting | Backend already computes combo sales, so reporting tests must cover it |
| Combo selection during order creation | `OrdersController` now accepts `comboSelections`, which is central to the ordering flow |
| Menu combo management | Backend exposes combo create/update and menu overview includes combo structures |
