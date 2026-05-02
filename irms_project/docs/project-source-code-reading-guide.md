# Project Source Code Reading Guide

## 1. Mục tiêu tài liệu

Tài liệu này giúp nhóm đọc source code IRMS trước meeting theo hướng nhanh, đúng trọng tâm và bám vào code thật. Phạm vi chính là source trong `irms_project`, đặc biệt là backend Spring Boot service-based runtime, gateway routing, remote calling, event-driven notification, database/resources/seed data, Docker setup và test coverage.

Ghi chú quan trọng: trong code không có file tên chính xác `IRMSApp`. Entry tương ứng hiện tại là `irms_project/backend/src/main/java/SA/irms/IrmsApplication.java`, đóng vai trò compatibility launcher để chọn runtime service bằng `IRMS_RUNTIME_MODE` hoặc `IRMS_RUNTIME_SERVICE`.

## 2. Project Structure Overview

### Các folder chính

| Folder/File | Vai trò | Mức độ cần đọc |
|---|---|---|
| `irms_project/README.md` | Mô tả cách chạy local/Docker, danh sách runtime service, URL mặc định, test workflow | Bắt buộc |
| `irms_project/docker-compose.yml` | Compose chính: Postgres, RabbitMQ, migration, API gateway, các service runtime và frontend | Bắt buộc |
| `irms_project/backend/` | Backend Java 17 Spring Boot, chạy nhiều runtime mode từ cùng codebase | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/IrmsApplication.java` | Launcher chọn runtime theo env/system property | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/runtime/` | Entry point riêng cho từng service: ordering, kitchen, billing, reservation, notification, reporting, inventory, identity, migration | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/gateway/` | API gateway Spring runtime, route locator, proxy controller, HTTP timeout config | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/common/` | Shared config, security, remote client config, outbox, RabbitMQ, events, web/error handling | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/identity/` | Auth, session, RBAC/staff/settings/audit/event operations | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/ordering/` | Order/menu/combo/promotion flow, order lifecycle, integration với kitchen | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/kitchen/` | Kitchen tickets, item status, station queue, automation, remote sync order/inventory | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/reservation/` | Reservation, tables, waitlist, seating, reservation notification | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/notification/` | Notification inbox/queue, RabbitMQ consumers, event mappers, channel adapters | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/billing/` | Bills, splits, payments, refunds, receipts, payment gateway, promotion remote client | Bắt buộc |
| `irms_project/backend/src/main/java/SA/irms/inventory/` | Inventory/low-stock flow; không thuộc 6 module chính nhưng liên quan kitchen/notification | Nên đọc |
| `irms_project/backend/src/main/resources/` | Application properties, per-service profiles, Flyway migrations, seed data | Bắt buộc |
| `irms_project/backend/src/test/java/SA/irms/` | Contract tests, architecture tests, non-functional tests | Bắt buộc |
| `irms_project/frontend/` | React/TanStack frontend, Nitro proxy `/api/**` tới gateway trong container | Nên đọc |
| `irms_project/docs/section1-test-matrix.md` | Matrix functional/non-functional coverage đang được test | Nên đọc |
| `docs/adr/` | ADR kiến trúc: service-based, gateway, RabbitMQ/outbox, Docker, RBAC | Optional |

### Nhận xét nhanh

- Backend không chạy monolith mặc định. `IrmsApplication` sẽ throw lỗi nếu không set runtime hợp lệ.
- Mỗi service runtime scan package riêng, ví dụ `OrderingServiceApplication` scan `SA.irms.ordering` và `SA.irms.common`; gateway cố ý không scan toàn bộ `SA.irms.common`.
- Database là shared PostgreSQL, schema/seed được quản lý bằng Flyway trong migration runtime.
- Docker Compose là cách chạy đúng nhất hiện tại: các service dùng service DNS như `ordering-service:8081`, `rabbitmq`, `postgres`.
- Local config có nhiều fallback (`localhost`, `postgres/123456`, RabbitMQ disabled), nên dễ lệch Docker nếu chạy thủ công.
- Event-driven flow dùng transactional outbox + RabbitMQ + manual ack + inbox deduplication + retry/DLQ.

## 3. Tổng quan kiến trúc

### Dự án này dùng để làm gì?

IRMS là hệ thống quản lý nhà hàng: đăng nhập/phiên làm việc, đặt bàn/waitlist, order/menu/combo/promotion, kitchen ticket, billing/payment/refund/receipt, inventory/low-stock, notification inbox, reporting/dashboard và audit.

### Kiến trúc tổng quan

- `frontend/`: React/TanStack app. Production container chạy Nitro server và proxy `/api/**` tới API gateway bằng `NITRO_API_PROXY_TARGET`.
- `backend/`: một Spring Boot codebase nhưng tách runtime theo service mode.
- `api-gateway`: nhận UI API `/api/**`, route sang service tương ứng bằng REST proxy.
- Domain services: `ordering-service`, `kitchen-service`, `billing-service`, `reservation-service`, `inventory-service`, `notification-service`, `reporting-service`, `identity-audit-service`.
- Remote calling: dùng `RestTemplate` trong `SA.irms.common.remote.RestClientConfiguration`, có connect timeout 3s, read timeout 8s và header internal service token.
- Async/event-driven: dùng `common/outbox`, `common/messaging`, `common/events`; RabbitMQ topology được tạo khi `irms.rabbitmq.enabled=true`.
- Database/config: `application.properties`, `application-docker.properties`, `application-local.properties`, các `application-*-service.properties`, `service-topology.yml`, Flyway SQL trong `db/migration`.

### Luồng chạy tổng quát

1. Docker Compose start `postgres` và `rabbitmq`.
2. `irms-migration` chạy `SPRING_PROFILES_ACTIVE=docker,migration`, bật Flyway và apply schema/seed trong `backend/src/main/resources/db/migration`.
3. Các service runtime start với profile riêng, ví dụ `docker,ordering-service`.
4. Mỗi runtime entry trong `SA.irms.runtime.*` set thêm profile service và scan package module + `SA.irms.common`.
5. `api-gateway` start port `8080`, đọc route endpoint từ `application-api-gateway.properties` hoặc env Docker.
6. Frontend container proxy `/api/**` tới `http://api-gateway:8080`.
7. Request UI đi qua gateway, gateway forward tới service theo route table.
8. Service xử lý DB, remote call nội bộ nếu cần, và publish event qua outbox/RabbitMQ cho notification/reporting/audit/follow-up.

## 4. Entry Point và Runtime Flow

### Entry point chính

| File | Vai trò |
|---|---|
| `backend/src/main/java/SA/irms/IrmsApplication.java` | Compatibility launcher. Đọc `IRMS_RUNTIME_MODE`, `IRMS_RUNTIME_SERVICE`, `irms.runtime.mode`, `irms.runtime.service`; route sang main class của service. |
| `backend/src/main/java/SA/irms/runtime/ordering/OrderingServiceApplication.java` | Entry ordering, scan `SA.irms.ordering`, `SA.irms.common`, bật scheduling. |
| `backend/src/main/java/SA/irms/runtime/kitchen/KitchenServiceApplication.java` | Entry kitchen, scan `SA.irms.kitchen`, `SA.irms.common`, bật scheduling. |
| `backend/src/main/java/SA/irms/runtime/billing/BillingServiceApplication.java` | Entry billing, scan `SA.irms.billing`, `SA.irms.common`, `SA.irms.adapters.pdf`, bật scheduling. |
| `backend/src/main/java/SA/irms/runtime/reservation/ReservationServiceApplication.java` | Entry reservation, scan `SA.irms.reservation`, `SA.irms.common`, bật scheduling. |
| `backend/src/main/java/SA/irms/runtime/notification/NotificationServiceApplication.java` | Entry notification, scan `SA.irms.notification`, `SA.irms.common`, bật scheduling. |
| `backend/src/main/java/SA/irms/runtime/identity/IdentityAuditServiceApplication.java` | Entry identity/audit, scan `SA.irms.identity`, `SA.irms.common`, bật scheduling. |
| `backend/src/main/java/SA/irms/runtime/migration/MigrationApplication.java` | Entry migration, web app type `NONE`, scan `SA.irms.common`, chạy xong thì exit. |
| `backend/src/main/java/SA/irms/gateway/ApiGatewayApplication.java` | Entry gateway, exclude datasource/Flyway, scan gateway + common config/error/identity/remote/security/web. |

### Runtime flow ở mức cao

1. Container backend chạy jar với main class `SA.irms.IrmsApplication`.
2. `IrmsApplication` đọc env runtime và dispatch sang service application tương ứng.
3. Spring load `application.properties`, profile `docker/local`, profile service cụ thể.
4. Migration runtime bật Flyway; service runtime khác có `spring.flyway.enabled=false`.
5. Service runtime đăng ký controller/application/persistence/integration của module.
6. `common.security` bảo vệ API bằng token/session/permission; `api/auth/login` là public theo security config.
7. Nếu RabbitMQ enabled, `RabbitMqTopologyConfig` tạo exchanges/queues/retry/DLQ và listener manual ack.
8. Gateway forward request; domain services xử lý DB, publish outbox event hoặc gọi service khác bằng internal REST.

### Những điểm team cần kiểm tra

- `application.properties` fallback runtime về `ordering-service`; nếu quên env khi chạy jar có thể chạy sai service.
- `GatewayRoutesProperties` và `ServiceEndpointProperties` fallback về `localhost:8081..8088`; trong Docker phải được override bằng env service DNS.
- `application-local.properties` mặc định RabbitMQ disabled, còn Docker services bật RabbitMQ. Flow event-driven local có thể khác Docker.
- Compose dùng RabbitMQ user mặc định `${RABBITMQ_USERNAME:-postgre}` nhưng `application-docker.properties` fallback là `guest`; Docker env đang override nên ổn, nhưng chạy thủ công dễ lệch.
- Gateway không có retry/circuit breaker, chỉ timeout và map `ResourceAccessException` thành 502.

## 5. Gateway Routing và Service Communication

### Gateway routing

| File | Vai trò | Route/service liên quan |
|---|---|---|
| `gateway/ApiGatewayApplication.java` | Gateway runtime, không dùng datasource/Flyway | API gateway port 8080 |
| `gateway/GatewayRouteLocator.java` | Route table chính | `/api/orders`, `/api/menu` -> ordering; `/api/kitchen` -> kitchen; `/api/billing`, `/api/bills`, `/api/payments`, `/api/refunds` -> billing; `/api/reservations`, `/api/tables`, `/api/waitlist` -> reservation; `/api/inventory` -> inventory; `/api/audit`, `/api/auth`, `/api/settings`, `/api/staff`, `/api/shifts` -> identity; `/api/dashboard`, `/api/reports` -> reporting |
| `gateway/GatewayRouteLocator.java` | Special route | `POST /api/notifications` -> reservation-service; các `/api/notifications` khác -> notification-service |
| `gateway/GatewayProxyController.java` | Proxy `/api/**`, copy headers/body/query, filter hop-by-hop headers | Tất cả UI API |
| `gateway/GatewayHttpClientConfiguration.java` | Gateway HTTP client | connect timeout 3s, read timeout 20s |
| `resources/application-api-gateway.properties` | Gateway endpoint config | `ORDERING_SERVICE_URL`, `KITCHEN_SERVICE_URL`, ... |
| `frontend/nitro.config.ts` | Frontend production proxy | `/api/**` -> `${NITRO_API_PROXY_TARGET}/api/**` |

### Service communication

Service communication có 2 kiểu chính:

- Synchronous REST: gateway -> service; service -> service qua `RestTemplate` nội bộ.
- Durable async: service publish event vào outbox, relay sang RabbitMQ, consumer xử lý bằng manual ack và inbox deduplication.

### Remote calling

| Caller | Callee | File liên quan | Ghi chú |
|---|---|---|---|
| Gateway | Tất cả domain service | `GatewayProxyController`, `GatewayRouteLocator` | Proxy public `/api/**`, timeout 20s, 502 nếu upstream không reachable |
| Ordering | Kitchen | `ordering/integration/RemoteKitchenOrderRoutingClient.java` | Gửi item confirmed/delayed/hold/block vào `/internal/kitchen/**` |
| Kitchen | Ordering | `kitchen/integration/RemoteOrderStateUpdateClient.java` | Refresh order và sync line status vào `/internal/ordering/**` |
| Kitchen | Inventory | `kitchen/integration/RemoteInventoryConsumptionClient.java` | Deduct stock khi kitchen start qua `/internal/inventory/consumption/kitchen-start` |
| Billing | Ordering | `billing/integration/RemotePromotionApplicationClient.java` | Apply promotion qua `/internal/ordering/promotions/apply` |
| Các service khác identity | Identity/audit | `common/identity/RemoteSharedIdentitySessionClient.java`, `RemoteSharedIdentityPolicyClient.java`, `RemoteSharedIdentityDirectoryClient.java` | Session introspection, touch session, policy snapshot, display names, role-priority lookup |

### Điểm cần review thêm

- Internal remote clients chưa có retry/circuit breaker riêng.
- Một số remote client không catch exception, nên remote failure có thể bubble lên business flow.
- Identity session lookup catch `RestClientException` và trả `Optional.empty()`, cần review xem có che lỗi identity outage không.
- Gateway route `POST /api/notifications` sang reservation-service là quyết định đặc biệt, cần team xác nhận có đúng API ownership không.
- Cần bổ sung test gateway route/proxy timeout/upstream failure nếu chưa có.

## 6. Resources, Database Config và Seed Data

### Resources

| File/Folder | Vai trò |
|---|---|
| `backend/src/main/resources/application.properties` | Config gốc: datasource, Flyway default false, CORS, security token/session, mail, outbox/RabbitMQ defaults |
| `application-docker.properties` | Docker profile: DB host `postgres`, RabbitMQ host `rabbitmq`, RabbitMQ enabled true, Flyway theo env |
| `application-local.properties` | Local profile: DB localhost, Flyway true, RabbitMQ disabled mặc định |
| `application-api-gateway.properties` | Gateway route endpoints và gateway runtime mode |
| `application-*-service.properties` | Port, runtime mode, service name, remote endpoints, RabbitMQ enabled cho từng service |
| `application-migration.properties` | Migration profile. Cần kiểm tra thêm nếu muốn biết toàn bộ override migration. |
| `service-topology.yml` | Tài liệu topology runtime: ports, ownership, synchronous interactions, durable async |
| `db/migration/V1__create_schema.sql` | Schema chính: identity, reservation, ordering, kitchen, billing, inventory, notification, reporting/outbox tables |
| `db/migration/V2__seed_demo_data.sql` | Seed demo ban đầu: roles/users, tables, reservations, menu, inventory, orders, kitchen, bills... |
| `db/migration/V103__expand_demo_seed_data.sql` | Seed mở rộng demo dashboard/reporting/order/billing |
| `db/migration/V104__enrich_ordering_demo_data.sql` | Seed bổ sung cho ordering demo |
| `db/migration/V11..V17` | Hardening outbox/inbox/idempotency/RabbitMQ pipeline |
| `docker/postgres/initdb/01-sync-dev-roles.sh` | Init script Postgres container |

### Database config

Database dùng PostgreSQL qua Spring JDBC. Config gốc:

- URL: `JDBC_DATABASE_URL` hoặc `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`.
- Username: `DB_USER` hoặc `DB_USERNAME`.
- Password: `DB_PASSWORD`, fallback `123456`.
- Flyway migration nằm trong `classpath:db/migration`.

Trong Docker, chỉ `irms-migration` bật `IRMS_FLYWAY_ENABLED=true`; các service domain tắt Flyway và phụ thuộc migration completed successfully.

### Seed data

Seed nằm trong Flyway migration, đặc biệt:

- `V2__seed_demo_data.sql`: branch, role/permission, demo users, policy, tables, reservations, waitlist, menu, inventory, orders, kitchen tickets, billing.
- `V103__expand_demo_seed_data.sql`: mở rộng demo table/session/order/kitchen/billing/reporting.
- `V104__enrich_ordering_demo_data.sql`: bổ sung dữ liệu ordering.

Seed được load khi migration runtime chạy Flyway. Không thấy cơ chế seed riêng ngoài Flyway.

### Lưu ý khi chạy project

- Nên chạy bằng Docker Compose từ `irms_project`.
- Không ưu tiên setup local thủ công nếu chưa hiểu config.
- Local mode mặc định RabbitMQ disabled; Docker mode bật RabbitMQ cho services.
- Gateway/remote endpoint fallback localhost nên local port conflict hoặc thiếu service sẽ dễ fail.

## 7. Các module bắt buộc cần đọc

### 7.1 Identity

#### Vai trò chính

Identity chịu trách nhiệm auth/login/logout/current user, session, RBAC permission, staff/shift/settings, audit log và event operations/replay/dead-letter view.

#### Các file/folder quan trọng cần đọc

| File/Folder | Vai trò | Mức độ ưu tiên |
|---|---|---|
| `runtime/identity/IdentityAuditServiceApplication.java` | Entry runtime identity/audit | Bắt buộc |
| `identity/api/AuthController.java` | `/api/auth/login`, `/logout`, `/me` | Bắt buộc |
| `identity/api/IdentityInternalController.java` | Internal identity endpoints cho service khác | Bắt buộc |
| `identity/api/StaffController.java` | `/api/staff`, `/api/shifts`, role update | Nên đọc |
| `identity/api/AuditController.java` | Audit log/export/follow-up | Nên đọc |
| `identity/api/EventOpsController.java` | Failed outbox/dead-letter/replay endpoints | Nên đọc |
| `identity/application/AuthService.java` | Login/session business flow | Bắt buộc |
| `identity/audit/`, `identity/infrastructure/messaging/` | Audit materialization/session touch consumers | Nên đọc |
| `common/security/` | Token, current user, permission guard, internal token | Bắt buộc |

#### Luồng xử lý chính

1. User login qua `AuthController.login`.
2. `AuthService` xác thực user, tạo token/session và trả `LoginResponse`.
3. Các endpoint protected dùng `CurrentUser` và `PermissionGuard`.
4. Service khác gọi identity qua internal REST client trong `common/identity`.
5. Audit/session touch có thể đi qua outbox/RabbitMQ consumer trong identity runtime.

#### Module này liên quan đến module nào khác?

| Module liên quan | Kiểu liên kết | File liên quan | Ghi chú |
|---|---|---|---|
| Tất cả service | Remote call | `common/identity/RemoteSharedIdentity*.java` | Session/policy/display-name lookup |
| Gateway | Routing | `GatewayRouteLocator.java` | `/api/auth`, `/api/audit`, `/api/settings`, `/api/staff`, `/api/shifts` |
| Notification | Event | `AuditFollowUpRequestedConsumer.java` | Audit follow-up có thể tạo notification |
| Common messaging | Event ops | `EventOpsController.java` | Replay failed outbox/dead-letter |

#### Test coverage hiện tại

| Test file/folder | Đang cover gì | Nhận xét |
|---|---|---|
| `identity/api/IdentityApiContractTest.java` | login/logout/me, staff/shift/role, settings, audit, forbidden/conflict error shape | Có contract test tốt ở controller level |
| `IrmsApplicationTests.java` | main launcher và gateway scan package | Cover entry/gateway scan một phần |
| `nonfunctional/Section1NonFunctionalContractTest.java` | session/permission/correlation checks | Có non-functional smoke |

#### Functional gaps cần kiểm tra

- Role/permission matrix có đủ cho toàn bộ UI flow chưa.
- Token/session expiration/revoke có match yêu cầu thực tế chưa.
- Audit follow-up/replay flow có UI/ops ownership rõ chưa.

#### Non-functional gaps cần kiểm tra

- Security secret đang có fallback dev trong config, production cần bắt buộc override.
- Remote identity outage đang bị một số client swallow thành empty result.
- Cần kiểm tra audit idempotency và replay permission.

#### Bugs/Risks có thể có

- High priority: nếu `IRMS_ACCESS_TOKEN_SIGNING_SECRET` và `IRMS_INTERNAL_SERVICE_TOKEN` không override, hệ thống dùng default dev secret/token.
- Local/Docker RabbitMQ khác nhau có thể làm audit/session touch behavior khác.

#### Câu hỏi cần trả lời khi review module này

- Module này nhận input từ đâu?
- Output đi đâu?
- Có gọi service khác không?
- Có được gateway route tới không?
- Có test chưa?
- Có thiếu validation không?
- Có xử lý lỗi chưa?
- Có case nào dễ fail khi chạy Docker/local không?

### 7.2 Kitchen

#### Vai trò chính

Kitchen quản lý ticket, station queue, trạng thái item/ticket, priority, serve/return/cancel, automation và sync trạng thái về ordering/inventory/notification.

#### Các file/folder quan trọng cần đọc

| File/Folder | Vai trò | Mức độ ưu tiên |
|---|---|---|
| `runtime/kitchen/KitchenServiceApplication.java` | Entry runtime kitchen | Bắt buộc |
| `kitchen/api/KitchenController.java` | Public API `/api/kitchen/**` | Bắt buộc |
| `kitchen/api/KitchenIntegrationController.java` | Internal API nhận request từ ordering | Bắt buộc |
| `kitchen/application/KitchenService.java` | Facade orchestration | Bắt buộc |
| `KitchenOrderRoutingService.java` | Tạo ticket từ order | Bắt buộc |
| `KitchenTicketItemWorkflowService.java`, `KitchenTicketHandoffService.java` | Status cooking/ready/blocked/served/returned | Bắt buộc |
| `KitchenTicketEventNotifier.java` | Publish dish status và notification command | Bắt buộc |
| `kitchen/integration/RemoteOrderStateUpdateClient.java` | Sync line status về ordering | Bắt buộc |
| `kitchen/integration/RemoteInventoryConsumptionClient.java` | Deduct inventory khi kitchen start | Nên đọc |
| `kitchen/application/workflow/` | Event consumers/steps cho cancellation/dish workflow | Nên đọc |

#### Luồng xử lý chính

1. Ordering gọi `/internal/kitchen/orders/{orderId}/tickets`.
2. `KitchenOrderRoutingService` tạo kitchen ticket/item theo station.
3. Staff thao tác `/api/kitchen/**` để update status/priority/serve/return/cancel.
4. `KitchenTicketEventNotifier` publish `KitchenDishStatusChanged` và notification command.
5. Kitchen sync line status về ordering và có flow inventory consumption khi start cooking.

#### Module này liên quan đến module nào khác?

| Module liên quan | Kiểu liên kết | File liên quan | Ghi chú |
|---|---|---|---|
| Ordering | Remote call/Event | `RemoteOrderStateUpdateClient`, `KitchenIntegrationController` | Nhận ticket request, sync status |
| Inventory | Remote call/Event | `RemoteInventoryConsumptionClient`, `KitchenInventoryUsageStep` | Deduct stock/stock changed |
| Notification | Event | `KitchenTicketEventNotifier`, `KitchenReadyNotificationStep` | Dish ready/status notification |
| Gateway | Routing | `GatewayRouteLocator.java` | `/api/kitchen` |

#### Test coverage hiện tại

| Test file/folder | Đang cover gì | Nhận xét |
|---|---|---|
| `operations/api/BillingInventoryKitchenApiContractTest.java` | Kitchen overview và ticket operations public contract | Có cover controller happy path |
| `nonfunctional/Section1NonFunctionalContractTest.java` | Một số concurrency/correlation liên quan operations | Chưa thay thế được integration test RabbitMQ/remote |

#### Functional gaps cần kiểm tra

- Status transition kitchen có đủ invalid transition cases chưa.
- Cancel/return/served có sync về ordering nhất quán không.
- Inventory deduction khi start cooking có rollback/compensation nếu remote fail không.

#### Non-functional gaps cần kiểm tra

- Remote call kitchen -> ordering/inventory chưa có retry/circuit breaker riêng.
- Automation scheduler cần kiểm tra idempotency khi nhiều instance hoặc restart.
- Event duplicate cần phụ thuộc inbox deduplication ở consumer, nhưng producer side cần kiểm tra idempotency business.

#### Bugs/Risks có thể có

- High priority: remote inventory/order sync fail có thể làm trạng thái giữa kitchen-ordering-inventory lệch nếu không có follow-up/retry nghiệp vụ.
- Cần kiểm tra Docker RabbitMQ enabled với local disabled có làm workflow khác nhau không.

#### Câu hỏi cần trả lời khi review module này

- Module này nhận input từ đâu?
- Output đi đâu?
- Có gọi service khác không?
- Có được gateway route tới không?
- Có test chưa?
- Có thiếu validation không?
- Có xử lý lỗi chưa?
- Có case nào dễ fail khi chạy Docker/local không?

### 7.3 Reservation

#### Vai trò chính

Reservation quản lý đặt bàn, recommendation, table assignment/status, check-in/no-show, waitlist, seating và notification request cho reservation/waitlist.

#### Các file/folder quan trọng cần đọc

| File/Folder | Vai trò | Mức độ ưu tiên |
|---|---|---|
| `runtime/reservation/ReservationServiceApplication.java` | Entry runtime reservation | Bắt buộc |
| `reservation/api/ReservationController.java` | `/api/reservations/**` | Bắt buộc |
| `reservation/api/TableController.java` | `/api/tables/**` | Bắt buộc |
| `reservation/api/WaitlistController.java` | `/api/waitlist/**` | Bắt buộc |
| `reservation/api/ReservationNotificationController.java` | `POST /api/notifications`, route đặc biệt từ gateway | Bắt buộc |
| `ReservationService.java` | Facade flow reservation | Bắt buộc |
| `ReservationCheckInService.java`, `ReservationTableAssignmentService.java` | Check-in/table assignment | Bắt buộc |
| `ReservationWaitlistService.java`, `ReservationWaitlistSeatingService.java` | Waitlist lifecycle/seating | Bắt buộc |
| `ReservationNotificationCoordinator.java` | Publish notification command | Nên đọc |
| `reservation/application/workflow/` | Reservation seated/waitlist notification event steps | Nên đọc |

#### Luồng xử lý chính

1. User tạo/đổi/confirm reservation qua `/api/reservations`.
2. Check-in có thể assign table, mở table session và publish `ReservationSeated`.
3. Waitlist flow create/notify/skip/prioritize/seat qua `WaitlistController`.
4. Notification cho reservation/waitlist được publish qua `NotificationCommandPublisher`.

#### Module này liên quan đến module nào khác?

| Module liên quan | Kiểu liên kết | File liên quan | Ghi chú |
|---|---|---|---|
| Notification | Event | `ReservationNotificationCoordinator`, `RequestReservationNotificationStep` | Reservation/waitlist notifications |
| Reporting/Audit | Event | `ReservationCreatedEvent`, `ReservationSeatedEvent`, `WaitlistUpdatedEvent` | Consumed qua RabbitMQ |
| Gateway | Routing | `GatewayRouteLocator.java` | `/api/reservations`, `/api/tables`, `/api/waitlist`, special `POST /api/notifications` |
| Identity | Remote call | common identity clients | Permission/user context |

#### Test coverage hiện tại

| Test file/folder | Đang cover gì | Nhận xét |
|---|---|---|
| `reservation/api/ReservationApiContractTest.java` | overview, recommendations, create/update/confirm/check-in/no-show, tables/waitlist/notifications, validation/conflict | Có coverage controller tốt |
| `nonfunctional/Section1NonFunctionalContractTest.java` | waitlist seating concurrency conflict | Có concurrency smoke |

#### Functional gaps cần kiểm tra

- Reservation cancel flow public API có đủ chưa; controller có no-show/check-in/update/confirm nhưng cần kiểm tra cancel requirement.
- Table release/cleanup sau completed/no-show/cancel có nhất quán không.
- Waitlist expiration/notify/seat edge cases cần đọc thêm scheduler/service.

#### Non-functional gaps cần kiểm tra

- Transaction boundary của check-in/seating/table session cần review kỹ.
- Notification failure có ảnh hưởng reservation flow không.
- Validation date/time/party/table capacity cần kiểm tra đủ invalid cases.

#### Bugs/Risks có thể có

- Special route `POST /api/notifications` thuộc reservation có thể gây nhầm ownership với notification inbox.
- Local RabbitMQ disabled làm notification event flow không giống Docker.

#### Câu hỏi cần trả lời khi review module này

- Module này nhận input từ đâu?
- Output đi đâu?
- Có gọi service khác không?
- Có được gateway route tới không?
- Có test chưa?
- Có thiếu validation không?
- Có xử lý lỗi chưa?
- Có case nào dễ fail khi chạy Docker/local không?

### 7.4 Notification

#### Vai trò chính

Notification quản lý inbox người dùng, nhận event từ RabbitMQ, map event thành `NotificationCommand`, validate channel và ghi `notification_messages`/`notification_deliveries`.

#### Các file/folder quan trọng cần đọc

| File/Folder | Vai trò | Mức độ ưu tiên |
|---|---|---|
| `runtime/notification/NotificationServiceApplication.java` | Entry runtime notification | Bắt buộc |
| `notification/api/NotificationController.java` | `/api/notifications` list/read | Bắt buộc |
| `notification/infrastructure/messaging/NotificationRequestedConsumer.java` | Rabbit listeners cho notification-related queues | Bắt buộc |
| `notification/infrastructure/messaging/AuditFollowUpRequestedConsumer.java` | Audit follow-up -> notification | Bắt buộc |
| `NotificationRequestMaterializer.java` | EventEnvelope -> NotificationCommand | Bắt buộc |
| `*NotificationMapper.java` | Typed mappers cho dish ready, low stock, payment, refund, reservation seated, order cancelled | Bắt buộc |
| `JdbcNotificationQueueRepository.java` | Queue notification vào DB, validate channel adapter | Bắt buộc |
| `JdbcNotificationInboxRepository.java` | Load/mark read inbox | Nên đọc |
| `Email/Sms/InApp/Phone/PrintNotificationChannelAdapter.java` | Channel validation | Nên đọc |
| `common/messaging/ManualAckConsumerSupport.java` | Retry/DLQ/manual ack/idempotency support | Bắt buộc |

#### Luồng xử lý chính

1. Module khác publish domain event hoặc `NotificationRequested` qua outbox.
2. RabbitMQ route event tới queue notification tương ứng.
3. `NotificationRequestedConsumer` nhận message và gọi `ManualAckConsumerSupport`.
4. `NotificationRequestMaterializer` chọn typed mapper hoặc generic mapper.
5. `JdbcNotificationQueueRepository` validate channel và insert `notification_messages`, `notification_deliveries`.
6. User đọc inbox qua `NotificationController`.

#### Module này liên quan đến module nào khác?

| Module liên quan | Kiểu liên kết | File liên quan | Ghi chú |
|---|---|---|---|
| Ordering | Event | `OrderCancelledNotificationMapper`, `NotificationRequestedConsumer` | order cancelled/menu/promotion events |
| Kitchen | Event | `DishReadyNotificationMapper` | dish status/ready |
| Billing | Event | `PaymentCompletedNotificationMapper`, `RefundIssuedNotificationMapper` | payment/refund/receipt |
| Reservation | Event | `ReservationSeatedNotificationMapper` | seated/waitlist/reservation |
| Inventory | Event | `LowStockNotificationMapper` | low-stock alert |
| Identity/Audit | Event | `AuditFollowUpRequestedConsumer` | audit follow-up notification |

#### Test coverage hiện tại

| Test file/folder | Đang cover gì | Nhận xét |
|---|---|---|
| `reporting/api/ReportingAndSystemApiContractTest.java` | notification inbox/read endpoint + unauthorized shape | Có API inbox contract |
| Không thấy `notification/*` test riêng | Event consumer/materializer/mapper/channel validation | Thiếu test trực tiếp cho event-driven flow |

#### Functional gaps cần kiểm tra

- Notification delivery hiện tại ghi queued/delivery rows; cần kiểm tra có worker gửi email/SMS thật hay chỉ validate/queue.
- Typed mapper chưa thấy cho mọi event mà consumer nhận, một số event sẽ fallback generic.
- Read/unread/inbox filtering theo user/role cần review kỹ trong repository.

#### Non-functional gaps cần kiểm tra

- Event duplicate xử lý qua `InboxEventDeduplicator`, cần test trực tiếp.
- Retry/DLQ có trong `ManualAckConsumerSupport`, nhưng cần integration test RabbitMQ thật.
- Channel adapters hiện chủ yếu validate; observability delivery thực tế cần kiểm tra thêm.

#### Bugs/Risks có thể có

- High priority: không thấy test event-driven notification end-to-end với RabbitMQ/outbox.
- Nếu typed mapper thiếu field, generic fallback vẫn tạo notification nhưng content có thể kém chính xác.

#### Câu hỏi cần trả lời khi review module này

- Module này nhận input từ đâu?
- Output đi đâu?
- Có gọi service khác không?
- Có được gateway route tới không?
- Có test chưa?
- Có thiếu validation không?
- Có xử lý lỗi chưa?
- Có case nào dễ fail khi chạy Docker/local không?

### 7.5 Ordering

#### Vai trò chính

Ordering quản lý order/menu/category/item/combo/promotion, tạo draft/confirmed order, send/cancel item, cập nhật line status và publish events cho kitchen/reporting/audit/notification.

#### Các file/folder quan trọng cần đọc

| File/Folder | Vai trò | Mức độ ưu tiên |
|---|---|---|
| `runtime/ordering/OrderingServiceApplication.java` | Entry runtime ordering | Bắt buộc |
| `ordering/api/OrdersController.java` | `/api/orders/**` | Bắt buộc |
| `ordering/api/OrderingIntegrationController.java` | Internal endpoints từ kitchen/billing | Bắt buộc |
| `MenuController`, `MenuItemController`, `MenuCategoryController`, `MenuPromotionController` | Menu CRUD/API | Nên đọc |
| `OrdersService.java` | Ordering facade | Bắt buộc |
| `OrderCreationService.java`, `OrderDraftConfirmationService.java`, `OrderItemWorkflowService.java` | Core lifecycle | Bắt buộc |
| `OrderCreationAuditPublisher.java` | Publish `OrderConfirmed` | Bắt buộc |
| `ordering/application/workflow/` | Order fulfillment/order cancellation follow-up | Nên đọc |
| `ordering/integration/RemoteKitchenOrderRoutingClient.java` | Call kitchen internal APIs | Bắt buộc |

#### Luồng xử lý chính

1. User load overview/menu qua `/api/orders/overview`, `/api/menu/**`.
2. User tạo order qua `OrdersController.createOrder`, có item và combo selections.
3. Draft có thể confirm sau; confirmed order publish `OrderConfirmed`.
4. Ordering gọi kitchen để queue confirmed items.
5. Kitchen callback internal ordering để refresh/sync line statuses.
6. Cancel order/item publish `OrderCancelled` và notification/audit follow-up.

#### Module này liên quan đến module nào khác?

| Module liên quan | Kiểu liên kết | File liên quan | Ghi chú |
|---|---|---|---|
| Kitchen | Remote call/Event | `RemoteKitchenOrderRoutingClient`, `OrderingIntegrationController` | Order -> ticket, kitchen -> line status |
| Billing | Remote call | `OrderingIntegrationController` | Billing apply promotion |
| Notification/Audit/Reporting | Event | `OrderConfirmedEvent`, `OrderCancelledEvent`, `PromotionUpdatedEvent`, `MenuItemAvailabilityChangedEvent` | Outbox/RabbitMQ |
| Gateway | Routing | `GatewayRouteLocator.java` | `/api/orders`, `/api/menu` |

#### Test coverage hiện tại

| Test file/folder | Đang cover gì | Nhận xét |
|---|---|---|
| `ordering/api/OrderingApiContractTest.java` | overview, create combo order, confirm/cancel/item actions, menu/combo/category/item/promotion CRUD, validation | Có controller contract tốt |
| `architecture/InternalServiceBoundaryContractTest.java` | Boundary/package rules | Có kiến trúc test |

#### Functional gaps cần kiểm tra

- Cancel/update order có đủ trạng thái và compensation tới kitchen/billing không.
- Combo/promotion pricing edge cases cần review thêm.
- Internal promotion apply cho billing cần test remote failure.

#### Non-functional gaps cần kiểm tra

- Remote call tới kitchen chưa có retry/circuit breaker.
- Idempotency order confirm/create cần kiểm tra thêm ngoài contract tests.
- Transaction consistency giữa order persistence và outbox publish cần review.

#### Bugs/Risks có thể có

- High priority: nếu kitchen service down, confirmed order routing có thể fail trực tiếp.
- Cần kiểm tra duplicate confirm hoặc resend delayed item có sinh duplicate kitchen ticket không.

#### Câu hỏi cần trả lời khi review module này

- Module này nhận input từ đâu?
- Output đi đâu?
- Có gọi service khác không?
- Có được gateway route tới không?
- Có test chưa?
- Có thiếu validation không?
- Có xử lý lỗi chưa?
- Có case nào dễ fail khi chạy Docker/local không?

### 7.6 Billing

#### Vai trò chính

Billing quản lý bill overview, create/update bill, split bill, payment, receipt, refund request/review/execution và publish billing events.

#### Các file/folder quan trọng cần đọc

| File/Folder | Vai trò | Mức độ ưu tiên |
|---|---|---|
| `runtime/billing/BillingServiceApplication.java` | Entry runtime billing | Bắt buộc |
| `billing/api/BillingController.java` | `/api/billing/overview` | Bắt buộc |
| `billing/api/BillsController.java` | create/update/promotion/split/payment | Bắt buộc |
| `billing/api/PaymentsController.java` | receipt/refunds | Bắt buộc |
| `billing/api/RefundsController.java` | pending refund/approval | Bắt buộc |
| `BillingService.java` | Facade billing | Bắt buộc |
| `BillingBillCreationService.java`, `BillingPaymentService.java`, `BillingSplitService.java` | Core bill/payment/split | Bắt buộc |
| `BillingRefund*Service.java` | Refund lifecycle | Bắt buộc |
| `BillingReceiptService.java`, `DigitalReceiptDeliveryAdapter.java` | Receipt flow | Nên đọc |
| `billing/integration/RemotePromotionApplicationClient.java` | Call ordering promotion | Bắt buộc |
| `billing/application/workflow/` | Payment completed/refund settlement/reporting/notification/audit steps | Nên đọc |

#### Luồng xử lý chính

1. User tạo bill từ `tableSessionId`.
2. Billing tính line/tax/service fee/tip/discount.
3. Apply promotion gọi ordering internal endpoint.
4. Split bill theo strategy domain.
5. Process payment publish `PaymentCompleted`.
6. Receipt/refund flow publish `ReceiptGenerated`, `RefundIssued`, notification/reporting/audit follow-up.

#### Module này liên quan đến module nào khác?

| Module liên quan | Kiểu liên kết | File liên quan | Ghi chú |
|---|---|---|---|
| Ordering | Remote call | `RemotePromotionApplicationClient` | Apply promotion |
| Notification | Event | `BillingNotificationRequestStep`, refund/receipt services | Payment/refund/receipt notification |
| Reporting/Audit | Event | `BillingReportingProjectionStep`, `BillingAuditRequestStep` | Projection/audit |
| Gateway | Routing | `GatewayRouteLocator.java` | `/api/billing`, `/api/bills`, `/api/payments`, `/api/refunds` |

#### Test coverage hiện tại

| Test file/folder | Đang cover gì | Nhận xét |
|---|---|---|
| `operations/api/BillingInventoryKitchenApiContractTest.java` | billing overview, bill ops, payment/receipt/refund, validation | Có controller happy path/error shape |
| `nonfunctional/Section1NonFunctionalContractTest.java` | refund approval concurrency | Có concurrency smoke |

#### Functional gaps cần kiểm tra

- Payment failure/refund failure/partial payment edge cases cần đọc kỹ.
- Promotion apply remote failure behavior cần test.
- Receipt delivery thực tế qua email/print cần kiểm tra thêm.

#### Non-functional gaps cần kiểm tra

- Transaction giữa payment/refund/outbox publish cần review.
- Idempotency payment/refund cần kiểm tra với duplicate request/event.
- PDF receipt adapter/mail config cần review Docker/local.

#### Bugs/Risks có thể có

- High priority: payment/refund là vùng tiền, cần bổ sung integration/idempotency tests.
- Remote promotion failure có thể làm apply promotion fail hard nếu ordering unavailable.

#### Câu hỏi cần trả lời khi review module này

- Module này nhận input từ đâu?
- Output đi đâu?
- Có gọi service khác không?
- Có được gateway route tới không?
- Có test chưa?
- Có thiếu validation không?
- Có xử lý lỗi chưa?
- Có case nào dễ fail khi chạy Docker/local không?

## 8. Event-driven Flow trong Notification

### Tổng quan

Notification dùng event-driven flow qua transactional outbox và RabbitMQ. Event producer ghi outbox; relay publish sang RabbitMQ theo `EventRoutingRegistry`; notification service consume các queue notification bằng `NotificationRequestedConsumer`; `ManualAckConsumerSupport` đảm nhiệm manual ack, retry, DLQ và inbox deduplication.

### Event publisher

| Event | Publisher | File | Khi nào publish |
|---|---|---|---|
| `NotificationRequested` | Common notification publisher | `common/notification/OutboxNotificationCommandPublisher.java` | Khi module gọi `NotificationCommandPublisher.enqueue` |
| `OrderConfirmed` | Ordering | `OrderCreationAuditPublisher.java`, `OrderDraftConfirmationService.java` | Order được tạo/confirm |
| `OrderCancelled` | Ordering | `OrderItemWorkflowService.java` | Order/item cancel |
| `KitchenDishStatusChanged` | Kitchen | `KitchenTicketEventNotifier.java` | Item cooking/ready/blocked/served/returned |
| `LowStockDetected` | Inventory | `PublishLowStockDetectedStep.java` | Low stock evaluation |
| `PaymentCompleted` | Billing | `BillingPaymentService.java` | Payment thành công |
| `RefundIssued` | Billing | `BillingRefundExecutionService.java` | Refund executed |
| `ReceiptGenerated` | Billing | `BillingReceiptService.java`, `BillingReceiptRequestStep.java` | Receipt generated/requested |
| `ReservationCreated`, `ReservationSeated`, `WaitlistUpdated` | Reservation | `ReservationSeatingMediator.java`, reservation workflow | Reservation/waitlist lifecycle |
| `MenuItemAvailabilityChanged`, `PromotionUpdated` | Ordering | `MenuItemService.java`, `MenuPromotionService.java` | Menu availability/promotion thay đổi |
| `AuditFollowUpRequested` | Identity/audit | audit flow | Audit follow-up tạo notification |

### Event consumer

| Event | Consumer/Handler | File | Xử lý gì |
|---|---|---|---|
| `NotificationRequested` | `notification.notification-request.delivery` | `NotificationRequestedConsumer.java` | Materialize notification command |
| `KitchenDishStatusChanged` | `notification.kitchen-dish-status.delivery` | `NotificationRequestedConsumer.java` | Dish/kitchen notification |
| `PaymentCompleted` | `notification.payment-completed.delivery` | `NotificationRequestedConsumer.java` | Payment notification |
| `RefundIssued` | `notification.refund-issued.delivery` | `NotificationRequestedConsumer.java` | Refund notification |
| `ReceiptGenerated` | `notification.receipt-generated.delivery` | `NotificationRequestedConsumer.java` | Receipt notification |
| `OrderCancelled` | `notification.order-cancelled.delivery` | `NotificationRequestedConsumer.java` | Cancel notification |
| `LowStockDetected` | `notification.low-stock.delivery` | `NotificationRequestedConsumer.java` | Low-stock notification |
| `ReservationCreated` | `notification.reservation-created.delivery` | `NotificationRequestedConsumer.java` | Reservation notification |
| `ReservationSeated` | `notification.reservation-seated.delivery` | `NotificationRequestedConsumer.java` | Table ready/seated notification |
| `WaitlistUpdated` | `notification.waitlist-updated.delivery` | `NotificationRequestedConsumer.java` | Waitlist notification |
| `MenuItemAvailabilityChanged` | `notification.menu-availability.delivery` | `NotificationRequestedConsumer.java` | Menu availability notification |
| `PromotionUpdated` | `notification.promotion-updated.delivery` | `NotificationRequestedConsumer.java` | Promotion notification |
| `AuditFollowUpRequested` | `notification.audit-follow-up.router` | `AuditFollowUpRequestedConsumer.java` | Tạo notification command cho audit follow-up |

### Event payload/schema

| Event | Payload chính | File/schema |
|---|---|---|
| Tất cả events | `EventEnvelope(metadata, payload)` | `common/events/EventEnvelope.java`, `EventMetadata.java` |
| Domain events | `eventType`, `eventVersion`, `aggregateType`, `aggregateId`, `Map<String,Object> payload` | `common/events/BaseDomainEvent.java` |
| Notification command | IDs liên quan, `channel`, `type`, `templateCode`, `payload`, `title`, `body`, recipient, priority | `common/notification/NotificationCommand.java` |
| Typed notification mapping | Field phụ thuộc từng event | `notification/application/*NotificationMapper.java` |

Không thấy schema registry/Avro/JSON Schema riêng. Payload đang là `Map<String,Object>`, cần review contract giữa producer và mapper.

### Event flow

1. Module A phát sinh hành động, ví dụ billing process payment hoặc kitchen item ready.
2. Module publish domain event/notification command vào outbox.
3. Outbox relay gửi event sang RabbitMQ exchange/routing key theo `EventRoutingRegistry`.
4. RabbitMQ route vào queue của notification service.
5. `NotificationRequestedConsumer` consume message với manual ack.
6. `NotificationRequestMaterializer` map event thành `NotificationCommand`.
7. `JdbcNotificationQueueRepository` insert notification message/delivery.
8. User xem inbox qua `/api/notifications`.
9. Nếu handler fail, `ManualAckConsumerSupport` retry với backoff, quá số lần thì record dead-letter và gửi DLQ.

### Rủi ro cần kiểm tra

- Event bị mất thì sao? Outbox/RabbitMQ giảm rủi ro, nhưng cần integration test thật.
- Event bị duplicate thì sao? Có `InboxEventDeduplicator`, cần test trực tiếp.
- Consumer xử lý fail thì sao? Có retry và DLQ trong `ManualAckConsumerSupport`.
- Có retry không? Có, header `x-irms-retry-count`, backoff exponential, max default 5.
- Có dead-letter queue không? Có DLX/DLQ trong `RabbitMqTopologyConfig`.
- Có idempotency không? Có metadata `idempotencyKey` và inbox processed event, cần review DB constraint/test.
- Có logging đủ để debug không? Cần kiểm tra thêm trong code.
- Có test cho event-driven flow không? Không thấy test riêng cho RabbitMQ notification end-to-end.

## 9. Test Coverage Overview

### Tổng quan test

Đã chạy `backend/mvnw.cmd -q test`: 87 tests pass, 0 failures, 0 errors.

| Module | Có test không? | Test file/folder | Nhận xét |
|---|---|---|---|
| identity | Có | `identity/api/IdentityApiContractTest.java` | Cover auth/staff/settings/audit/error shape |
| kitchen | Có | `operations/api/BillingInventoryKitchenApiContractTest.java` | Cover public kitchen API contract, chưa cover remote/RabbitMQ sâu |
| reservation | Có | `reservation/api/ReservationApiContractTest.java`, `nonfunctional/Section1NonFunctionalContractTest.java` | Cover reservation/table/waitlist/validation/concurrency smoke |
| notification | Có một phần | `reporting/api/ReportingAndSystemApiContractTest.java` | Cover inbox/read/unauthorized, thiếu event consumer/materializer tests |
| ordering | Có | `ordering/api/OrderingApiContractTest.java` | Cover order/menu/combo/promotion/controller contract |
| billing | Có | `operations/api/BillingInventoryKitchenApiContractTest.java`, `nonfunctional/Section1NonFunctionalContractTest.java` | Cover bill/payment/refund API và refund concurrency smoke |

### Những phần đang được cover tốt

- Controller contract cho identity, reservation, ordering, billing/kitchen/inventory, reporting/notification.
- Error envelope/validation/forbidden/conflict ở nhiều API.
- Architecture boundary rules trong `InternalServiceBoundaryContractTest`.
- Một số non-functional smoke: latency baseline, burst order, concurrency waitlist/refund/inventory, correlation/session permission.

### Những phần có vẻ thiếu test

- Gateway route/proxy/upstream failure/timeout.
- RabbitMQ/outbox/inbox notification end-to-end.
- Internal REST clients khi callee timeout/down/error.
- Docker Compose/migration/runtime startup integration.
- Payment/refund idempotency ở mức DB/integration.

### Test gaps cần ưu tiên

1. Event-driven notification end-to-end với RabbitMQ enabled.
2. Gateway route contract cho tất cả route trong `GatewayRouteLocator`.
3. Internal remote call failure tests: ordering->kitchen, kitchen->ordering/inventory, billing->ordering, service->identity.
4. Docker Compose smoke: migration + health endpoints + frontend proxy.
5. Idempotency/concurrency sâu cho payment, refund, order confirm, kitchen status sync.

## 10. Functional Gaps cần kiểm tra

| Khu vực | Gap/Risk | File liên quan | Mức độ ưu tiên |
|---|---|---|---|
| Gateway | Special route `POST /api/notifications` đi reservation-service, dễ nhầm với notification ownership | `GatewayRouteLocator.java`, `ReservationNotificationController.java`, `NotificationController.java` | High |
| Remote calling | Remote client failure chưa có retry/circuit breaker/compensation rõ | `RemoteKitchenOrderRoutingClient.java`, `RemoteOrderStateUpdateClient.java`, `RemoteInventoryConsumptionClient.java`, `RemotePromotionApplicationClient.java` | High |
| Notification | Event-driven flow thiếu test RabbitMQ end-to-end | `NotificationRequestedConsumer.java`, `ManualAckConsumerSupport.java` | High |
| Billing | Payment failure/refund failure/refund duplicate cần kiểm tra sâu | `BillingPaymentService.java`, `BillingRefund*Service.java` | High |
| Ordering | Duplicate confirm/resend/cancel compensation tới kitchen/billing | `OrderDraftConfirmationService.java`, `OrderItemWorkflowService.java` | High |
| Kitchen | Sync trạng thái kitchen-ordering-inventory khi một service fail | `KitchenTicketEventNotifier.java`, remote integration clients | High |
| Reservation | Cancel/release table/session lifecycle cần xác nhận đủ API/flow | `ReservationLifecycleService.java`, `ReservationTableStatusService.java` | Medium |
| Identity | Role/permission matrix có đủ mọi route UI chưa | `common/security/PermissionGuard.java`, controllers | Medium |
| Seed data | Seed dùng nhiều trạng thái demo, cần đảm bảo không che bug runtime thật | `V2__seed_demo_data.sql`, `V103__expand_demo_seed_data.sql` | Medium |

## 11. Non-functional Gaps cần kiểm tra

| Nhóm | Cần kiểm tra | File/module liên quan | Ghi chú |
|---|---|---|---|
| Error handling | Internal REST exceptions có bubble đúng error envelope không | Remote clients, `GlobalExceptionHandler` | Gateway có 502, service clients ít catch |
| Logging | Consumer retry/DLQ/outbox relay có log đủ correlation/event id không | `common/messaging`, `common/outbox` | Cần kiểm tra thêm trong code |
| Security | Dev fallback token/secret không được dùng production | `application.properties`, Docker env | High priority |
| Authentication/Authorization | Tất cả gateway-routed endpoints có permission guard chưa | Controllers, `SecurityConfig` | Cần audit route-by-route |
| Input validation | Request DTO có `@Valid/@NotNull` chưa đủ domain rule không | Controllers + services | Một số body record ít annotation |
| Performance | Gateway read timeout 20s, internal read timeout 8s có hợp use case không | `GatewayHttpClientConfiguration`, `RestClientConfiguration` | Cần benchmark Docker |
| Retry/Timeout | Không thấy retry/circuit breaker cho REST | Remote clients | RabbitMQ có retry/DLQ |
| Idempotency | Event inbox có dedup, business commands cần thêm idempotency test | `InboxEventDeduplicator`, payment/order/kitchen | High priority |
| Observability | Correlation ID có ở API; event/Rabbit logs cần kiểm tra | `CorrelationIdFilter`, `EventMetadata` | Contract test có một phần |
| Config | Local/Docker khác RabbitMQ/Flyway/default endpoints | `application-local.properties`, `application-docker.properties` | High priority |
| Docker | Compose healthcheck/migration dependency ổn, cần smoke full stack | `docker-compose.yml` | Chưa chạy trong lần đọc này |
| Database transaction | Shared DB nhiều service, cần boundary/ownership review | `service-topology.yml`, JDBC repositories | Shared DB risk |
| Event reliability | Retry/DLQ có nhưng thiếu integration evidence | `RabbitMqTopologyConfig`, `ManualAckConsumerSupport` | High priority |
| Testability | Tests chủ yếu standalone MockMvc, ít integration thật | `src/test/java` | Cần bổ sung Docker/Rabbit/Postgres tests |

## 12. Chia việc cho nhóm 6 người

### Người 1: Identity

#### Phạm vi đọc

- Auth, session, RBAC, staff/shift/settings, audit, event ops.

#### File/folder cần đọc trước

- `IrmsApplication.java`
- `runtime/identity/IdentityAuditServiceApplication.java`
- `identity/api/AuthController.java`
- `identity/api/IdentityInternalController.java`
- `identity/api/AuditController.java`
- `identity/api/EventOpsController.java`
- `common/security/`

#### Câu hỏi cần trả lời

- Login/session/token flow hoạt động thế nào?
- Permission nào bảo vệ từng route?
- Service khác gọi identity bằng internal endpoint nào?
- Dev secret/token có được override trong Docker không?

#### Risk/bug nên tìm

- Default dev secret/token.
- Identity outage bị swallow thành empty session/policy ở client nào.
- Audit replay/dead-letter permission.

#### Test coverage cần kiểm tra

- `IdentityApiContractTest.java`
- `IrmsApplicationTests.java`
- `Section1NonFunctionalContractTest.java`

---

### Người 2: Kitchen

#### Phạm vi đọc

- Kitchen ticket/item lifecycle, station queue, priority, automation, sync ordering/inventory, dish status events.

#### File/folder cần đọc trước

- `runtime/kitchen/KitchenServiceApplication.java`
- `kitchen/api/KitchenController.java`
- `kitchen/api/KitchenIntegrationController.java`
- `KitchenOrderRoutingService.java`
- `KitchenTicketItemWorkflowService.java`
- `KitchenTicketEventNotifier.java`
- `kitchen/integration/`

#### Câu hỏi cần trả lời

- Ordering gửi order sang kitchen bằng flow nào?
- Status transition nào hợp lệ?
- Kitchen sync order/inventory ra sao?
- Event dish ready/status publish khi nào?

#### Risk/bug nên tìm

- Sync fail làm lệch kitchen-ordering-inventory.
- Duplicate event/order ticket.
- Automation scheduler chạy trùng hoặc sai Docker/local.

#### Test coverage cần kiểm tra

- `BillingInventoryKitchenApiContractTest.java`
- `Section1NonFunctionalContractTest.java`

---

### Người 3: Reservation

#### Phạm vi đọc

- Reservation, table, waitlist, seating/check-in/no-show, notification request.

#### File/folder cần đọc trước

- `runtime/reservation/ReservationServiceApplication.java`
- `reservation/api/ReservationController.java`
- `reservation/api/TableController.java`
- `reservation/api/WaitlistController.java`
- `ReservationCheckInService.java`
- `ReservationWaitlistSeatingService.java`
- `ReservationNotificationCoordinator.java`

#### Câu hỏi cần trả lời

- Reservation tạo/confirm/check-in/no-show đi qua file nào?
- Table session mở/đóng ở đâu?
- Waitlist notify/skip/seat có đủ edge cases không?
- `POST /api/notifications` tại sao thuộc reservation?

#### Risk/bug nên tìm

- Table release/session consistency.
- Waitlist conflict/expiration.
- Notification event fail ảnh hưởng business flow.

#### Test coverage cần kiểm tra

- `ReservationApiContractTest.java`
- `Section1NonFunctionalContractTest.java`

---

### Người 4: Notification + Event-driven Flow

#### Phạm vi đọc

- Outbox/RabbitMQ/event routing, notification consumers, mappers, queue/inbox repository, retry/DLQ/idempotency.

#### File/folder cần đọc trước

- `runtime/notification/NotificationServiceApplication.java`
- `notification/infrastructure/messaging/NotificationRequestedConsumer.java`
- `notification/application/NotificationRequestMaterializer.java`
- `notification/application/*NotificationMapper.java`
- `notification/infrastructure/persistence/JdbcNotificationQueueRepository.java`
- `common/events/EventRoutingRegistry.java`
- `common/messaging/ManualAckConsumerSupport.java`
- `common/outbox/`

#### Câu hỏi cần trả lời

- Event nào publish vào notification?
- Mapper nào xử lý event nào?
- Duplicate/fail/retry/DLQ hoạt động ra sao?
- Có gửi notification thật hay chỉ queue/inbox?

#### Risk/bug nên tìm

- Thiếu mapper hoặc payload contract lỏng.
- Thiếu integration test RabbitMQ.
- Retry/DLQ không đủ observability.

#### Test coverage cần kiểm tra

- `ReportingAndSystemApiContractTest.java` phần notification inbox.
- Không thấy test riêng event-driven notification.

---

### Người 5: Ordering

#### Phạm vi đọc

- Order/menu/combo/promotion, create/confirm/cancel/send item, remote kitchen, order events.

#### File/folder cần đọc trước

- `runtime/ordering/OrderingServiceApplication.java`
- `ordering/api/OrdersController.java`
- `ordering/api/OrderingIntegrationController.java`
- `OrdersService.java`
- `OrderCreationService.java`
- `OrderDraftConfirmationService.java`
- `OrderItemWorkflowService.java`
- `RemoteKitchenOrderRoutingClient.java`

#### Câu hỏi cần trả lời

- Order draft/confirmed khác nhau thế nào?
- Combo/promotion được tính/validate ở đâu?
- Khi confirm order thì gọi kitchen và publish event thế nào?
- Cancel item/order có compensation đầy đủ không?

#### Risk/bug nên tìm

- Duplicate confirm/resend.
- Remote kitchen down.
- Pricing/promotion edge cases.

#### Test coverage cần kiểm tra

- `OrderingApiContractTest.java`
- `InternalServiceBoundaryContractTest.java`

---

### Người 6: Billing

#### Phạm vi đọc

- Bill, split, payment, receipt, refund, promotion remote call, billing events.

#### File/folder cần đọc trước

- `runtime/billing/BillingServiceApplication.java`
- `billing/api/BillsController.java`
- `billing/api/PaymentsController.java`
- `billing/api/RefundsController.java`
- `BillingService.java`
- `BillingPaymentService.java`
- `BillingRefund*Service.java`
- `BillingReceiptService.java`
- `RemotePromotionApplicationClient.java`

#### Câu hỏi cần trả lời

- Bill tạo từ table session/order data như thế nào?
- Payment/refund transaction boundary ở đâu?
- Promotion gọi ordering ra sao?
- Receipt/notification/reporting/audit publish khi nào?

#### Risk/bug nên tìm

- Payment/refund duplicate.
- Promotion remote failure.
- Receipt delivery/mail/PDF config.

#### Test coverage cần kiểm tra

- `BillingInventoryKitchenApiContractTest.java`
- `Section1NonFunctionalContractTest.java`

## 13. Checklist trước meeting

### Checklist chung

- [ ] Pull source code mới nhất.
- [ ] Chạy project bằng Docker.
- [ ] Không ưu tiên setup local thủ công nếu chưa rõ config.
- [ ] Đọc `IrmsApplication.java` và `runtime/`.
- [ ] Đọc gateway routing.
- [ ] Đọc module được phân công.
- [ ] Kiểm tra test coverage của module.
- [ ] Tìm functional gaps.
- [ ] Tìm non-functional gaps.
- [ ] Ghi lại bugs/risk nếu có.
- [ ] Chuẩn bị câu hỏi để chốt trong meeting.

### Checklist khi đọc từng module

- [ ] Module này làm gì?
- [ ] Entry file của module là gì?
- [ ] Flow chính đi qua những file nào?
- [ ] Module này gọi module nào khác?
- [ ] Module này được gọi bởi ai?
- [ ] Có remote call không?
- [ ] Có event không?
- [ ] Có test không?
- [ ] Error handling đã ổn chưa?
- [ ] Config có phụ thuộc Docker không?

## 14. Script nói trong meeting

### Phiên bản 1: Nói tự nhiên trong meeting

Mọi người ơi, mình đã đẩy source code lên rồi. Trước buổi họp, mỗi người đọc phần context và test coverage của module mình để xem còn thiếu gì về functional và non-functional nhé. Các module cần ưu tiên là identity, kitchen, reservation, notification vì có event-driven flow, ordering và billing; phần còn lại đọc thêm nếu có thời gian.

Mọi người nhớ review gateway routing trước, vì hiện project đi theo service-based architecture và có remote calling giữa các service. Entry chính hiện tại là `IrmsApplication` và các class trong `runtime`. Phần `resources` chứa config database, service profiles và seed data qua Flyway. Khi chạy thì ưu tiên Docker nhé, vì setup local thủ công dễ bị fallback config hoặc lệch so với runtime thật.

### Phiên bản 2: Gửi vào group chat

Team ơi, source code IRMS đã được đẩy lên rồi. Trước meeting mọi người giúp mình:

- Đọc context + test coverage để tìm thiếu sót functional/non-functional.
- Ưu tiên các module: `identity`, `kitchen`, `reservation`, `notification`, `ordering`, `billing`.
- Riêng `notification` đọc kỹ event-driven flow.
- Review gateway routing cho các service.
- Xem remote calling giữa service với service.
- Đọc entry: `IrmsApplication.java` và folder `runtime/`.
- Đọc `resources` để nắm DB config, service config, Flyway migration và seed data.
- Chạy thử bằng Docker, hạn chế setup local thủ công vì dễ fallback/sai config.
- Ghi lại bugs/risk/câu hỏi để chốt trong meeting.

### Phiên bản 3: Checklist ngắn

- [ ] Pull code mới nhất.
- [ ] Chạy bằng Docker.
- [ ] Đọc `IrmsApplication.java` + `runtime/`.
- [ ] Review gateway routing.
- [ ] Đọc module được phân công.
- [ ] Kiểm tra remote call/event flow.
- [ ] Kiểm tra resources/config/seed data.
- [ ] Kiểm tra test coverage.
- [ ] Ghi functional gaps.
- [ ] Ghi non-functional gaps.
- [ ] Chuẩn bị bugs/risk/câu hỏi.

## 15. Các câu hỏi cần chốt trong meeting

### Architecture

- Shared DB hiện tại là quyết định cố định hay tạm thời?
- Boundary giữa service đã đủ rõ theo package/runtime chưa?
- Module nào là owner của dữ liệu nào?

### Gateway/Routing

- Route table trong `GatewayRouteLocator` đã đủ endpoint chưa?
- `POST /api/notifications` route sang reservation-service có đúng ownership không?
- Có cần test gateway route/proxy riêng không?

### Remote Calling

- Remote call fail thì nghiệp vụ rollback/retry/compensate thế nào?
- Có cần circuit breaker/retry cho REST không?
- Internal service token đã đủ bảo vệ `/internal/**` chưa?

### Module Responsibility

- Ordering/kitchen/billing responsibility có bị overlap trạng thái order item không?
- Reservation có chịu trách nhiệm notification request trực tiếp hay nên đi qua notification service?
- Identity/audit có gom quá nhiều responsibility không?

### Notification/Event-driven

- Event payload contract có cần schema rõ hơn không?
- Event duplicate/failure/retry/DLQ đã có test chưa?
- Notification hiện có gửi email/SMS thật hay chỉ queue/inbox?
- Mapper nào còn thiếu cho các event đang consume?

### Database/Resources

- Flyway migration/seed có chạy repeatable ổn trong Docker không?
- Seed demo có đủ cho meeting/demo không?
- Shared DB có constraint đảm bảo consistency giữa service không?

### Test Coverage

- Contract tests hiện tại có đủ cho Section 1 chưa?
- Thiếu test gateway/RabbitMQ/Docker nào cần ưu tiên?
- Test nào đang chỉ happy path?

### Functional Requirements

- Reservation cancel/release table/session lifecycle đã đủ chưa?
- Ordering cancel/update/resend có đủ trạng thái chưa?
- Kitchen item status sync đã đồng bộ với ordering chưa?
- Billing payment failure/refund/partial payment đã đủ chưa?
- Notification event mapping có đúng từng business event không?

### Non-functional Requirements

- Config local vs Docker có được kiểm soát không?
- Production secret/token có bắt buộc override không?
- Observability/log/correlation event có đủ để debug không?
- Idempotency payment/order/event đã đủ chưa?

### Docker/Runtime/Config

- Runtime nào cần start trước/sau ngoài `depends_on` hiện tại?
- Healthcheck service đã đủ chuẩn chưa?
- Local script có còn nên dùng hay chỉ Docker?
- RabbitMQ local disabled có gây hiểu nhầm khi test không?

### Bugs/Risks

- Risk nghiêm trọng nhất hiện tại là gì?
- Module nào cần fix/test trước demo?
- Có inconsistency giữa docs, code, config và Docker không?

## 16. Kết luận và next steps

Source code hiện tại đã có service-based runtime rõ: `IrmsApplication` dispatch service, `runtime/*` tách entry, gateway proxy route UI API, Docker Compose chạy full stack, Flyway quản lý schema/seed và RabbitMQ/outbox hỗ trợ event-driven flow.

Next steps ưu tiên:

1. Cả nhóm đọc đúng module được phân công theo section 12.
2. Review kỹ gateway routing và remote calling failure.
3. Bổ sung hoặc ít nhất ghi nhận test gap cho RabbitMQ notification end-to-end, gateway proxy, Docker full-stack smoke.
4. Chốt trong meeting các risk high priority: default secrets, local/Docker config lệch, remote call không retry/circuit breaker, payment/refund/order/kitchen idempotency.
5. Khi demo/chạy thử, ưu tiên Docker Compose thay vì setup local thủ công.
