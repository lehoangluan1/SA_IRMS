# IRMS Backend Context Map

## Topology

IRMS runs as a service-based system with a shared PostgreSQL database and an API gateway in front. The backend source of truth is `backend/src/main/resources/service-topology.yml`.

| Service | Owns | Authoritative backend touchpoints |
| --- | --- | --- |
| `identity-audit-service` | login, session, roles, audit | `/api/auth/**`, `/api/staff`, `/api/shifts`, `/api/audit/**` |
| `reservation-service` | reservations, waitlist, seating, table assignment | `/api/reservations/**`, `/api/tables/**`, `/api/waitlist/**` |
| `ordering-service` | menu, modifiers, combos, orders | `/api/menu/**`, `/api/orders/**` |
| `kitchen-service` | ticket routing, station queues, serve/return states | `/api/kitchen/**` |
| `billing-service` | bills, splits, payments, refunds, receipts | `/api/billing/**`, `/api/bills/**`, `/api/payments/**`, `/api/refunds/**` |
| `inventory-service` | stock, usage, low-stock alerts | `/api/inventory/**` |
| `notification-service` | outbound/in-app notifications | `/api/notifications/**` |
| `reporting-service` | dashboard snapshot, typed reports, exports | `/api/dashboard`, `/api/reports/**` |

## Dashboard And Reports Contract

### Dashboard source of truth

- `GET /api/dashboard`
- Live operational counters come from `ReportingProjectionRepository.loadOperationsMetrics()`
- Alerts come from `JdbcOperationsDashboardQueryRepository`
- Active orders come from transactional tables joined with identity display names
- Historical/analytical chart payload comes from the latest `operations` snapshot in `report_snapshots`

### Reports source of truth

- `GET /api/reports/operations`
  - curated operations snapshot + live counters
  - still the only backend contract that contains `categoryRevenue`
- `GET /api/reports/sales`
  - typed daily net-sales rows for weekly revenue chart
- `GET /api/reports/peak-hours`
  - typed hourly demand rows
- `GET /api/reports/best-selling-items`
  - typed top-dish rows
- `GET /api/reports/revenue`
  - typed payment-method revenue mix
- `GET /api/reports/kitchen-bottlenecks`
  - typed station delay rows
- `GET /api/reports/staff-efficiency`
  - typed staff throughput rows
- `GET /api/reports/inventory-usage`
  - typed ingredient usage rows
- `GET /api/reports/combo-sales`
  - typed combo performance rows
- `GET /api/reports/export`
  - export surface for the same report families

## Live Vs Projection

| Data | Comes from | Notes |
| --- | --- | --- |
| `activeTables`, `openOrders`, `readyToServe`, `kitchenQueue`, `averageKitchenWaitMinutes`, `openLowStockAlerts` | live transactional query | This is why dashboard stays operational, not purely analytical. |
| `revenueToday`, `transactionsToday`, `reservationCountToday`, `refundsToday`, `revenueTrend`, `weeklyRevenue`, `categoryRevenue` | `operations` snapshot payload | Snapshot remains useful because some analytical fields are not present in typed projection tables yet. |
| `sales`, `peak-hours`, `best-selling-items`, `revenue`, `kitchen-bottlenecks`, `staff-efficiency`, `inventory-usage`, `combo-sales` | typed reporting projection tables | Best place for reports page tables/charts that need stable typed contracts. |

## Underused Data That Already Existed

- `report_snapshots.payload` in `V2__seed_demo_data.sql` already held:
  - `revenueTrend`
  - `weeklyRevenue`
  - `topDishes`
  - `peakHours`
  - `categoryRevenue`
  - `kitchenMetrics`
- `ReportingQueryService` already had typed getters for more report families than the UI consumed.
- `V100__final_polish_combo_and_typed_reporting.sql` already created projection tables for sales, best-selling items, revenue, kitchen bottlenecks, staff efficiency, inventory usage, and combo sales.

## Where The Old UI Drifted

- Dashboard duplicated the same operational meaning three times:
  - top summary cards
  - KPI list
  - metrics card grid
- Reports page reused live counters from operations, which made it feel like a second dashboard instead of an analytical surface.
- Frontend endpoint definitions only exposed `operations`, `peak-hours`, and `combo-sales`, so the UI ignored several backend report families.

## Recommended Ownership Split

### Dashboard should own

- Live table occupancy
- Open orders / ready-to-serve / kitchen queue
- Alerts and anomalies
- Active orders in service
- Intraday revenue trend
- Supporting quick stats such as reservations today, refunds today, average service time

### Reports should own

- Weekly revenue trend
- Revenue by category
- Peak demand by hour
- Best-selling dishes
- Kitchen bottleneck comparison
- Revenue mix by payment method
- Combo sales detail

## Reuse Points

- `frontend/src/components/ui/chart.tsx` is the standard chart wrapper. Reuse it before inventing report-specific chart chrome.
- `OperationsReportResponse` is the shared backbone between dashboard operational summary and reports snapshot-derived charts.
- Typed report rows are already normalized enough to feed tables directly without custom frontend reshaping beyond formatting.
