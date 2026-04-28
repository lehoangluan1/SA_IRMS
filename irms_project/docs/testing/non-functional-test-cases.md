# IRMS Non-Functional Test Cases

All cases map back to the Section 1 commitments summarized in [docs/section1-test-matrix.md](../section1-test-matrix.md).

## NF-01 Menu Lookup Baseline

- Test ID: `NF-01`
- Tên test: `Menu overview responds under 1 second`
- Nhóm NFR: `Performance / response time`
- Nguồn cam kết: `Section 1 -> Single-request latency`
- Mục tiêu/rủi ro được kiểm chứng: Verify menu browsing remains fast enough for order entry without blocking front-of-house service.
- Môi trường chạy: `Local smoke` and `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Logged-in manager token; seeded menu with active, inactive, seasonal items and combos.
- Công cụ đo / cách đo: `curl -w`, browser network panel, or Postman timing.
- Các bước thực hiện:
  1. Call `GET /api/menu`.
  2. Repeat 20 times with a fresh correlation ID.
  3. Capture min, median, p95, and max.
- Metric đo được: End-to-end response time in milliseconds.
- Threshold/pass criteria: `p95 <= 1000 ms`.
- Kết quả mong đợi: Response remains within threshold and payload contains categories, items, promotions, and combos.
- Bằng chứng cần thu thập: Timing table plus one representative JSON response.
- Tần suất chạy: `smoke`
- Owner gợi ý: `Backend developer`

## NF-02 Reservation Overview Baseline

- Test ID: `NF-02`
- Tên test: `Reservation overview under 1 second`
- Nhóm NFR: `Performance / response time`
- Nguồn cam kết: `Section 1 -> Single-request latency`
- Mục tiêu/rủi ro được kiểm chứng: Ensure host staff can refresh tables, reservations, and waitlist without lag.
- Môi trường chạy: `Local smoke`
- Dữ liệu đầu vào / seed / preconditions: Seed includes pending, confirmed, seated, completed, no-show, cancelled reservations and multiple waitlist states.
- Công cụ đo / cách đo: `curl -w` or Postman collection runner.
- Các bước thực hiện:
  1. Call `GET /api/reservations/overview` 20 times.
  2. Record timings.
  3. Validate response includes non-empty `tables`, `reservations`, and `waitlist`.
- Metric đo được: p95 response time.
- Threshold/pass criteria: `p95 <= 1000 ms`.
- Kết quả mong đợi: Host overview returns in under one second and contains the expanded seed states.
- Bằng chứng cần thu thập: Timing output and one saved response body.
- Tần suất chạy: `smoke`
- Owner gợi ý: `Reservation service owner`

## NF-03 Order Create Peak Baseline

- Test ID: `NF-03`
- Tên test: `Order create stays under 2 seconds in normal flow`
- Nhóm NFR: `Performance / response time`
- Nguồn cam kết: `Section 1 -> Single-request latency`
- Mục tiêu/rủi ro được kiểm chứng: Prevent slow order entry from delaying kitchen dispatch.
- Môi trường chạy: `Local smoke`
- Dữ liệu đầu vào / seed / preconditions: Active session ID and valid menu item IDs from seed.
- Công cụ đo / cách đo: Postman or scripted `curl` POST with timing capture.
- Các bước thực hiện:
  1. POST `/api/orders` with one regular item and one combo selection.
  2. Repeat 10 times against disposable test sessions.
  3. Record response time and correlation ID.
- Metric đo được: p95 create-order latency.
- Threshold/pass criteria: `p95 <= 2000 ms`.
- Kết quả mong đợi: Order creation remains below threshold and each response returns a correlation ID.
- Bằng chứng cần thu thập: Timing summary plus sample payload and response.
- Tần suất chạy: `smoke`
- Owner gợi ý: `Ordering service owner`

## NF-04 Dashboard Read Latency

- Test ID: `NF-04`
- Tên test: `Dashboard read remains under 1 second`
- Nhóm NFR: `Performance / response time`
- Nguồn cam kết: `Section 1 -> Single-request latency`
- Mục tiêu/rủi ro được kiểm chứng: Ensure operational dashboard remains usable during service.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Full stack running with refreshed operations snapshot.
- Công cụ đo / cách đo: Browser network panel or `curl -w`.
- Các bước thực hiện:
  1. Call `GET /api/dashboard` 20 times.
  2. Capture timings and verify alert + active order sections are populated.
- Metric đo được: p95 response time.
- Threshold/pass criteria: `p95 <= 1000 ms`.
- Kết quả mong đợi: Dashboard endpoint stays under threshold and returns live + snapshot fields.
- Bằng chứng cần thu thập: Timing output and one JSON payload showing `revenueTrend`.
- Tần suất chạy: `smoke`
- Owner gợi ý: `Reporting service owner`

## NF-05 Export Latency

- Test ID: `NF-05`
- Tên test: `Sales export completes within 3 seconds`
- Nhóm NFR: `Performance / export latency`
- Nguồn cam kết: `Section 1 -> Single-request latency`, `Section 1 -> Maintainability/extensibility`
- Mục tiêu/rủi ro được kiểm chứng: Ensure reporting export stays usable for demo and operational review.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Seeded sales projection rows present.
- Công cụ đo / cách đo: `curl -w -OJ` or browser download timing.
- Các bước thực hiện:
  1. Request `GET /api/reports/export?type=sales&format=csv`.
  2. Record time to first byte and total transfer time.
  3. Verify downloaded CSV has non-empty rows.
- Metric đo được: End-to-end export duration.
- Threshold/pass criteria: `<= 3000 ms`.
- Kết quả mong đợi: Export returns a file and completes within threshold.
- Bằng chứng cần thu thập: Saved CSV file and timing summary.
- Tần suất chạy: `release`
- Owner gợi ý: `Reporting service owner`

## NF-06 Order Burst Correlation Preservation

- Test ID: `NF-06`
- Tên test: `Burst order requests preserve correlation IDs`
- Nhóm NFR: `Observability / peak-load integrity`
- Nguồn cam kết: `Section 1 -> Scalability under peak load`, `Section 1 -> Observability`
- Mục tiêu/rủi ro được kiểm chứng: Under burst traffic, traceability must remain intact.
- Môi trường chạy: `Backend automated suite`
- Dữ liệu đầu vào / seed / preconditions: Existing `Section1NonFunctionalContractTest`.
- Công cụ đo / cách đo: Maven test execution.
- Các bước thực hiện:
  1. Run `mvnw test`.
  2. Inspect `burstOrderRequestsPreserveCorrelationIdsUnderPeakHourStyleLoad`.
- Metric đo được: Pass/fail plus count of successful correlation IDs.
- Threshold/pass criteria: `100% responses contain the expected correlation prefix`.
- Kết quả mong đợi: No burst request drops the header-to-envelope correlation chain.
- Bằng chứng cần thu thập: Test report and console output.
- Tần suất chạy: `nightly`
- Owner gợi ý: `Backend developer`

## NF-07 Waitlist Seat Concurrency

- Test ID: `NF-07`
- Tên test: `Concurrent waitlist seat attempts produce one success and explicit conflicts`
- Nhóm NFR: `Reliability / integrity`
- Nguồn cam kết: `Section 1 -> Reliability and integrity`
- Mục tiêu/rủi ro được kiểm chứng: Avoid silent double seating of the same party.
- Môi trường chạy: `Backend automated suite`
- Dữ liệu đầu vào / seed / preconditions: Existing waitlist test harness.
- Công cụ đo / cách đo: Maven test execution.
- Các bước thực hiện:
  1. Run `mvnw test`.
  2. Inspect `waitlistSeatingAllowsOneSuccessAndSurfacesConflictsDuringConcurrency`.
- Metric đo được: Success count and conflict count.
- Threshold/pass criteria: `Exactly 1x 200 and remaining attempts 409`.
- Kết quả mong đợi: Race is explicit and not hidden by duplicate success.
- Bằng chứng cần thu thập: Surefire result and test logs.
- Tần suất chạy: `nightly`
- Owner gợi ý: `Reservation service owner`

## NF-08 Refund Approval Concurrency

- Test ID: `NF-08`
- Tên test: `Concurrent refund decisions avoid duplicate success`
- Nhóm NFR: `Reliability / integrity`
- Nguồn cam kết: `Section 1 -> Reliability and integrity`
- Mục tiêu/rủi ro được kiểm chứng: Prevent duplicate approval paths for the same refund.
- Môi trường chạy: `Backend automated suite`
- Dữ liệu đầu vào / seed / preconditions: Existing refund concurrency contract test.
- Công cụ đo / cách đo: Maven test execution.
- Các bước thực hiện:
  1. Run `mvnw test`.
  2. Inspect `refundApprovalConcurrencyAvoidsSilentDuplicateSuccess`.
- Metric đo được: Success count and conflict count.
- Threshold/pass criteria: `Exactly 1x success; all other concurrent attempts fail cleanly`.
- Kết quả mong đợi: Duplicate approval is rejected with conflict semantics.
- Bằng chứng cần thu thập: Surefire output.
- Tần suất chạy: `nightly`
- Owner gợi ý: `Billing service owner`

## NF-09 Inventory Update Concurrency

- Test ID: `NF-09`
- Tên test: `Concurrent inventory updates surface conflicts`
- Nhóm NFR: `Reliability / integrity`
- Nguồn cam kết: `Section 1 -> Reliability and integrity`
- Mục tiêu/rủi ro được kiểm chứng: Avoid hidden last-write-wins on ingredient stock edits.
- Môi trường chạy: `Backend automated suite`
- Dữ liệu đầu vào / seed / preconditions: Existing inventory concurrency contract test.
- Công cụ đo / cách đo: Maven test execution.
- Các bước thực hiện:
  1. Run `mvnw test`.
  2. Inspect `inventoryUpdateConcurrencySurfacesConflictsInsteadOfHidingThem`.
- Metric đo được: Success count and conflict count.
- Threshold/pass criteria: `Exactly 1x success and all remaining attempts 409`.
- Kết quả mong đợi: Conflicting inventory edits are explicit.
- Bằng chứng cần thu thập: Surefire output.
- Tần suất chạy: `nightly`
- Owner gợi ý: `Inventory service owner`

## NF-10 Migration Re-run Safety

- Test ID: `NF-10`
- Tên test: `Flyway migrations remain repeat-safe in dev/demo`
- Nhóm NFR: `Availability / recoverability`
- Nguồn cam kết: `Section 1 -> Availability and recoverability`
- Mục tiêu/rủi ro được kiểm chứng: Prevent demo environments from failing when migrations are replayed on rebuilt stacks.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Clean or recreated compose database.
- Công cụ đo / cách đo: Docker logs, migration container status, Flyway history table.
- Các bước thực hiện:
  1. Bring the migration container up on a fresh database.
  2. Recreate the stack without deleting the volume.
  3. Verify Flyway reports no checksum issue and `V103` is present once.
- Metric đo được: Migration completion status and duration.
- Threshold/pass criteria: `Migration completes successfully on first run and remains clean on restart`.
- Kết quả mong đợi: No checksum drift, no duplicate object errors.
- Bằng chứng cần thu thập: Migration logs and `flyway_schema_history` query result.
- Tần suất chạy: `release`
- Owner gợi ý: `Platform / backend owner`

## NF-11 Health Recovery Window

- Test ID: `NF-11`
- Tên test: `Service health returns to UP within 10 minutes after container restart`
- Nhóm NFR: `Availability / recoverability`
- Nguồn cam kết: `Section 1 -> Availability and recoverability`
- Mục tiêu/rủi ro được kiểm chứng: Validate restart recovery stays inside Section 1 expectation.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Full stack healthy before test.
- Công cụ đo / cách đo: `docker compose restart`, health endpoint polling, timestamp logging.
- Các bước thực hiện:
  1. Restart one backend app container, preferably `api-gateway` or `reporting-service`.
  2. Poll `/api/health` every 15 seconds.
  3. Record time until the stack returns to healthy and routable.
- Metric đo được: Recovery duration.
- Threshold/pass criteria: `<= 10 minutes`.
- Kết quả mong đợi: Health returns to `UP` without manual DB repair.
- Bằng chứng cần thu thập: Restart timestamps, health responses, `docker compose ps`.
- Tần suất chạy: `release`
- Owner gợi ý: `Platform owner`

## NF-12 Protected Endpoint Boundary

- Test ID: `NF-12`
- Tên test: `Protected endpoints reject unauthenticated access`
- Nhóm NFR: `Security / permission boundary`
- Nguồn cam kết: `Section 1 -> Security and session control`
- Mục tiêu/rủi ro được kiểm chứng: Ensure sensitive routes are not publicly accessible.
- Môi trường chạy: `Local smoke`
- Dữ liệu đầu vào / seed / preconditions: No bearer token.
- Công cụ đo / cách đo: `curl -i` or Postman.
- Các bước thực hiện:
  1. Call `GET /api/dashboard`.
  2. Call `GET /api/reports/operations`.
  3. Call `POST /api/refunds/{id}/approval`.
- Metric đo được: HTTP status and error code.
- Threshold/pass criteria: `401 or 403 on every protected route`.
- Kết quả mong đợi: Standard error envelope and no sensitive data leakage.
- Bằng chứng cần thu thập: Response headers/bodies for each route.
- Tần suất chạy: `smoke`
- Owner gợi ý: `Security / backend owner`

## NF-13 Session Idle Timeout

- Test ID: `NF-13`
- Tên test: `Idle session expires at 15 minutes`
- Nhóm NFR: `Security / session control`
- Nguồn cam kết: `Section 1 -> Security and session control`
- Mục tiêu/rủi ro được kiểm chứng: Ensure idle sessions do not remain valid beyond the configured policy.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Valid login token; authorization policy seed sets `session_idle_timeout_minutes = 15`.
- Công cụ đo / cách đo: Login flow + timed polling of `/api/auth/me`.
- Các bước thực hiện:
  1. Log in and store the token.
  2. Stop sending authenticated traffic for 16 minutes.
  3. Call `/api/auth/me`.
- Metric đo được: Session validity duration.
- Threshold/pass criteria: Request after 15 minutes idle must be rejected.
- Kết quả mong đợi: Session becomes invalid and token cannot keep reading protected endpoints.
- Bằng chứng cần thu thập: Login timestamp, failed `/api/auth/me` response, user session DB row if inspected.
- Tần suất chạy: `release`
- Owner gợi ý: `Identity service owner`

## NF-14 Refund Audit Traceability

- Test ID: `NF-14`
- Tên test: `Refund workflow leaves an auditable trail`
- Nhóm NFR: `Auditability / traceability`
- Nguồn cam kết: `Section 1 -> Security and session control`, `Section 1 -> Observability`
- Mục tiêu/rủi ro được kiểm chứng: Sensitive actions must be attributable and reconstructable.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Pending or seeded refund exists.
- Công cụ đo / cách đo: API calls plus SQL query against `audit_logs` and `audit_details`.
- Các bước thực hiện:
  1. Approve or reject a refund.
  2. Query the resulting audit log using the correlation ID.
  3. Confirm actor, reason, entity, and before/after details are present.
- Metric đo được: Presence of trace rows and correlation linkage.
- Threshold/pass criteria: `1+ audit row and matching correlation ID required`.
- Kết quả mong đợi: Refund decision is fully traceable.
- Bằng chứng cần thu thập: API response, audit SQL output, correlation ID.
- Tần suất chạy: `release`
- Owner gợi ý: `Billing service owner`

## NF-15 Correlation Header Coverage

- Test ID: `NF-15`
- Tên test: `All tested API responses echo or generate correlation IDs`
- Nhóm NFR: `Observability`
- Nguồn cam kết: `Section 1 -> Observability`
- Mục tiêu/rủi ro được kiểm chứng: Requests must remain traceable through logs and client responses.
- Môi trường chạy: `Local smoke`
- Dữ liệu đầu vào / seed / preconditions: Any authenticated token.
- Công cụ đo / cách đo: `curl -i` with and without `X-Correlation-Id`.
- Các bước thực hiện:
  1. Send one request with explicit `X-Correlation-Id`.
  2. Send one request without it.
  3. Compare header and envelope values.
- Metric đo được: Presence and consistency of correlation ID.
- Threshold/pass criteria: `100% of tested responses include a correlation ID`.
- Kết quả mong đợi: Explicit IDs are echoed; missing IDs are generated.
- Bằng chứng cần thu thập: Raw headers and response body snippets.
- Tần suất chạy: `smoke`
- Owner gợi ý: `Backend platform owner`

## NF-16 Low-Stock Alert Latency

- Test ID: `NF-16`
- Tên test: `Low-stock alert appears within 60 seconds of threshold breach`
- Nhóm NFR: `Observability / alerting`
- Nguồn cam kết: `Section 1 -> Observability`
- Mục tiêu/rủi ro được kiểm chứng: Alerting must be timely enough for kitchen and inventory intervention.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Pick one ingredient with threshold near current on-hand.
- Công cụ đo / cách đo: API action to reduce stock plus polling `GET /api/inventory`.
- Các bước thực hiện:
  1. Perform an inventory or order action that drops an item below threshold.
  2. Poll inventory overview every 10 seconds.
  3. Stop when the alert appears.
- Metric đo được: Seconds from threshold breach to visible alert.
- Threshold/pass criteria: `<= 60 seconds`.
- Kết quả mong đợi: Alert is visible in inventory overview and notification flow starts.
- Bằng chứng cần thu thập: Start timestamp, first alert timestamp, relevant logs.
- Tần suất chạy: `release`
- Owner gợi ý: `Inventory service owner`

## NF-17 Reporting Freshness Lag

- Test ID: `NF-17`
- Tên test: `Reporting projection freshness stays within 5 minutes`
- Nhóm NFR: `Observability / eventual consistency`
- Nguồn cam kết: `Section 1 -> Observability`
- Mục tiêu/rủi ro được kiểm chứng: Reports must not drift too far behind transactional events during demo use.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Reporting service and worker processes healthy.
- Công cụ đo / cách đo: Create an order/payment event, then compare event time to `generatedAt` and projection row timestamps.
- Các bước thực hiện:
  1. Trigger a new order or payment.
  2. Poll `GET /api/reports/operations` and the relevant typed report.
  3. Record first time the new activity is reflected.
- Metric đo được: Lag between transactional event and report visibility.
- Threshold/pass criteria: `<= 5 minutes`.
- Kết quả mong đợi: Snapshot and typed projections converge inside the freshness window.
- Bằng chứng cần thu thập: Event timestamp, report timestamps, optional DB query.
- Tần suất chạy: `release`
- Owner gợi ý: `Reporting service owner`

## NF-18 Dashboard Poll Stability

- Test ID: `NF-18`
- Tên test: `Dashboard survives continuous polling for 30 minutes`
- Nhóm NFR: `Reliability / stability`
- Nguồn cam kết: `Section 1 -> Scalability under peak load`
- Mục tiêu/rủi ro được kiểm chứng: Prevent dashboard memory leaks or intermittent failures during demos.
- Môi trường chạy: `Docker compose demo`
- Dữ liệu đầu vào / seed / preconditions: Full stack healthy.
- Công cụ đo / cách đo: Browser tab or script polling `/api/dashboard` every 5 seconds.
- Các bước thực hiện:
  1. Run continuous polling for 30 minutes.
  2. Track failures, median latency, and browser console errors.
- Metric đo được: Error count and p95 latency.
- Threshold/pass criteria: `0 failed polls` and `p95 <= 1000 ms`.
- Kết quả mong đợi: Dashboard remains stable without degraded responses.
- Bằng chứng cần thu thập: Poll log, browser console export, optional server logs.
- Tần suất chạy: `nightly`
- Owner gợi ý: `Frontend + reporting owners`

## NF-19 Frontend Chart Render Stability

- Test ID: `NF-19`
- Tên test: `Dashboard and reports charts render without overflow or crash`
- Nhóm NFR: `Usability / chart stability`
- Nguồn cam kết: `Section 1 -> Maintainability/extensibility`
- Mục tiêu/rủi ro được kiểm chứng: Ensure restored charts remain stable with dense projection rows.
- Môi trường chạy: `Local smoke`
- Dữ liệu đầu vào / seed / preconditions: New dashboard/report datasets from V103.
- Công cụ đo / cách đo: Browser visual inspection plus console error scan.
- Các bước thực hiện:
  1. Open dashboard and reports.
  2. Verify line, bar, pie, and table sections render.
  3. Inspect console for uncaught errors and layout overflow.
- Metric đo được: JS error count and visible layout defects.
- Threshold/pass criteria: `0 uncaught chart errors`, `0 clipped critical labels`.
- Kết quả mong đợi: Charts and legends render successfully on seeded data.
- Bằng chứng cần thu thập: Screenshots and console log capture.
- Tần suất chạy: `smoke`
- Owner gợi ý: `Frontend owner`

## NF-20 Responsive Viewport Readability

- Test ID: `NF-20`
- Tên test: `Dashboard and reports remain usable at mobile and desktop widths`
- Nhóm NFR: `Usability / responsiveness`
- Nguồn cam kết: `Section 1 -> Maintainability/extensibility`
- Mục tiêu/rủi ro được kiểm chứng: Prevent restored charts and tables from becoming unreadable on common demo viewports.
- Môi trường chạy: `Browser-based smoke`
- Dữ liệu đầu vào / seed / preconditions: Seeded dashboard/report data present.
- Công cụ đo / cách đo: Chromium responsive mode.
- Các bước thực hiện:
  1. Test at `390x844`, `768x1024`, and `1440x900`.
  2. Verify cards stack correctly, charts remain visible, and tables stay horizontally scrollable when needed.
- Metric đo được: Count of blocking layout defects.
- Threshold/pass criteria: `0 blocking defects` across the three viewports.
- Kết quả mong đợi: No impossible tap target, unreadable chart, or broken overflow.
- Bằng chứng cần thu thập: One screenshot per viewport per page.
- Tần suất chạy: `release`
- Owner gợi ý: `Frontend owner`

## NF-21 Keyboard Accessibility Smoke

- Test ID: `NF-21`
- Tên test: `Primary dashboard and reports actions are keyboard reachable`
- Nhóm NFR: `Accessibility`
- Nguồn cam kết: `Section 1 -> Maintainability/extensibility`
- Mục tiêu/rủi ro được kiểm chứng: Ensure major interactions are not mouse-only.
- Môi trường chạy: `Browser-based smoke`
- Dữ liệu đầu vào / seed / preconditions: Logged-in session.
- Công cụ đo / cách đo: Manual tab navigation with visible focus state.
- Các bước thực hiện:
  1. Tab through navigation, export actions, and page controls.
  2. Verify focus order is sensible and focus ring is visible.
  3. Trigger one export action using keyboard only.
- Metric đo được: Count of inaccessible primary actions.
- Threshold/pass criteria: `0 inaccessible primary actions`.
- Kết quả mong đợi: Dashboard and reports can be traversed without a mouse.
- Bằng chứng cần thu thập: Screen recording or screenshots of focus states.
- Tần suất chạy: `release`
- Owner gợi ý: `Frontend owner`

## NF-22 Graceful Degradation On Empty Reports

- Test ID: `NF-22`
- Tên test: `Reports page degrades gracefully when a report returns zero rows`
- Nhóm NFR: `Error tolerance / graceful degradation`
- Nguồn cam kết: `Section 1 -> Maintainability/extensibility`
- Mục tiêu/rủi ro được kiểm chứng: Avoid blank or crashing analytics screens when one projection family is temporarily empty.
- Môi trường chạy: `Local smoke`
- Dữ liệu đầu vào / seed / preconditions: Temporarily stub or clear one typed report response in a local test environment.
- Công cụ đo / cách đo: Browser visual check and console inspection.
- Các bước thực hiện:
  1. Force one report endpoint, such as combo sales, to return an empty `rows` array.
  2. Load the reports page.
  3. Confirm the empty panel renders while the rest of the page remains usable.
- Metric đo được: JS error count and count of broken sections.
- Threshold/pass criteria: `0 JS crashes`, `only the empty section degrades`.
- Kết quả mong đợi: Empty-state messaging appears and other charts/tables still render.
- Bằng chứng cần thu thập: Screenshot and console output.
- Tần suất chạy: `release`
- Owner gợi ý: `Frontend owner`
