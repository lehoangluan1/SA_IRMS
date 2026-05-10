# IRMS Service Deep Dive Documentation

Tài liệu này bám theo source code hiện tại trong `irms_project`. Mục tiêu là giúp dev trace nhanh từ màn hình frontend tới API gateway, controller, service, repository, event hoặc remote call liên quan.

## 0. Tổng quan runtime, gateway và cách FE gọi BE

### 0.1 Runtime model

Entry chính là `backend/src/main/java/SA/irms/IrmsApplication.java`. File này đọc `IRMS_RUNTIME_MODE`, `IRMS_RUNTIME_SERVICE`, `irms.runtime.mode`, `irms.runtime.service`, rồi dispatch sang runtime application tương ứng. Nếu không set runtime hợp lệ thì throw `IllegalStateException`, tức monolith runtime không được bật mặc định.

| Service | Runtime entry file | Port/config | Package scan | Ghi chú |
| ------- | ------------------ | ----------- | ------------ | ------- |
| API Gateway | `backend/src/main/java/SA/irms/gateway/ApiGatewayApplication.java` | `application-api-gateway.properties`, Docker `SERVER_PORT=8080` | Gateway + common config/security/web/error/identity/remote | Exclude datasource/Flyway, proxy `/api/**`. |
| ordering-service | `backend/src/main/java/SA/irms/runtime/ordering/OrderingServiceApplication.java` | `application-ordering-service.properties`, Docker `8081` | `SA.irms.ordering`, `SA.irms.common` | `@EnableScheduling`, gọi kitchen và identity. |
| kitchen-service | `backend/src/main/java/SA/irms/runtime/kitchen/KitchenServiceApplication.java` | `application-kitchen-service.properties`, Docker `8082` | `SA.irms.kitchen`, `SA.irms.common` | Gọi ordering, inventory, identity. |
| billing-service | `backend/src/main/java/SA/irms/runtime/billing/BillingServiceApplication.java` | `application-billing-service.properties`, Docker `8083` | `SA.irms.billing`, `SA.irms.common`, `SA.irms.adapters.pdf` | Tạo bill/payment/refund/receipt PDF. |
| reservation-service | `backend/src/main/java/SA/irms/runtime/reservation/ReservationServiceApplication.java` | `application-reservation-service.properties`, Docker `8084` | `SA.irms.reservation`, `SA.irms.common` | Reservation, table, waitlist, notification request. |
| inventory-service | `backend/src/main/java/SA/irms/runtime/inventory/InventoryServiceApplication.java` | `application-inventory-service.properties`, Docker `8085` | `SA.irms.inventory`, `SA.irms.common` | Inventory overview, stock, kitchen consumption. |
| notification-service | `backend/src/main/java/SA/irms/runtime/notification/NotificationServiceApplication.java` | `application-notification-service.properties`, Docker `8086` | `SA.irms.notification`, `SA.irms.common` | Inbox API và RabbitMQ notification consumers. |
| reporting-service | `backend/src/main/java/SA/irms/runtime/reporting/ReportingServiceApplication.java` | `application-reporting-service.properties`, Docker `8087` | `SA.irms.reporting`, `SA.irms.common` | Dashboard/report APIs và projection consumers. |
| identity-audit-service | `backend/src/main/java/SA/irms/runtime/identity/IdentityAuditServiceApplication.java` | `application-identity-audit-service.properties`, Docker `8088` | `SA.irms.identity`, `SA.irms.common` | Auth/session/RBAC/staff/settings/audit/internal identity. |
| migration | `backend/src/main/java/SA/irms/runtime/migration/MigrationApplication.java` | Docker `SERVER_PORT=0`, `IRMS_FLYWAY_ENABLED=true` | `SA.irms.common` | Chạy Flyway migration rồi exit. |

Docker root `docker-compose.yml` start `postgres`, `rabbitmq`, `irms-migration`, gateway, 8 backend services và `frontend`. `README.md` ghi local frontend `http://localhost:5173`, gateway `http://localhost:8080`.

### 0.2 Gateway routing

Gateway route nằm trong `backend/src/main/java/SA/irms/gateway/GatewayRouteLocator.java`, target URL đọc từ `GatewayRoutesProperties.java` và `application-api-gateway.properties`.

| API path | Target service | Gateway file | Controller xử lý ở service | Ghi chú |
| -------- | -------------- | ------------ | -------------------------- | ------- |
| `/api/auth/**` | identity-audit-service | `GatewayRouteLocator.java` | `identity/api/AuthController.java` | Login/logout/me. |
| `/api/staff/**`, `/api/shifts/**` | identity-audit-service | `GatewayRouteLocator.java` | `identity/api/StaffController.java` | Staff, role, shift. |
| `/api/settings/**` | identity-audit-service | `GatewayRouteLocator.java` | `identity/api/SettingsController.java` | Operational settings. |
| `/api/audit/**` | identity-audit-service | `GatewayRouteLocator.java` | `identity/api/AuditController.java`, `EventOpsController.java` | Audit log/export/event ops. |
| `/api/orders/**` | ordering-service | `GatewayRouteLocator.java` | `ordering/api/OrdersController.java` | Order overview/create/confirm/item actions. |
| `/api/menu/**` | ordering-service | `GatewayRouteLocator.java` | `MenuController.java`, `MenuItemController.java`, `MenuCategoryController.java`, `MenuPromotionController.java` | Menu, combo, category, item, promotion. |
| `/api/kitchen/**` | kitchen-service | `GatewayRouteLocator.java` | `kitchen/api/KitchenController.java` | Kitchen overview/status/priority/serve/return. |
| `/api/reservations/**` | reservation-service | `GatewayRouteLocator.java` | `reservation/api/ReservationController.java` | Reservation overview/create/update/confirm/check-in/no-show. |
| `/api/tables/**` | reservation-service | `GatewayRouteLocator.java` | `reservation/api/TableController.java` | Table CRUD/status. |
| `/api/waitlist/**` | reservation-service | `GatewayRouteLocator.java` | `reservation/api/WaitlistController.java` | Waitlist create/notify/skip/prioritize/seat. |
| `POST /api/notifications` | reservation-service | `GatewayRouteLocator.java` | `reservation/api/ReservationNotificationController.java` | Special route: queue reservation/waitlist notification. |
| `GET/POST /api/notifications/**` except POST root | notification-service | `GatewayRouteLocator.java` | `notification/api/NotificationController.java` | Inbox list/mark read. |
| `/api/billing/**`, `/api/bills/**`, `/api/payments/**`, `/api/refunds/**` | billing-service | `GatewayRouteLocator.java` | `BillingController.java`, `BillsController.java`, `PaymentsController.java`, `RefundsController.java` | Billing, bill, payment, receipt, refund. |
| `/api/inventory/**` | inventory-service | `GatewayRouteLocator.java` | `inventory/api/InventoryController.java` | Inventory overview/items/alerts. |
| `/api/dashboard/**`, `/api/reports/**` | reporting-service | `GatewayRouteLocator.java` | `DashboardController.java`, `ReportingController.java` | Dashboard/report/export. |

Internal APIs không đi qua FE gateway route table: `/internal/identity/**`, `/internal/ordering/**`, `/internal/kitchen/**`, `/internal/inventory/**`.

### 0.3 FE gọi BE như thế nào

| FE file/config | Cách gọi API | Target | Ghi chú |
| -------------- | ------------ | ------ | ------- |
| `frontend/src/lib/api/endpoints.ts` | Central endpoint constants `ENDPOINTS` | Gateway `/api/**` | Định nghĩa tất cả path FE dùng. |
| `frontend/src/lib/api/client.ts` | `apiFetch(path, init)` | `API_BASE_URL + path` | Set `Accept`, `Content-Type`, `X-Correlation-Id`, bearer token từ `localStorage`. |
| `frontend/src/lib/auth.tsx` | `signIn`, `signOut`, `refresh` | `/api/auth/login`, `/logout`, `/me` | Lưu token key `irms.auth.token`. |
| `frontend/nitro.config.ts` | Nitro proxy `/api/**` | `NITRO_API_PROXY_TARGET`, default `http://api-gateway:8080` | Dùng trong container frontend. |
| `frontend/.env.example` | `VITE_API_BASE_URL=` | Gateway hoặc same-origin | Local README khuyên `http://localhost:8080`. |
| `frontend/src/components/layout/TopBar.tsx` | `useQuery`, `useMutation` | `/api/notifications`, `/api/notifications/{id}/read` | Notification inbox. |
| `frontend/src/routes/_app.reservations.tsx` | `apiFetch` mutations/query | reservation/table/waitlist/POST notifications | Reservation & tables screen. |
| `frontend/src/routes/_app.orders.tsx` | `apiFetch` mutations/query | order/menu APIs | POS ordering screen. |
| `frontend/src/routes/_app.kitchen.tsx` | `apiFetch` mutations/query | kitchen APIs | Kitchen queue. |
| `frontend/src/routes/_app.billing.tsx` | `apiFetch` + direct `fetch` PDF | billing/bills/payments/refunds APIs | Billing settlement. |
| `frontend/src/routes/_app.inventory.tsx` | `apiFetch` mutations/query | inventory APIs | Inventory screen. |
| `frontend/src/routes/_app.reports.tsx` | `apiFetch` + direct `fetch` export | report APIs | Reports and export. |
| `frontend/src/routes/_app.staff.tsx` | `apiFetch` mutations/query | staff/shifts APIs | Staff & roles. |
| `frontend/src/routes/_app.audit.tsx` | `apiFetch` | audit APIs | Audit logs. |
| `frontend/src/routes/_app.settings.tsx` | `apiFetch` | settings APIs | Settings update. |

## 1. Identity Service

### 1.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Đăng nhập, đăng xuất, current user, signed access token, session repository, staff/shift/role, settings/policy, audit log, event operations. |
| API public chính | `/api/auth/**`, `/api/staff`, `/api/shifts`, `/api/settings`, `/api/audit/**`. |
| Internal API nếu có | `/internal/identity/sessions/**`, `/internal/identity/policy-snapshot`, `/internal/identity/default-branch`, `/internal/identity/users/**`. |
| Service khác gọi vào | Common security/identity clients từ các service gọi identity để introspect session, touch session, lấy policy, branch, display names, role-priority user. |
| Service này gọi ra | Publish notification command khi tạo shift; audit/session consumers nhận event qua RabbitMQ. |
| FE màn hình/flow liên quan | `login.tsx`, `AuthProvider`, `TopBar`, `_app.staff.tsx`, `_app.audit.tsx`, `_app.settings.tsx`. |

### 1.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `frontend/src/routes/login.tsx` + `frontend/src/lib/auth.tsx` | Submit login | `POST /api/auth/login` | `/api/auth` → identity | `AuthController.java` | `login` | `AuthService.login` | Xác thực email/password/role, tạo signed token và session. |
| `frontend/src/lib/auth.tsx` | App refresh session | `GET /api/auth/me` | `/api/auth` → identity | `AuthController.java` | `me` | `AuthService.currentUser` | Dùng bearer token từ localStorage. |
| `frontend/src/lib/auth.tsx`, `TopBar.tsx` | Sign out | `POST /api/auth/logout` | `/api/auth` → identity | `AuthController.java` | `logout` | `AuthService.logout` | Terminate session, FE clear token. |
| `frontend/src/routes/_app.staff.tsx` | Load staff/roles/shifts | `GET /api/staff?search=&shiftDate=` | `/api/staff` → identity | `StaffController.java` | `staff` | `StaffService.loadStaff` | Staff management. |
| `frontend/src/routes/_app.staff.tsx` | Create shift | `POST /api/shifts` | `/api/shifts` → identity | `StaffController.java` | `createShift` | `StaffService.createShift` | Audit + notification command. |
| `frontend/src/routes/_app.staff.tsx` | Update roles | `PATCH /api/staff/{userId}/roles` | `/api/staff` → identity | `StaffController.java` | `updateRoles` | `StaffService.updateRoles` | Replaces roles and terminates active sessions for target user. |
| `frontend/src/routes/_app.settings.tsx` | Load settings | `GET /api/settings` | `/api/settings` → identity | `SettingsController.java` | `settings` | `SettingsService.loadSettings` | Reads branch + policy snapshot. |
| `frontend/src/routes/_app.settings.tsx` | Save settings | `PATCH /api/settings` | `/api/settings` → identity | `SettingsController.java` | `updateSettings` | `SettingsService.updateSettings` | Updates seating/preparation/expedite/refund policy. |
| `frontend/src/routes/_app.audit.tsx` | Load/export/follow up audit | `/api/audit/logs`, `/api/audit/export`, `/api/audit/logs/{id}/follow-up` | `/api/audit` → identity | `AuditController.java` | `logs`, `export`, `followUp` | `AuditService.search`, `exportCsv`, `setFollowUp` | Audit screen. |

### 1.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| POST | `/api/auth/login` | `AuthController` | `login` | `LoginRequest(email,password,role,deviceId)` | `LoginResponse(token,expiresAt,user)` | Public auth endpoint. |
| POST | `/api/auth/logout` | `AuthController` | `logout` | Bearer token | `StatusResponse("signed_out")` | Protected. |
| GET | `/api/auth/me` | `AuthController` | `me` | Bearer token | `IdentityViews.UserView` | Current user. |
| GET | `/api/staff` | `StaffController` | `staff` | `search`, `shiftDate` | `IdentityViews.StaffView` | Requires `staff.manage`. |
| POST | `/api/shifts` | `StaffController` | `createShift` | `CreateShiftBody` | `ShiftView` | Audit + notification. |
| PATCH | `/api/staff/{userId}/roles` | `StaffController` | `updateRoles` | `UpdateRolesBody` | `StaffRowView` | Terminates target sessions. |
| GET | `/api/settings` | `SettingsController` | `settings` | none | settings map | Requires settings permission. |
| PATCH | `/api/settings` | `SettingsController` | `updateSettings` | `SettingsUpdateBody` | settings map | Updates policies. |
| GET | `/api/audit/logs` | `AuditController` | `logs` | filters | audit rows | Audit query. |
| GET | `/api/audit/export` | `AuditController` | `export` | filters | CSV/string response | Audit export. |
| PATCH | `/api/audit/logs/{auditLogId}/follow-up` | `AuditController` | `followUp` | `FollowUpRequest` | `AuditFollowUpResponse` | Marks follow-up. |
| GET | `/api/audit/events/failed-outbox` | `EventOpsController` | `failedOutbox` | none | failed outbox list | Event ops. |
| GET | `/api/audit/events/dead-letter` | `EventOpsController` | `deadLetters` | none | DLQ list | Event ops. |
| POST | `/api/audit/events/outbox/{eventId}/replay` | `EventOpsController` | `replayOutbox` | `ReplayBody` | `QueuedOperationResponse` | Replay. |
| POST | `/api/audit/events/consumer/{eventId}/{consumerName}/replay` | `EventOpsController` | `replayConsumer` | `ReplayBody` | `QueuedOperationResponse` | Replay. |
| GET | `/internal/identity/sessions/by-token-hash/{tokenHash}` | `IdentityInternalController` | `findActiveSessionByTokenHash` | token hash | `SessionPrincipal` | Internal service/session validation. |
| POST | `/internal/identity/sessions/{sessionId}/touch` | `IdentityInternalController` | `touchSession` | `TouchSessionRequest` | 204 | Internal session touch. |
| GET | `/internal/identity/policy-snapshot` | `IdentityInternalController` | `getPolicySnapshot` | none | `PolicySnapshot` | Used by reservation/billing/kitchen. |
| GET | `/internal/identity/default-branch` | `IdentityInternalController` | `findDefaultBranch` | none | `BranchView` | Used by reservation/inventory/billing. |
| POST | `/internal/identity/users/display-names` | `IdentityInternalController` | `findDisplayNames` | user IDs | `DisplayNameLookupResponse` | Used by reporting/read models. |
| POST | `/internal/identity/users/first-active-by-role-priority` | `IdentityInternalController` | `findFirstActiveUserIdByRolePriority` | role priority | `UserIdResponse` | Used by automation actor selection. |

### 1.4 Các file quan trọng và chức năng của chúng

#### 1.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `backend/src/main/java/SA/irms/runtime/identity/IdentityAuditServiceApplication.java` | Entry runtime, scan `SA.irms.identity`, `SA.irms.common`. |
| `backend/src/main/resources/application-identity-audit-service.properties` | Port `8088`, service name, RabbitMQ enabled, Flyway disabled. |
| `backend/src/main/java/SA/irms/common/security/*` | Token auth, current user, permission guard, internal token. |

#### 1.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `identity/api/AuthController.java` | `login`, `logout`, `me` | Public auth/session APIs. |
| `identity/api/IdentityInternalController.java` | `findActiveSessionByTokenHash`, `touchSession`, `getPolicySnapshot`, `findDefaultBranch` | Internal identity boundary. |
| `identity/api/StaffController.java` | `staff`, `createShift`, `updateRoles` | Staff/role/shift APIs. |
| `identity/api/SettingsController.java` | `settings`, `updateSettings` | Operational settings. |
| `identity/api/AuditController.java` | `logs`, `export`, `followUp` | Audit logs. |
| `identity/api/EventOpsController.java` | `failedOutbox`, `deadLetters`, `replayOutbox`, `replayConsumer` | Event operation APIs. |

#### 1.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `identity/application/AuthService.java` | `login`, `logout`, `currentUser` | Login/session/token flow. |
| `identity/application/IdentityBoundaryService.java` | internal identity port methods | Exposes session/policy/directory to other services. |
| `identity/application/StaffService.java` | `loadStaff`, `createShift`, `updateRoles` | Staff workflow, audit, notification. |
| `identity/application/SettingsService.java` | `loadSettings`, `updateSettings` | Policy/settings read/write. |
| `identity/audit/AuditService.java` | `record`, `search`, `setFollowUp`, `exportCsv` | Audit write/read/export. |
| `identity/application/EventOperationsService.java` | `failedOutbox`, `deadLetters`, replay methods | Event ops workflow. |

#### 1.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `identity/persistence/IdentityUserAuthRepository.java` | users/session auth | User lookup/update login. |
| `identity/persistence/IdentitySessionRepository.java` | user sessions | Create/touch/terminate session. |
| `identity/persistence/IdentityStaffRepository.java` | staff, roles, shifts | Staff write operations. |
| `identity/persistence/IdentityStaffReadRepository.java` | staff, roles, shifts | Staff read operations. |
| `identity/persistence/IdentityPolicyRepository.java` | policy tables | Policy snapshot and updates. |
| `identity/persistence/IdentityAuditRepository.java`, `IdentityAuditWriteRepository.java`, `IdentityAuditReadRepository.java` | audit log | Audit persistence. |
| `identity/persistence/IdentityEventOperationsRepository.java` | outbox/inbox/DLQ | Event operations query/replay support. |

#### 1.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `identity/infrastructure/messaging/AuditRecordingRequestedConsumer.java` | Event consumer | Consumes many service events and records audit via `AuditEventMaterializer`. |
| `identity/infrastructure/messaging/SessionTouchRequestedConsumer.java` | Event consumer | Handles session touch requested event. |
| `common/identity/RemoteSharedIdentitySessionClient.java` | Remote client | Other services call identity session APIs. |
| `common/identity/RemoteSharedIdentityPolicyClient.java` | Remote client | Other services call policy/default branch APIs. |
| `common/identity/RemoteSharedIdentityDirectoryClient.java` | Remote client | Other services resolve user display names/role priority user. |

### 1.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `AuthService.java` | `login` | email, password, role, deviceId, ip | `LoginResult` | Validate account/password/role, create token/session | `IdentityUserRepository`, `IdentitySessionRepositoryPort`, `SignedAccessTokenService`. |
| `AuthService.java` | `logout` | `AuthenticatedUser` | void | Terminate session | `identitySessionRepository.terminateSession`. |
| `AuthService.java` | `currentUser` | `AuthenticatedUser` | `UserView` | Reload account for active session | `identityUserRepository.findUserAccountById`. |
| `IdentityBoundaryService.java` | `findActiveSessionByTokenHash` | token hash | optional `SessionPrincipal` | Session validation for common security | `IdentitySessionRepositoryPort`. |
| `StaffService.java` | `createShift` | shift request, actor | `ShiftView` | Check overlap, create shift, audit, enqueue notification | `IdentityStaffRepositoryPort`, `AuditService`, `NotificationCommandPublisher`. |
| `StaffService.java` | `updateRoles` | userId, roleIds | `StaffRowView` | Validate roles, preserve last admin, replace roles, terminate sessions | `IdentityStaffRepositoryPort`, `IdentityUserRepository`, `AuditService`. |
| `SettingsService.java` | `updateSettings` | settings body, actor | settings map | Validate and update operational policies | `IdentityPolicyRepositoryPort`, `AuditService`. |

### 1.6 Các flow chính của service

#### Flow: Login

##### Mục đích
Xác thực user, tạo signed access token và session.

##### Trigger
FE login form trong `frontend/src/routes/login.tsx` gọi `useAuth().signIn`.

##### Step-by-step
1. FE gọi `POST /api/auth/login` qua `frontend/src/lib/auth.tsx`.
2. Gateway route `/api/auth/**` sang identity-audit-service.
3. `AuthController.login` nhận `LoginRequest`, lấy `User-Agent` hoặc `deviceId`, IP client.
4. `AuthService.login` tìm account bằng email, kiểm tra status active, password hash, role được request.
5. Service tính session expiry/access token expiry theo `AppProperties.security`.
6. `SignedAccessTokenService.issue` tạo token, `TokenHashingService.hash` hash token.
7. `IdentitySessionRepositoryPort.createSession` lưu session, `IdentityUserRepository.updateLastLogin` cập nhật last login.
8. Response trả `token`, `expiresAt`, `user`; FE lưu token vào `localStorage` key `irms.auth.token`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/lib/auth.tsx` | `signIn` | Build login body và gọi endpoint. |
| 2 | `gateway/GatewayRouteLocator.java` | route `/api/auth` | Chọn identity target. |
| 3 | `identity/api/AuthController.java` | `login` | API boundary. |
| 4 | `identity/application/AuthService.java` | `login` | Business auth/session. |
| 5 | `identity/persistence/IdentityUserAuthRepository.java` | user lookup/update | DB user auth. |
| 6 | `identity/persistence/IdentitySessionRepository.java` | `createSession` | DB session. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/auth/login` | `AuthController.java` | Public. |
| Data | user account/session | `IdentityUserAuthRepository.java`, `IdentitySessionRepository.java` | Password hash, session hash. |
| Event | Chưa tìm thấy event publish trực tiếp trong login source code | Chưa tìm thấy trong source code | Login chỉ ghi DB session/user. |

#### Flow: Logout

##### Mục đích
Đóng session hiện tại và clear token phía FE.

##### Trigger
FE `signOut` trong `frontend/src/lib/auth.tsx`.

##### Step-by-step
1. FE gọi `POST /api/auth/logout` bằng bearer token.
2. Gateway route sang identity.
3. `AuthController.logout` gọi `currentUser.require()`.
4. `AuthService.logout` gọi `identitySessionRepository.terminateSession`.
5. Response `signed_out`; FE clear local token dù request lỗi.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/lib/auth.tsx` | `signOut` | Call logout và clear token. |
| 2 | `identity/api/AuthController.java` | `logout` | API endpoint. |
| 3 | `identity/application/AuthService.java` | `logout` | Terminate session. |
| 4 | `identity/persistence/IdentitySessionRepository.java` | `terminateSession` | DB update. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/auth/logout` | `AuthController.java` | Protected. |
| Data | session termination | `IdentitySessionRepository.java` | Token không bị xóa ở FE cho tới finally. |

#### Flow: Get current user/me

##### Mục đích
Khôi phục auth state khi app reload và trả user/roles/permissions.

##### Trigger
`AuthProvider.refresh` khi app mount.

##### Step-by-step
1. FE đọc token localStorage, gọi `GET /api/auth/me`.
2. Gateway route sang identity.
3. Common security validate bearer token/session trước khi vào controller.
4. `AuthController.me` gọi `currentUser.require()`.
5. `AuthService.currentUser` reload account và trả `UserView`.
6. FE set `user` trong auth context.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/lib/auth.tsx` | `refresh` | Restore session. |
| 3 | `common/security/*` | token/session validation | Auth filter/current user. |
| 4 | `identity/api/AuthController.java` | `me` | Endpoint. |
| 5 | `identity/application/AuthService.java` | `currentUser` | Build current user response. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/auth/me` | `AuthController.java` | Protected. |
| Internal API | `/internal/identity/sessions/by-token-hash/{tokenHash}` | `IdentityInternalController.java` | Dùng bởi remote identity session client trong các runtime khác. |

#### Flow: Session/token validation

##### Mục đích
Cho các service validate bearer token qua identity boundary.

##### Trigger
Protected API request ở gateway/service có bearer token.

##### Step-by-step
1. FE gửi `Authorization: Bearer <token>` trong `apiFetch`.
2. Common security hash token và cần session principal.
3. Nếu đang trong identity runtime, `IdentityBoundaryService.findActiveSessionByTokenHash` đọc session local.
4. Nếu ở runtime khác, common remote client gọi `GET /internal/identity/sessions/by-token-hash/{tokenHash}`.
5. `IdentityInternalController.findActiveSessionByTokenHash` trả `SessionPrincipal` hoặc 404.
6. Common security tạo `AuthenticatedUser`, controller dùng `PermissionGuard.require(...)`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/lib/api/client.ts` | `apiFetch` | Gắn bearer token. |
| 3 | `identity/application/IdentityBoundaryService.java` | `findActiveSessionByTokenHash` | Local identity session lookup. |
| 4 | `common/identity/RemoteSharedIdentitySessionClient.java` | remote session methods | Service khác gọi identity. |
| 5 | `identity/api/IdentityInternalController.java` | `findActiveSessionByTokenHash` | Internal endpoint. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Internal API | `/internal/identity/sessions/by-token-hash/{tokenHash}` | `IdentityInternalController.java` | Chưa thấy FE gọi trực tiếp. |
| Internal API | `/internal/identity/sessions/{sessionId}/touch` | `IdentityInternalController.java` | Cập nhật last activity. |

#### Flow: Staff/role/permission

##### Mục đích
Quản lý staff, ca làm và role của user.

##### Trigger
FE staff screen `_app.staff.tsx`.

##### Step-by-step
1. FE load `GET /api/staff?search=&shiftDate=`.
2. `StaffController.staff` gọi `StaffService.loadStaff`.
3. `StaffService` đọc staff, roles, shifts từ `IdentityStaffRepositoryPort`.
4. Khi tạo shift, FE gọi `POST /api/shifts`.
5. `StaffService.createShift` kiểm tra overlap, tạo shift, ghi audit, enqueue notification.
6. Khi update roles, FE gọi `PATCH /api/staff/{userId}/roles`.
7. `StaffService.updateRoles` validate role IDs, giữ last admin, replace role, terminate active sessions target user, audit.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.staff.tsx` | staff query | FE caller. |
| 2 | `identity/api/StaffController.java` | `staff`, `createShift`, `updateRoles` | API boundary. |
| 3 | `identity/application/StaffService.java` | `loadStaff`, `createShift`, `updateRoles` | Staff business. |
| 4 | `identity/persistence/IdentityStaffRepository.java` | staff/shift/role writes | DB. |
| 5 | `common/notification/NotificationCommandPublisher.java` | `enqueue` | Shift assigned notification. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `/api/staff`, `/api/shifts`, `/api/staff/{userId}/roles` | `StaffController.java` | Public through gateway. |
| Data | users, roles, shifts | `IdentityStaffRepository.java` | Staff/role persistence. |
| Event/notification | `SHIFT_ASSIGNED` | `StaffService.java` | Notification command queued. |

#### Flow: Settings/audit

##### Mục đích
Settings cập nhật policy vận hành; audit đọc/export/follow-up log.

##### Trigger
FE `_app.settings.tsx`, `_app.audit.tsx`.

##### Step-by-step
1. FE settings gọi `GET /api/settings` hoặc `PATCH /api/settings`.
2. `SettingsController` gọi `SettingsService`.
3. `SettingsService.loadSettings` đọc default branch + policy snapshot.
4. `SettingsService.updateSettings` validate values, update seating/preparation/expedite/refund window policies, ghi audit.
5. FE audit gọi `/api/audit/logs`, `/api/audit/export`, `/api/audit/logs/{id}/follow-up`.
6. `AuditController` gọi `AuditService.search`, `exportCsv`, `setFollowUp`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.settings.tsx` | settings query/mutation | FE settings. |
| 2 | `identity/api/SettingsController.java` | `settings`, `updateSettings` | API. |
| 3 | `identity/application/SettingsService.java` | `loadSettings`, `updateSettings` | Policy logic. |
| 5 | `frontend/src/routes/_app.audit.tsx` | audit query/export/follow-up | FE audit. |
| 6 | `identity/api/AuditController.java` | `logs`, `export`, `followUp` | Audit API. |
| 7 | `identity/audit/AuditService.java` | `search`, `exportCsv`, `setFollowUp` | Audit service. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Data | policy snapshot | `IdentityPolicyRepository.java` | Seating/preparation/expedite/authorization/tax. |
| Data | audit logs | `IdentityAuditRepository.java` | Search/export/follow-up. |
| Event consumer | audit materialization | `AuditRecordingRequestedConsumer.java` | Consumes service events into audit. |

## 2. Ordering Service

### 2.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Menu/order overview, create order/draft, confirm draft, cancel order/item, mark item served/delayed/send, menu/category/item/promotion/combo management. |
| API public chính | `/api/orders/**`, `/api/menu/**`. |
| Internal API nếu có | `/internal/ordering/orders/{orderId}/refresh-status`, `/internal/ordering/orders/{orderId}/line-statuses`, `/internal/ordering/promotions/apply`. |
| Service khác gọi vào | Kitchen sync status về ordering; billing apply promotion qua ordering. |
| Service này gọi ra | Kitchen internal API qua `RemoteKitchenOrderRoutingClient`; identity policy/session qua common clients; publish order/menu events. |
| FE màn hình/flow liên quan | `_app.orders.tsx`, `_app.menu.tsx`, `_app.billing.tsx` apply promotion indirectly. |

### 2.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `_app.orders.tsx` | Load POS/menu/order overview | `GET /api/orders/overview?sessionId=` | `/api/orders` → ordering | `OrdersController.java` | `overview` | `OrdersService.load` | Loads sessions, menu items, ordered items, combos. |
| `_app.orders.tsx` | Create order | `POST /api/orders` with `draft=false` | `/api/orders` → ordering | `OrdersController.java` | `createOrder` | `OrdersService.createOrder` → `OrderCreationService.createOrder` | Publishes `OrderConfirmedEvent`; async consumer routes to kitchen. |
| `_app.orders.tsx` | Create draft order | `POST /api/orders` with `draft=true` | `/api/orders` → ordering | `OrdersController.java` | `createOrder` | `OrderCreationService.createOrder` | Saves draft and audits, no kitchen route. |
| `_app.orders.tsx` | Confirm draft | `POST /api/orders/{orderId}/confirm` | `/api/orders` → ordering | `OrdersController.java` | `confirmDraftOrder` | `OrderDraftConfirmationService.confirmDraftOrder` | Publishes `OrderConfirmedEvent`. |
| `_app.orders.tsx` | Cancel order | `POST /api/orders/{orderId}/cancel` | `/api/orders` → ordering | `OrdersController.java` | `cancelOrder` | `OrderItemWorkflowService.cancelOrder` | Publishes `OrderCancelledEvent`. |
| `_app.orders.tsx` | Mark served | `PATCH /api/orders/items/{orderItemId}/served` | `/api/orders` → ordering | `OrdersController.java` | `markServed` | `OrderItemWorkflowService.markServed` | Updates order item and refreshes order. |
| `_app.orders.tsx` | Mark delayed | `PATCH /api/orders/items/{orderItemId}/delayed` | `/api/orders` → ordering | `OrdersController.java` | `markDelayed` | `OrderItemWorkflowService.markDelayed` | Calls kitchen hold. |
| `_app.orders.tsx` | Send delayed item | `POST /api/orders/items/{orderItemId}/send` | `/api/orders` → ordering | `OrdersController.java` | `sendDelayedItem` | `OrderItemWorkflowService.sendDelayedItem` | Calls kitchen release. |
| `_app.orders.tsx` | Cancel item | `POST /api/orders/items/{orderItemId}/cancel` | `/api/orders` → ordering | `OrdersController.java` | `cancelItem` | `OrderItemWorkflowService.cancelOrderItem` | Calls kitchen block. |
| `_app.menu.tsx` | Manage menu | `/api/menu/**` | `/api/menu` → ordering | `MenuController`, `MenuItemController`, `MenuCategoryController`, `MenuPromotionController` | multiple | `MenuService.*` | Category/item/combo/promotion CRUD. |

### 2.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| GET | `/api/orders/overview` | `OrdersController` | `overview` | `sessionId?` | `OrdersOverview` | Public via gateway. |
| POST | `/api/orders` | `OrdersController` | `createOrder` | `CreateOrderBody` | `OrdersOverview` | Supports `draft`. |
| POST | `/api/orders/{orderId}/confirm` | `OrdersController` | `confirmDraftOrder` | path | `OrdersOverview` | Draft only. |
| POST | `/api/orders/{orderId}/cancel` | `OrdersController` | `cancelOrder` | `CancelBody` | `EntityStatusResponse` | Order cancellation. |
| PATCH | `/api/orders/items/{orderItemId}/served` | `OrdersController` | `markServed` | path | `OrderedItemView` | Service-side served. |
| PATCH | `/api/orders/items/{orderItemId}/delayed` | `OrdersController` | `markDelayed` | path | `OrderedItemView` | Holds in kitchen. |
| POST | `/api/orders/items/{orderItemId}/send` | `OrdersController` | `sendDelayedItem` | path | `OrderedItemView` | Releases held/delayed item. |
| POST | `/api/orders/items/{orderItemId}/cancel` | `OrdersController` | `cancelItem` | `CancelBody` | `OrderedItemView` | Cancels item. |
| GET | `/api/menu` | `MenuController` | `overview` | none | `MenuOverview` | Menu management overview. |
| GET | `/api/menu/combos` | `MenuController` | `combos` | none | combo list | Combo list. |
| GET | `/api/menu/combos/{comboId}` | `MenuController` | `combo` | path | combo | Combo detail. |
| POST | `/api/menu/combos` | `MenuController` | `createCombo` | `ComboUpsert` | combo | Combo create. |
| PUT | `/api/menu/combos/{comboId}` | `MenuController` | `updateCombo` | `ComboUpsert` | combo | Combo update. |
| GET/POST/PUT/DELETE | `/api/menu/categories/**` | `MenuCategoryController` | `categories`, `category`, `createCategory`, `updateCategory`, `deleteCategory` | category body/path | category/reference | Category CRUD. |
| GET/POST/PUT/PATCH/DELETE | `/api/menu/items/**` | `MenuItemController` | `items`, `item`, `createItem`, `updateItem`, `toggleAvailability`, `deleteItem` | item body/path | item/reference | Item CRUD/availability. |
| GET/POST/PUT/DELETE | `/api/menu/promotions/**` | `MenuPromotionController` | `promotions`, `promotion`, `createPromotion`, `updatePromotion`, `deletePromotion` | promotion body/path | promotion/reference | Promotion CRUD. |
| POST | `/internal/ordering/orders/{orderId}/refresh-status` | `OrderingIntegrationController` | `refreshOrder` | path | 202 | Called by kitchen. |
| POST | `/internal/ordering/orders/{orderId}/line-statuses` | `OrderingIntegrationController` | `applyKitchenLineStatuses` | `LineStatusRequest` | 202 | Called by kitchen. |
| POST | `/internal/ordering/promotions/apply` | `OrderingIntegrationController` | `applyPromotion` | `PromotionRequest` | `PromotionResponse` | Called by billing. |

### 2.4 Các file quan trọng và chức năng của chúng

#### 2.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `runtime/ordering/OrderingServiceApplication.java` | Entry service, scan ordering + common. |
| `application-ordering-service.properties` | Port `8081`, kitchen/identity remote URL. |

#### 2.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `ordering/api/OrdersController.java` | order methods | Order POS APIs. |
| `ordering/api/MenuController.java` | combo/menu overview | Menu and combo APIs. |
| `ordering/api/MenuItemController.java` | item CRUD/availability | Menu item management. |
| `ordering/api/MenuCategoryController.java` | category CRUD | Menu category management. |
| `ordering/api/MenuPromotionController.java` | promotion CRUD | Promotion management. |
| `ordering/api/OrderingIntegrationController.java` | internal sync/promotion | Kitchen/billing internal API. |

#### 2.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `ordering/application/OrdersService.java` | facade methods | Delegates to read/create/confirm/item workflow. |
| `ordering/application/OrderCreationService.java` | `createOrder` | Create draft or confirmed order. |
| `ordering/application/OrderDraftConfirmationService.java` | `confirmDraftOrder` | Confirm draft and publish order event. |
| `ordering/application/OrderItemWorkflowService.java` | item/order lifecycle methods | Served/delayed/send/cancel/order cancel. |
| `ordering/application/MenuService.java` | menu CRUD methods | Menu, combo, promotion logic. |
| `ordering/application/OrderStateCoordinator.java` | `refreshOrder`, `applyKitchenLineStatuses` | Sync kitchen line statuses into ordering. |
| `ordering/application/workflow/OrderFulfillmentMediator.java` | `handleOrderConfirmed`, `handleDishStatusChanged`, `handleOrderCancelled` | Event workflow after order/kitchen events. |
| `ordering/application/workflow/OrderKitchenRoutingStep.java` | `execute` | Calls kitchen for confirmed items. |

#### 2.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `ordering/infrastructure/persistence/JdbcOrderRepository.java` | orders, order_items, menu, promotion | Main ordering writes. |
| `JdbcOrderQueryRepository.java` | orders/menu read models | Loads overview, menu, draft, order item routing. |
| `JdbcOrderLifecycleRepository.java` | order lifecycle | Status/order lifecycle updates. |
| `JdbcOrderItemCommandRepository.java` | order items | Item command updates. |
| `JdbcDraftOrderQueryRepository.java` | draft orders | Draft reads. |
| `JdbcMenuCatalogQueryRepository.java`, `JdbcComboCatalogQueryRepository.java` | menu/combo catalog | Menu read operations. |
| `JdbcPromotionCommandRepository.java` | promotions | Promotion writes. |

#### 2.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `ordering/integration/RemoteKitchenOrderRoutingClient.java` | Remote client | Calls `/internal/kitchen/**`. |
| `ordering/application/workflow/OrderConfirmedConsumer.java` | Event consumer | Consumes order confirmed, kitchen dish status, order cancelled. |
| `ordering/application/events/OrderConfirmedEvent.java` | Event | Order confirmation event. |
| `ordering/application/events/OrderCancelledEvent.java` | Event | Order cancellation event. |
| `ordering/application/workflow/OrderReadyNotificationStep.java` | Event publisher | Publishes ready notification when kitchen dish ready. |

### 2.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `OrdersService.java` | `createOrder` | `CreateOrderRequest`, actor | `OrdersOverview` | Facade create order | `OrderCreationService.createOrder`. |
| `OrderCreationService.java` | `createOrder` | order request | `OrdersOverview` | Validate, persist order/items/combos; if non-draft publish `OrderConfirmedEvent` | `OrderPersistenceCoordinator`, `OrderCreationAuditPublisher`, `OrderSnapshotService`. |
| `OrderDraftConfirmationService.java` | `confirmDraftOrder` | orderId, actor | `OrdersOverview` | Reload draft, validate menu/modifiers, mark confirmed, publish event | `OrderRepository`, `DomainEventPublisher`. |
| `OrderItemWorkflowService.java` | `markDelayed` | orderItemId | `OrderedItemView` | Mark delayed and hold item in kitchen | `KitchenCoordinationPort.holdOrderItemForService`. |
| `OrderItemWorkflowService.java` | `sendDelayedItem` | orderItemId | `OrderedItemView` | Release held item to kitchen and mark sent | `KitchenCoordinationPort.releaseHeldOrderItem`. |
| `OrderItemWorkflowService.java` | `cancelOrderItem` | orderItemId, reason | `OrderedItemView` | Cancel item, block in kitchen, maybe publish order cancelled | `KitchenCoordinationPort.blockOrderItem`, `DomainEventPublisher`. |
| `OrderItemWorkflowService.java` | `cancelOrder` | orderId, reason | void | Cancel active items/order and publish event | `OrderRepository`, `DomainEventPublisher`. |
| `OrderStateCoordinator.java` | `applyKitchenLineStatuses` | orderId, statuses | void | Sync kitchen statuses into order item line status | `OrderRepository`. |
| `RemoteKitchenOrderRoutingClient.java` | `queueConfirmedItems` | orderId, items, notes | void | Remote call to kitchen ticket creation | `POST /internal/kitchen/orders/{orderId}/tickets`. |

### 2.6 Các flow chính của service

#### Flow: Load menu/order overview

##### Mục đích
Load POS screen gồm sessions, menu items, combos, ordered items.

##### Trigger
FE `_app.orders.tsx` query `["orders", selectedSessionId]`.

##### Step-by-step
1. FE gọi `GET /api/orders/overview?sessionId=...`.
2. Gateway route sang ordering.
3. `OrdersController.overview` require `orders.create`.
4. `OrdersService.load` gọi `OrdersReadService.load`.
5. Repository đọc table sessions, menu items, combos, ordered items.
6. Response `OrdersOverview` được FE map qua `mapOrdersData`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.orders.tsx` | `useQuery` | FE load overview. |
| 3 | `ordering/api/OrdersController.java` | `overview` | API. |
| 4 | `ordering/application/OrdersService.java` | `load` | Service facade. |
| 5 | `ordering/application/OrdersReadService.java` | `load` | Build overview. |
| 5 | `ordering/infrastructure/persistence/JdbcOrderQueryRepository.java` | `loadTableSessions`, `loadMenuItemsForOrdering`, `loadOrderedItems` | DB reads. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/orders/overview` | `OrdersController.java` | Public. |
| Data | table sessions/menu/order items | `JdbcOrderQueryRepository.java` | Shared DB read model. |

#### Flow: Create order

##### Mục đích
Tạo order confirmed và phát event để chuyển món sang kitchen.

##### Trigger
FE `_app.orders.tsx` submit cart với `draft=false`.

##### Step-by-step
1. FE gọi `POST /api/orders` với `sessionId`, `items`, `comboSelections`, `draft=false`.
2. Gateway route sang ordering.
3. `OrdersController.createOrder` require `orders.send`, map body sang `CreateOrderRequest`.
4. `OrdersService.createOrder` gọi `OrderCreationService.createOrder`.
5. `OrderCreationService` validate request, tạo `orderId`, persist order/items/combos qua `OrderPersistenceCoordinator`.
6. Vì không draft, service upsert snapshot, publish `OrderConfirmedEvent`, audit confirmed order.
7. Response trả `OrdersOverview`.
8. RabbitMQ consumer `OrderConfirmedConsumer.consumeOrderConfirmed` gọi `OrderFulfillmentMediator.handleOrderConfirmed`.
9. `OrderKitchenRoutingStep.execute` gọi `RemoteKitchenOrderRoutingClient.queueConfirmedItems`.
10. Kitchen nhận `/internal/kitchen/orders/{orderId}/tickets`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.orders.tsx` | `createOrderMutation` | FE submit order. |
| 3 | `ordering/api/OrdersController.java` | `createOrder` | API. |
| 4 | `ordering/application/OrdersService.java` | `createOrder` | Facade. |
| 5 | `ordering/application/OrderCreationService.java` | `createOrder` | Business create. |
| 5 | `ordering/application/OrderPersistenceCoordinator.java` | `createOrder` | Persist order data. |
| 6 | `ordering/application/OrderCreationAuditPublisher.java` | `publishOrderConfirmed`, `auditConfirmedOrder` | Event/audit. |
| 8 | `ordering/application/workflow/OrderConfirmedConsumer.java` | `consumeOrderConfirmed` | Event consumer. |
| 9 | `ordering/application/workflow/OrderKitchenRoutingStep.java` | `execute` | Route to kitchen. |
| 9 | `ordering/integration/RemoteKitchenOrderRoutingClient.java` | `queueConfirmedItems` | Remote call. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/orders` | `OrdersController.java` | Public. |
| Event | `OrderConfirmedEvent` | `ordering/application/events/OrderConfirmedEvent.java` | Drives kitchen routing/reporting/audit. |
| Remote call | `POST /internal/kitchen/orders/{orderId}/tickets` | `RemoteKitchenOrderRoutingClient.java` | To kitchen. |

#### Flow: Create draft order

##### Mục đích
Lưu order nháp, chưa gửi kitchen.

##### Trigger
FE `_app.orders.tsx` submit cart với `draft=true`.

##### Step-by-step
1. FE gọi `POST /api/orders` với `draft=true`.
2. `OrdersController.createOrder` gọi `OrdersService.createOrder`.
3. `OrderCreationService.createOrder` persist order/items với trạng thái draft.
4. Nhánh `request.draft()` chỉ gọi `auditPublisher.auditDraftOrder`.
5. Không gọi `publishOrderConfirmed`, không route kitchen.
6. Response trả overview mới.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.orders.tsx` | `createOrderMutation(true)` | FE draft. |
| 2 | `ordering/api/OrdersController.java` | `createOrder` | API. |
| 3 | `ordering/application/OrderCreationService.java` | `createOrder` | Draft branch. |
| 4 | `ordering/application/OrderCreationAuditPublisher.java` | `auditDraftOrder` | Audit. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/orders` | `OrdersController.java` | `draft=true`. |
| Event | Kitchen routing event | Chưa tìm thấy trong source code | Draft không publish order confirmed. |

#### Flow: Confirm order

##### Mục đích
Chuyển draft order thành confirmed, refresh menu/modifier price, gửi kitchen qua event.

##### Trigger
FE action confirm draft trong `_app.orders.tsx`.

##### Step-by-step
1. FE gọi `POST /api/orders/{orderId}/confirm`.
2. `OrdersController.confirmDraftOrder` require `orders.send`.
3. `OrderDraftConfirmationService.confirmDraftOrder` load draft order/items.
4. Service validate status `draft`, item không empty.
5. `refreshDraftItems` reload menu item, modifiers, update draft item price/line status `sent_to_kitchen`.
6. Upsert snapshot, `repository.confirmDraftOrder`, publish `OrderConfirmedEvent`, audit.
7. Event consumer route sang kitchen giống create confirmed order.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.orders.tsx` | action mutation | FE confirm. |
| 2 | `ordering/api/OrdersController.java` | `confirmDraftOrder` | API. |
| 3 | `ordering/application/OrderDraftConfirmationService.java` | `confirmDraftOrder` | Confirm draft. |
| 5 | `ordering/application/OrderMenuSelectionService.java` | `requireAvailableMenuItem`, `resolveModifierSelection` | Validate menu/modifiers. |
| 6 | `ordering/infrastructure/persistence/JdbcOrderRepository.java` | `confirmDraftOrder`, update item methods | DB writes. |
| 7 | `ordering/application/workflow/OrderKitchenRoutingStep.java` | `execute` | Kitchen routing. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/orders/{orderId}/confirm` | `OrdersController.java` | Public. |
| Event | `OrderConfirmedEvent` | `OrderDraftConfirmationService.java` | Async route to kitchen. |

#### Flow: Add/update/cancel item

##### Mục đích
FE thêm item/combo vào cart trước khi tạo order; backend xử lý cancel/served/delayed/send item sau khi item tồn tại.

##### Trigger
FE `_app.orders.tsx`.

##### Step-by-step
1. Add/update quantity/note/modifier/combo trong FE là state local cart, chưa gọi backend.
2. Khi submit order, FE gửi toàn bộ cart qua `POST /api/orders`.
3. Backend persist từng item trong `OrderPersistenceCoordinator`.
4. Cancel item sau khi order tồn tại: FE gọi `POST /api/orders/items/{orderItemId}/cancel`.
5. `OrderItemWorkflowService.cancelOrderItem` validate policy, cancel DB item, gọi kitchen block, refresh order, audit.
6. Update quantity/item sau khi order đã tạo: Chưa tìm thấy API update quantity/item trực tiếp trong source code.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.orders.tsx` | `addToCart`, `updateQty`, `toggleModifier`, `addComboToCart` | Local FE cart. |
| 2 | `frontend/src/routes/_app.orders.tsx` | `createOrderMutation` | Submit order. |
| 3 | `ordering/application/OrderPersistenceCoordinator.java` | `createOrder` | Persist order items. |
| 4 | `ordering/api/OrdersController.java` | `cancelItem` | API cancel item. |
| 5 | `ordering/application/OrderItemWorkflowService.java` | `cancelOrderItem` | Cancel + kitchen block. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/orders/items/{orderItemId}/cancel` | `OrdersController.java` | Cancel item. |
| Remote call | `/internal/kitchen/order-items/{orderItemId}/block` | `RemoteKitchenOrderRoutingClient.java` | Block kitchen item. |
| Gap | Update item quantity after create | Chưa tìm thấy trong source code | Không thấy API update item trực tiếp. |

#### Flow: Cancel order

##### Mục đích
Cancel toàn bộ order đang active và phát event cho kitchen/billing/reporting/notification/audit.

##### Trigger
FE `_app.orders.tsx` cancel order action.

##### Step-by-step
1. FE gọi `POST /api/orders/{orderId}/cancel` với reason.
2. `OrdersController.cancelOrder` require `orders.edit`.
3. `OrderItemWorkflowService.cancelOrder` load current order status.
4. Nếu status `cancelled` hoặc `completed` thì throw conflict.
5. Repository cancel active order items, mark order cancelled.
6. Publish `OrderCancelledEvent`, audit.
7. Consumers như kitchen/billing/inventory/reporting/notification xử lý event theo queue của từng service.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.orders.tsx` | action mutation | FE cancel. |
| 2 | `ordering/api/OrdersController.java` | `cancelOrder` | API. |
| 3 | `ordering/application/OrderItemWorkflowService.java` | `cancelOrder` | Business cancel. |
| 5 | `ordering/infrastructure/persistence/JdbcOrderRepository.java` | cancel methods | DB updates. |
| 6 | `ordering/application/events/OrderCancelledEvent.java` | event | Cross-service event. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/orders/{orderId}/cancel` | `OrdersController.java` | Public. |
| Event | `OrderCancelledEvent` | `OrderItemWorkflowService.java` | Cross-service. |

#### Flow: Combo/promotion

##### Mục đích
Quản lý combo/promotion và apply promotion cho billing.

##### Trigger
FE menu screen hoặc billing apply promo.

##### Step-by-step
1. FE menu gọi `/api/menu/combos`, `/api/menu/promotions`, `/api/menu/items`, `/api/menu/categories`.
2. Gateway route `/api/menu/**` sang ordering.
3. Menu controllers gọi `MenuService` để CRUD combo/promotion/item/category.
4. Billing apply promotion gọi internal `POST /internal/ordering/promotions/apply`.
5. `OrderingIntegrationController.applyPromotion` gọi `MenuService.applyPromotion`.
6. Response trả discount cho billing.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.menu.tsx` | menu API calls | FE menu management. |
| 3 | `ordering/api/MenuController.java` | combo methods | Combo API. |
| 3 | `ordering/api/MenuPromotionController.java` | promotion methods | Promotion API. |
| 4 | `billing/integration/RemotePromotionApplicationClient.java` | `applyPromotion` | Billing remote caller. |
| 5 | `ordering/api/OrderingIntegrationController.java` | `applyPromotion` | Internal API. |
| 5 | `ordering/application/MenuService.java` | `applyPromotion` | Promotion logic. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `/api/menu/**` | Menu controllers | Public. |
| Internal API | `POST /internal/ordering/promotions/apply` | `OrderingIntegrationController.java` | Billing → ordering. |

#### Flow: Gửi order/item sang kitchen

##### Mục đích
Route confirmed items sang kitchen theo station.

##### Trigger
`OrderConfirmedEvent` sau create confirmed hoặc confirm draft.

##### Step-by-step
1. Ordering publish `OrderConfirmedEvent`.
2. `OrderConfirmedConsumer.consumeOrderConfirmed` nhận event.
3. `OrderFulfillmentMediator.handleOrderConfirmed` chạy workflow.
4. `OrderKitchenRoutingStep.execute` extract `items` từ payload.
5. `RemoteKitchenOrderRoutingClient.queueConfirmedItems` gọi kitchen internal API.
6. Kitchen tạo ticket theo station.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `OrderCreationService.java`, `OrderDraftConfirmationService.java` | publish event | Source event. |
| 2 | `ordering/application/workflow/OrderConfirmedConsumer.java` | `consumeOrderConfirmed` | Consumer. |
| 3 | `ordering/application/workflow/OrderFulfillmentMediator.java` | `handleOrderConfirmed` | Workflow. |
| 4 | `ordering/application/workflow/OrderKitchenRoutingStep.java` | `execute` | Build kitchen commands. |
| 5 | `ordering/integration/RemoteKitchenOrderRoutingClient.java` | `queueConfirmedItems` | Remote call. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Event | `OrderConfirmedEvent` | `ordering/application/events` | Event-driven trigger. |
| Remote call | `/internal/kitchen/orders/{orderId}/tickets` | `RemoteKitchenOrderRoutingClient.java` | To kitchen. |

#### Flow: Nhận sync status từ kitchen

##### Mục đích
Kitchen cập nhật trạng thái line item/order về ordering.

##### Trigger
Kitchen reconcile ticket hoặc item status change.

##### Step-by-step
1. Kitchen gọi `RemoteOrderStateUpdateClient.applyKitchenLineStatuses`.
2. Remote call tới `POST /internal/ordering/orders/{orderId}/line-statuses`.
3. `OrderingIntegrationController.applyKitchenLineStatuses` nhận list status.
4. `OrderStateCoordinator.applyKitchenLineStatuses` cập nhật line statuses.
5. Khi cần refresh tổng order, kitchen gọi `POST /internal/ordering/orders/{orderId}/refresh-status`.
6. `OrderStateCoordinator.refreshOrder` recompute order status.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `kitchen/integration/RemoteOrderStateUpdateClient.java` | `applyKitchenLineStatuses`, `refreshOrder` | Remote caller. |
| 3 | `ordering/api/OrderingIntegrationController.java` | `applyKitchenLineStatuses`, `refreshOrder` | Internal receiver. |
| 4 | `ordering/application/OrderStateCoordinator.java` | `applyKitchenLineStatuses`, `refreshOrder` | Sync logic. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Internal API | `/internal/ordering/orders/{orderId}/line-statuses` | `OrderingIntegrationController.java` | Kitchen → ordering. |
| Internal API | `/internal/ordering/orders/{orderId}/refresh-status` | `OrderingIntegrationController.java` | Kitchen → ordering. |

## 3. Kitchen Service

### 3.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Nhận confirmed order items, tạo kitchen route plan/tickets/items, quản lý queue/status/priority/cancel/serve/return, trừ inventory khi nấu, sync status về ordering, publish notification/event. |
| API public chính | `/api/kitchen/overview`, `/api/kitchen/tickets/**`, `/api/kitchen/items/**`. |
| Internal API nếu có | `/internal/kitchen/orders/{orderId}/tickets`, `/internal/kitchen/order-items/{orderItemId}/hold`, `/internal/kitchen/orders/{orderId}/items/release`, `/internal/kitchen/order-items/{orderItemId}/block`. |
| Service khác gọi vào | Ordering gọi internal kitchen để queue/hold/release/block items. |
| Service này gọi ra | Ordering internal status sync; inventory internal consumption; notification/outbox events. |
| FE màn hình/flow liên quan | `_app.kitchen.tsx`, `_app.orders.tsx` via cross-service. |

### 3.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `_app.kitchen.tsx` | Load kitchen queue | `GET /api/kitchen/overview?station=` | `/api/kitchen` → kitchen | `KitchenController.java` | `overview` | `KitchenService.load` | Reads tickets. |
| `_app.kitchen.tsx` | Update ticket status | `PATCH /api/kitchen/tickets/{ticketId}/status` | `/api/kitchen` → kitchen | `KitchenController.java` | `updateTicketStatus` | `KitchenService.updateTicketStatus` | Can move queued items to cooking. |
| `_app.kitchen.tsx` | Update item status | `PATCH /api/kitchen/items/{ticketItemId}/status` | `/api/kitchen` → kitchen | `KitchenController.java` | `updateItemStatus` | `KitchenService.updateItemStatus` | cooking/ready/blocked path. |
| `_app.kitchen.tsx` | Toggle priority | `POST /api/kitchen/tickets/{ticketId}/priority` | `/api/kitchen` → kitchen | `KitchenController.java` | `togglePriority` | `KitchenPriorityService.togglePriority` | Priority/audit. |
| `_app.kitchen.tsx` | Cancel ticket | `POST /api/kitchen/tickets/{ticketId}/cancel` | `/api/kitchen` → kitchen | `KitchenController.java` | `cancelTicket` | `KitchenTicketCancellationService.cancelTicket` | Blocks open items. |
| `_app.kitchen.tsx` | Serve ticket | `POST /api/kitchen/tickets/{ticketId}/serve` | `/api/kitchen` → kitchen | `KitchenController.java` | `serveTicket` | `KitchenTicketHandoffService.serveTicket` | Handoff/served. |
| `_app.kitchen.tsx` | Return ticket | `POST /api/kitchen/tickets/{ticketId}/return` | `/api/kitchen` → kitchen | `KitchenController.java` | `returnTicket` | `KitchenTicketHandoffService.returnTicket` | Returned flow. |
| Chưa tìm thấy FE caller trực tiếp trong frontend source. | Ordering routes confirmed items | `POST /internal/kitchen/orders/{orderId}/tickets` | Internal, không qua gateway FE | `KitchenIntegrationController.java` | `queueConfirmedItems` | `KitchenOrderRoutingService.queueConfirmedItems` | Service-to-service. |

### 3.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| GET | `/api/kitchen/overview` | `KitchenController` | `overview` | `station?` | `KitchenOverview` | Public via gateway. |
| PATCH | `/api/kitchen/tickets/{ticketId}/status` | `KitchenController` | `updateTicketStatus` | `StatusBody` | `TicketView` | Ticket transition. |
| PATCH | `/api/kitchen/items/{ticketItemId}/status` | `KitchenController` | `updateItemStatus` | `StatusBody(status,reason)` | `TicketView` | Item transition. |
| POST | `/api/kitchen/tickets/{ticketId}/priority` | `KitchenController` | `togglePriority` | `StatusBody` | `TicketView` | Priority. |
| POST | `/api/kitchen/tickets/{ticketId}/cancel` | `KitchenController` | `cancelTicket` | `StatusBody` | `TicketView` | Ticket cancel. |
| POST | `/api/kitchen/tickets/{ticketId}/serve` | `KitchenController` | `serveTicket` | path | `TicketView` | Serve ready ticket. |
| POST | `/api/kitchen/tickets/{ticketId}/return` | `KitchenController` | `returnTicket` | `StatusBody` | `TicketView` | Return served ticket. |
| POST | `/internal/kitchen/orders/{orderId}/tickets` | `KitchenIntegrationController` | `queueConfirmedItems` | `QueueItemsRequest` | 202 | Ordering → kitchen. |
| POST | `/internal/kitchen/order-items/{orderItemId}/hold` | `KitchenIntegrationController` | `holdOrderItemForService` | path | 202 | Ordering delayed item. |
| POST | `/internal/kitchen/orders/{orderId}/items/release` | `KitchenIntegrationController` | `releaseHeldOrderItem` | `ReleaseItemRequest` | 202 | Ordering sends held item. |
| POST | `/internal/kitchen/order-items/{orderItemId}/block` | `KitchenIntegrationController` | `blockOrderItem` | `BlockItemRequest` | 202 | Ordering item cancel. |

### 3.4 Các file quan trọng và chức năng của chúng

#### 3.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `runtime/kitchen/KitchenServiceApplication.java` | Entry service, scan kitchen + common. |
| `application-kitchen-service.properties` | Port `8082`, ordering/inventory/identity URLs. |

#### 3.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `kitchen/api/KitchenController.java` | overview/status/priority/cancel/serve/return | Public kitchen APIs. |
| `kitchen/api/KitchenIntegrationController.java` | queue/hold/release/block | Internal ordering → kitchen APIs. |

#### 3.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `kitchen/application/KitchenOrderRoutingService.java` | `queueConfirmedItems`, `holdOrderItemForService`, `releaseHeldOrderItem`, `blockOrderItem` | Create tickets by station and manage held/blocked item. |
| `kitchen/application/KitchenService.java` | `load`, `updateTicketStatus`, `updateItemStatus` | Public kitchen workflow facade. |
| `kitchen/application/KitchenTicketItemWorkflowService.java` | `moveItemToCooking`, `markItemReady`, `blockItem` | Item lifecycle; inventory + events + notification. |
| `kitchen/application/KitchenTicketEventNotifier.java` | `publishDishStatus`, `queueReadyNotification` | Publishes dish status event and ready notification command. |
| `kitchen/application/KitchenTicketHandoffService.java` | `serveTicket`, `returnTicket` | Handoff/serve/return flow. |
| `kitchen/application/KitchenTicketCancellationService.java` | `cancelTicket` | Cancel/block ticket flow. |
| `kitchen/application/KitchenPriorityService.java` | `togglePriority`, auto actor methods | Manual/automatic priority. |
| `kitchen/application/workflow/KitchenWorkflowMediator.java` | `handleDishStatusChanged`, `transitionTicket`, `handleOrderCancelled` | Event workflow. |

#### 3.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `kitchen/infrastructure/persistence/JdbcKitchenOrderRoutingRepository.java` | route plan, tickets, ticket items | Create route/ticket/ticket item, hold/release/block. |
| `JdbcKitchenTicketQueryRepository.java` | kitchen ticket read model | Load overview/ticket. |
| `JdbcKitchenTicketItemRepository.java` | ticket items | Load item context. |
| `JdbcKitchenTicketItemCommandAdapter.java` | ticket item commands | Move cooking, mark ready, block, inventory deducted. |
| `JdbcKitchenTicketStateCoordinator.java` | ticket/order status projection | Reconcile ticket and sync ordering after commit. |
| `JdbcKitchenTicketHandoffCommandAdapter.java` | handoff/ticket status | Serve/return ticket writes. |
| `JdbcKitchenTicketCancellationCommandAdapter.java` | ticket cancel | Block open items/ticket. |

#### 3.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `kitchen/integration/RemoteOrderStateUpdateClient.java` | Remote client | Calls `/internal/ordering/**` to sync order status. |
| `kitchen/integration/RemoteInventoryConsumptionClient.java` | Remote client | Calls `/internal/inventory/consumption/kitchen-start`. |
| `kitchen/application/events/KitchenTicketCreatedEvent.java` | Event publisher | Published when ticket created. |
| `kitchen/application/events/KitchenDishStatusChangedEvent.java` | Event publisher | Published on item status changes. |
| `kitchen/application/workflow/KitchenDishStatusChangedConsumer.java` | Event consumer | Handles dish status event. |
| `kitchen/application/workflow/KitchenOrderCancelledConsumer.java` | Event consumer | Handles order cancelled event. |

### 3.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `KitchenOrderRoutingService.java` | `queueConfirmedItems` | orderId, items, notes | void | Group items by station, create route/tickets/items, publish ticket created event | `KitchenOrderRoutingRepository`, `DomainEventPublisher`. |
| `KitchenService.java` | `updateTicketStatus` | ticketId, targetStatus | `TicketView` | Validate ticket transition; cooking moves queued items to cooking | `KitchenTicketItemWorkflowService`, `KitchenTicketStateCoordinatorPort`. |
| `KitchenService.java` | `updateItemStatus` | ticketItemId, status, reason | `TicketView` | Validate and apply item transition | `moveItemToCooking`, `markItemReady`, `blockItem`. |
| `KitchenTicketItemWorkflowService.java` | `moveItemToCooking` | item context, actor | void | Deduct inventory then mark cooking and publish event | `RemoteInventoryConsumptionClient`, `KitchenTicketEventNotifier`. |
| `KitchenTicketItemWorkflowService.java` | `markItemReady` | item context | void | Mark ready, publish dish status, enqueue ready notification | `KitchenTicketEventNotifier`. |
| `KitchenTicketEventNotifier.java` | `publishDishStatus` | item, status details | void | Publish `KitchenDishStatusChangedEvent` | Outbox/RabbitMQ. |
| `RemoteOrderStateUpdateClient.java` | `applyKitchenLineStatuses` | orderId, lineStatuses | void | Sync kitchen status to ordering | `/internal/ordering/orders/{orderId}/line-statuses`. |

### 3.6 Các flow chính của service

#### Flow: Nhận order từ ordering

##### Mục đích
Nhận item đã confirm từ ordering và tạo ticket theo station.

##### Trigger
Ordering `OrderKitchenRoutingStep` gọi internal kitchen API.

##### Step-by-step
1. Ordering gọi `POST /internal/kitchen/orders/{orderId}/tickets`.
2. `KitchenIntegrationController.queueConfirmedItems` nhận `QueueItemsRequest`.
3. `KitchenOrderRoutingService.queueConfirmedItems` lọc item chưa có ticket.
4. Service tạo route plan, group items theo station.
5. Với mỗi station, repository resolve station, create ticket, create ticket item.
6. Nếu có item insert, publish `KitchenTicketCreatedEvent`.
7. Response 202 về ordering.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `ordering/integration/RemoteKitchenOrderRoutingClient.java` | `queueConfirmedItems` | Remote caller. |
| 2 | `kitchen/api/KitchenIntegrationController.java` | `queueConfirmedItems` | Internal API. |
| 3 | `kitchen/application/KitchenOrderRoutingService.java` | `queueConfirmedItems` | Routing logic. |
| 5 | `kitchen/infrastructure/persistence/JdbcKitchenOrderRoutingRepository.java` | `createRoutePlan`, `createTicket`, `createTicketItem` | DB writes. |
| 6 | `kitchen/application/events/KitchenTicketCreatedEvent.java` | event | Ticket event. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Internal API | `/internal/kitchen/orders/{orderId}/tickets` | `KitchenIntegrationController.java` | Ordering → kitchen. |
| Event | `KitchenTicketCreatedEvent` | `KitchenOrderRoutingService.java` | Published per created ticket. |

#### Flow: Tạo kitchen ticket

##### Mục đích
Persist route plan/ticket/ticket items theo station.

##### Trigger
`KitchenOrderRoutingService.queueConfirmedItems`.

##### Step-by-step
1. Service gọi `routableItems` để skip item đã có ticket.
2. Repository tạo route plan.
3. Group command theo `KitchenOrderItemCommand.station`.
4. Với mỗi station: resolve station ID, create ticket, create ticket item.
5. Empty ticket/route plan bị xóa nếu không insert được item.
6. Ticket created event được publish khi ticket có item.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `KitchenOrderRoutingService.java` | `routableItems` | Idempotency filter. |
| 2 | `JdbcKitchenOrderRoutingRepository.java` | `createRoutePlan` | DB route plan. |
| 4 | `JdbcKitchenOrderRoutingRepository.java` | `resolveStationId`, `createTicket`, `createTicketItem` | DB ticket. |
| 6 | `KitchenOrderRoutingService.java` | `outboxPublisher.publish` | Ticket event. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Data | kitchen route/ticket/ticket item | `JdbcKitchenOrderRoutingRepository.java` | Station-based grouping. |
| Event | `KitchenTicketCreatedEvent` | `KitchenTicketCreatedEvent.java` | For reporting/audit. |

#### Flow: Xem kitchen queue/overview

##### Mục đích
Hiển thị queue hiện tại cho bếp.

##### Trigger
FE `_app.kitchen.tsx`.

##### Step-by-step
1. FE gọi `GET /api/kitchen/overview?station=...`.
2. Gateway route sang kitchen.
3. `KitchenController.overview` require `kitchen.manage`.
4. `KitchenService.load` gọi `KitchenTicketQueryRepository.load`.
5. Repository trả `KitchenOverview(stations,tickets)`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.kitchen.tsx` | kitchen query | FE. |
| 3 | `kitchen/api/KitchenController.java` | `overview` | API. |
| 4 | `kitchen/application/KitchenService.java` | `load` | Facade. |
| 5 | `kitchen/infrastructure/persistence/JdbcKitchenTicketQueryRepository.java` | `load` | DB read. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/kitchen/overview` | `KitchenController.java` | Public. |

#### Flow: Update item status cooking/ready/blocked

##### Mục đích
Cho bếp chuyển trạng thái từng món.

##### Trigger
FE `_app.kitchen.tsx` item status action.

##### Step-by-step
1. FE gọi `PATCH /api/kitchen/items/{ticketItemId}/status` với `status`, `reason`.
2. `KitchenController.updateItemStatus` gọi `KitchenService.updateItemStatus`.
3. Service normalize status và load item context for update.
4. `KitchenStatusPolicy.ensureValidItemTransition` validate transition.
5. Nếu target `blocked`, reason bắt buộc; nếu đã bắt đầu nấu có thể cần manager/admin.
6. Target `cooking`: `KitchenTicketItemWorkflowService.moveItemToCooking`.
7. Target `ready`: `markItemReady`.
8. Target `blocked`: `blockItem`.
9. Service reconcile ticket và trả `TicketView`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.kitchen.tsx` | item status mutation | FE. |
| 2 | `kitchen/api/KitchenController.java` | `updateItemStatus` | API. |
| 3 | `kitchen/application/KitchenService.java` | `updateItemStatus` | Main flow. |
| 4 | `kitchen/domain/KitchenStatusPolicy.java` | `ensureValidItemTransition` | Domain policy. |
| 6 | `KitchenTicketItemWorkflowService.java` | `moveItemToCooking` | Cooking transition. |
| 7 | `KitchenTicketItemWorkflowService.java` | `markItemReady` | Ready transition. |
| 8 | `KitchenTicketItemWorkflowService.java` | `blockItem` | Blocked transition. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `PATCH /api/kitchen/items/{ticketItemId}/status` | `KitchenController.java` | Public. |
| Event | `KitchenDishStatusChangedEvent` | `KitchenTicketEventNotifier.java` | Published for cooking/ready/blocked. |
| Remote call | `/internal/inventory/consumption/kitchen-start` | `RemoteInventoryConsumptionClient.java` | Only when move to cooking. |

#### Flow: Sync status về ordering

##### Mục đích
Đồng bộ trạng thái món/order từ kitchen sang ordering.

##### Trigger
Kitchen ticket/item reconcile.

##### Step-by-step
1. Kitchen item/ticket update gọi state coordinator reconcile.
2. `JdbcKitchenTicketStateCoordinator` tính ticket/order status projection.
3. Sau commit, coordinator gọi `OrderingStatusSyncPort`.
4. `RemoteOrderStateUpdateClient.applyKitchenLineStatuses` gọi ordering internal line status API.
5. Ordering cập nhật order item line status và refresh order.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `KitchenService.java` | `updateTicketStatus`, `updateItemStatus` | Trigger reconcile. |
| 2 | `JdbcKitchenTicketStateCoordinator.java` | `reconcileTicket`, `reconcileTicketsForOrderItem` | Compute status. |
| 3 | `JdbcKitchenTicketStateCoordinator.java` | after commit sync | Calls port. |
| 4 | `kitchen/integration/RemoteOrderStateUpdateClient.java` | `applyKitchenLineStatuses`, `refreshOrder` | Remote call. |
| 5 | `ordering/api/OrderingIntegrationController.java` | `applyKitchenLineStatuses`, `refreshOrder` | Receiver. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Internal API | `/internal/ordering/orders/{orderId}/line-statuses` | `RemoteOrderStateUpdateClient.java` | Kitchen → ordering. |
| Internal API | `/internal/ordering/orders/{orderId}/refresh-status` | `RemoteOrderStateUpdateClient.java` | Kitchen → ordering. |

#### Flow: Trừ inventory khi bắt đầu nấu

##### Mục đích
Deduct inventory theo recipe khi món bắt đầu cooking.

##### Trigger
Kitchen item target status `cooking` hoặc backfill.

##### Step-by-step
1. FE hoặc automation chuyển item sang `cooking`.
2. `KitchenTicketItemWorkflowService.moveItemToCooking` gọi `inventoryService.consumeForKitchenStart`.
3. `RemoteInventoryConsumptionClient.consumeForKitchenStart` gọi `POST /internal/inventory/consumption/kitchen-start`.
4. Inventory xử lý deduction theo recipe.
5. Kitchen mark item cooking và publish dish status.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `KitchenService.java` | `updateItemStatus` | Trigger. |
| 2 | `KitchenTicketItemWorkflowService.java` | `moveItemToCooking` | Calls inventory before DB cooking update. |
| 3 | `kitchen/integration/RemoteInventoryConsumptionClient.java` | `consumeForKitchenStart` | Remote client. |
| 4 | `inventory/api/InventoryIntegrationController.java` | `consumeForKitchenStart` | Inventory receiver. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Internal API | `/internal/inventory/consumption/kitchen-start` | `RemoteInventoryConsumptionClient.java` | Kitchen → inventory. |
| Event | `InventoryStockChangedEvent` | inventory service | Published by inventory after transaction. |

#### Flow: Publish notification/event khi món ready

##### Mục đích
Thông báo server khi món sẵn sàng lấy.

##### Trigger
Kitchen item target status `ready`.

##### Step-by-step
1. FE gọi item status `ready`.
2. `KitchenTicketItemWorkflowService.markItemReady` mark ready.
3. `KitchenTicketEventNotifier.publishDishStatus` publish `KitchenDishStatusChangedEvent`.
4. `KitchenTicketEventNotifier.queueReadyNotification` enqueue `NotificationCommand` với type `kitchen_status`, template `KITCHEN_ITEM_READY`.
5. Notification service materialize/queue inbox.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.kitchen.tsx` | status mutation | FE. |
| 2 | `KitchenTicketItemWorkflowService.java` | `markItemReady` | Mark DB ready. |
| 3 | `KitchenTicketEventNotifier.java` | `publishDishStatus` | Event. |
| 4 | `KitchenTicketEventNotifier.java` | `queueReadyNotification` | Notification command. |
| 5 | `notification/application/NotificationRequestMaterializer.java` | `materialize` | Queue notification. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Event | `KitchenDishStatusChangedEvent` | `KitchenTicketEventNotifier.java` | Reporting/audit/notification. |
| Notification | `KITCHEN_ITEM_READY` | `KitchenTicketEventNotifier.java` | Recipient user is item server. |

## 4. Reservation Service

### 4.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Reservation overview/recommendation/create/update/confirm/check-in/no-show, table CRUD/status, waitlist create/notify/skip/prioritize/seat, reservation notification command. |
| API public chính | `/api/reservations/**`, `/api/tables/**`, `/api/waitlist/**`, special `POST /api/notifications`. |
| Internal API nếu có | Chưa tìm thấy `/internal/reservation/**` controller trong source code. |
| Service khác gọi vào | Gateway routes FE APIs; no internal caller found. |
| Service này gọi ra | Identity policy/default branch; notification command publisher; outbox audit/events. |
| FE màn hình/flow liên quan | `_app.reservations.tsx`, `_app.tables.$tableId.tsx`. |

### 4.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `_app.reservations.tsx` | Load overview | `GET /api/reservations/overview?date=` | `/api/reservations` → reservation | `ReservationController.java` | `overview` | `ReservationService.load` | Tables/reservations/waitlist. |
| `_app.reservations.tsx` | Get recommendations | `GET /api/reservations/recommendations` | reservation | `ReservationController.java` | `recommendations` | `ReservationService.recommend` | Candidate tables/quoted wait. |
| `_app.reservations.tsx` | Create reservation | `POST /api/reservations` | reservation | `ReservationController.java` | `createReservation` | `ReservationLifecycleService.createReservation` | May assign table or fallback waitlist. |
| `_app.reservations.tsx` | Update reservation | `PUT /api/reservations/{id}` | reservation | `ReservationController.java` | `updateReservation` | `ReservationLifecycleService.updateReservation` | Guest/party/notes. |
| `_app.reservations.tsx` | Confirm reservation | `POST /api/reservations/{id}/confirm` | reservation | `ReservationController.java` | `confirmReservation` | `ReservationLifecycleService.confirmReservation` | Reserves table if possible. |
| `_app.reservations.tsx` | Check-in | `POST /api/reservations/{id}/check-in` | reservation | `ReservationController.java` | `checkIn` | `ReservationCheckInService.checkIn` | Opens table session. |
| `_app.reservations.tsx` | No-show | `POST /api/reservations/{id}/no-show` | reservation | `ReservationController.java` | `noShow` | `ReservationLifecycleService.markNoShow` | Releases table, queues staff notification. |
| `_app.reservations.tsx` | Table create/update/delete/status | `/api/tables/**` | reservation | `TableController.java` | table methods | `ReservationTableService.*` | Table CRUD/status. |
| `_app.reservations.tsx` | Waitlist create/notify/skip/prioritize/seat | `/api/waitlist/**` | reservation | `WaitlistController.java` | waitlist methods | `ReservationWaitlistService.*` | Waitlist workflow. |
| `_app.reservations.tsx` | Send reservation notification | `POST /api/notifications` | special route → reservation | `ReservationNotificationController.java` | `sendNotification` | `ReservationNotificationCoordinator.sendNotification` | Not notification-service. |

### 4.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| GET | `/api/reservations/overview` | `ReservationController` | `overview` | `date?` | `ReservationOverview` | Public. |
| GET | `/api/reservations/recommendations` | `ReservationController` | `recommendations` | `date,time,party` | `ReservationRecommendation` | Candidate tables. |
| POST | `/api/reservations` | `ReservationController` | `createReservation` | `ReservationBody` | `ReservationView` | Create. |
| PUT | `/api/reservations/{reservationId}` | `ReservationController` | `updateReservation` | `ReservationEditBody` | `ReservationView` | Update. |
| POST | `/api/reservations/{reservationId}/confirm` | `ReservationController` | `confirmReservation` | path | `ReservationView` | Confirm. |
| POST | `/api/reservations/{reservationId}/check-in` | `ReservationController` | `checkIn` | `CheckInBody?` | `ReservationView` | Opens session. |
| POST | `/api/reservations/{reservationId}/no-show` | `ReservationController` | `noShow` | path | `ReservationView` | No-show. |
| POST | `/api/tables` | `TableController` | `createTable` | `TableBody` | `TableView` | Create table. |
| PUT | `/api/tables/{tableId}` | `TableController` | `updateTable` | `TableBody` | `TableView` | Update table. |
| DELETE | `/api/tables/{tableId}` | `TableController` | `deleteTable` | path | `EntityReferenceResponse` | Delete table. |
| POST | `/api/tables/{tableId}/status` | `TableController` | `updateTableStatus` | `TableStatusBody` | `TableActionResult` | Table status/session update. |
| POST | `/api/waitlist` | `WaitlistController` | `createWaitlist` | `WaitlistBody` | `WaitlistView` | Create waitlist. |
| POST | `/api/waitlist/{waitlistEntryId}/notify` | `WaitlistController` | `notifyWaitlist` | path | `WaitlistView` | Mark notified + notification. |
| POST | `/api/waitlist/{waitlistEntryId}/skip` | `WaitlistController` | `skipWaitlist` | path | `WaitlistView` | Skip. |
| POST | `/api/waitlist/{waitlistEntryId}/prioritize` | `WaitlistController` | `prioritizeWaitlist` | path | `WaitlistView` | Priority. |
| POST | `/api/waitlist/{waitlistEntryId}/seat` | `WaitlistController` | `seatWaitlist` | path | `WaitlistView` | Seat waitlist. |
| POST | `/api/notifications` | `ReservationNotificationController` | `sendNotification` | `NotificationBody` | `StatusResponse("queued")` | Gateway special route. |

### 4.4 Các file quan trọng và chức năng của chúng

#### 4.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `runtime/reservation/ReservationServiceApplication.java` | Entry service, scan reservation + common. |
| `application-reservation-service.properties` | Port `8084`, identity URL. |

#### 4.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `reservation/api/ReservationController.java` | overview/recommend/create/update/confirm/check-in/no-show | Reservation lifecycle API. |
| `reservation/api/TableController.java` | create/update/delete/status | Table API. |
| `reservation/api/WaitlistController.java` | create/notify/skip/prioritize/seat | Waitlist API. |
| `reservation/api/ReservationNotificationController.java` | `sendNotification` | Customer/staff reservation notification request. |

#### 4.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `reservation/application/ReservationService.java` | facade methods, `processReservationTimers` | Delegates reservation/table/waitlist/notification. |
| `ReservationLifecycleService.java` | `createReservation`, `confirmReservation`, `markNoShow`, `checkIn` | Main reservation lifecycle. |
| `ReservationCheckInService.java` | `checkIn` | Validates grace window, opens table session. |
| `ReservationTableService.java` | table CRUD/status | Table operations. |
| `ReservationTableStatusService.java` | `updateTableStatus` | Table status/session release/close behavior. |
| `ReservationWaitlistService.java` | waitlist facade | Delegates waitlist flows. |
| `ReservationWaitlistCreationService.java` | `createWaitlistEntry` | Waitlist create. |
| `ReservationWaitlistNotificationFlowService.java` | `notifyWaitlist`, `skipWaitlist`, `prioritizeWaitlist` | Waitlist actions. |
| `ReservationWaitlistSeatingService.java` | `seatWaitlist` | Seat waitlist and create session. |
| `ReservationNotificationCoordinator.java` | `sendNotification`, `queueStaffReservationNotification` | Notification command construction. |

#### 4.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `JdbcReservationQueryRepository.java` | reservations/tables/waitlist | Overview/reservation row reads. |
| `JdbcReservationLifecycleCommandAdapter.java` | reservations/contact | Create/update/confirm/no-show/cancel timers. |
| `JdbcReservationCheckInCommandAdapter.java` | table_session, reservation_assignment | Open session, occupy table, mark seated. |
| `JdbcReservationTableCommandAdapter.java` | dining tables/table sessions | Table CRUD/status/session close. |
| `JdbcReservationTableAssignmentRepository.java` | table assignment | Assign/release/reserve table. |
| `JdbcReservationWaitlistCommandAdapter.java` | waitlist/table session | Waitlist create/notify/skip/seat. |
| `JdbcWaitlistQueryRepository.java` | waitlist | Waitlist row reads. |
| `JdbcReservationNotificationTargetRepository.java` | reservation/waitlist target | Loads notification target contact. |

#### 4.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `ReservationNotificationCoordinator.java` | Adapter | Builds `NotificationCommand` and enqueues. |
| `reservation/application/events/ReservationCreatedEvent.java` | Event | Reservation created. |
| `reservation/application/events/ReservationSeatedEvent.java` | Event | Reservation seated/check-in. |
| `reservation/application/events/WaitlistUpdatedEvent.java` | Event | Waitlist updates. |
| `reservation/application/workflow/RequestReservationNotificationStep.java` | Event publisher | Queue notification for seated/waitlist event. |

### 4.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `ReservationService.java` | `load` | date | `ReservationOverview` | Load overview | `ReservationQueryRepository.load`. |
| `ReservationService.java` | `recommend` | date,time,party | `ReservationRecommendation` | Candidate table/quoted wait | `ReservationTableAssignmentService`. |
| `ReservationLifecycleService.java` | `createReservation` | upsert, actor | `ReservationView` | Create contact/reservation, assign table or waitlist fallback, audit | `ReservationLifecycleCommandPort`, `ReservationWaitlistService`. |
| `ReservationLifecycleService.java` | `confirmReservation` | reservationId | `ReservationView` | Reserve assigned/available table, confirm reservation | `ReservationTableAssignmentService`, `ReservationLifecycleCommandPort`. |
| `ReservationCheckInService.java` | `checkIn` | reservationId, request, actor | `ReservationView` | Grace-window validation, resolve table, open session, mark seated | `ReservationCheckInCommandPort`. |
| `ReservationTableService.java` | `updateTableStatus` | tableId, status | `TableActionResult` | Delegates table status workflow | `ReservationTableStatusService`. |
| `ReservationNotificationCoordinator.java` | `sendNotification` | reservation/waitlist target, title/body/channel | void | Validate target and enqueue notification command | `NotificationCommandPublisher`. |

### 4.6 Các flow chính của service

#### Flow: Create reservation

##### Mục đích
Tạo reservation, gán bàn nếu có, hoặc fallback waitlist nếu không có bàn.

##### Trigger
FE `_app.reservations.tsx` create reservation dialog.

##### Step-by-step
1. FE gọi `POST /api/reservations`.
2. Gateway route sang reservation.
3. `ReservationController.createReservation` require `reservations.manage`.
4. `ReservationLifecycleService.createReservation` validate party size.
5. Service lấy default branch từ identity, tạo contact, parse arrival time.
6. Nếu có `tableId`, validate assigned table; nếu không, tìm candidate table.
7. Status là `confirmed` nếu có table, ngược lại `pending`.
8. Repository tạo reservation.
9. Nếu có table, reserve table; nếu không và `fallbackToWaitlist=true`, tạo waitlist từ reservation.
10. Audit `reservation.created`, response `ReservationView`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.reservations.tsx` | action mutation | FE create. |
| 3 | `reservation/api/ReservationController.java` | `createReservation` | API. |
| 4 | `reservation/application/ReservationLifecycleService.java` | `createReservation` | Business. |
| 5 | `common/identity/RemoteSharedIdentityPolicyClient.java` | `findDefaultBranch` | Identity call. |
| 8 | `JdbcReservationLifecycleCommandAdapter.java` | `createContact`, `createReservation` | DB. |
| 9 | `ReservationTableAssignmentService.java` | `reserveTableForReservation` | Table assignment. |
| 9 | `ReservationWaitlistService.java` | `createWaitlistFromReservation` | Fallback waitlist. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/reservations` | `ReservationController.java` | Public. |
| Data | reservation/contact/table assignment | `JdbcReservationLifecycleCommandAdapter.java`, `JdbcReservationTableAssignmentRepository.java` | DB writes. |
| Event | `ReservationCreatedEvent` | Chưa thấy publish trực tiếp trong method đọc được; event class tồn tại | Chưa tìm thấy publish tại đoạn đọc chính. |

#### Flow: Update/confirm reservation

##### Mục đích
Cập nhật thông tin reservation hoặc confirm reservation pending.

##### Trigger
FE edit/confirm button.

##### Step-by-step
1. Update: FE gọi `PUT /api/reservations/{reservationId}`.
2. `ReservationLifecycleService.updateReservation` update notes/party/contact name.
3. Confirm: FE gọi `POST /api/reservations/{reservationId}/confirm`.
4. `ReservationLifecycleService.confirmReservation` tìm assigned table hoặc available table phù hợp.
5. Nếu có table, reserve table cho reservation.
6. Repository confirm reservation và trả `ReservationView`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.reservations.tsx` | update action | FE. |
| 2 | `ReservationLifecycleService.java` | `updateReservation` | Update DB. |
| 3 | `_app.reservations.tsx` | confirm action | FE. |
| 4 | `ReservationLifecycleService.java` | `confirmReservation` | Confirm flow. |
| 5 | `ReservationTableAssignmentService.java` | `findAssignedTableId`, `findAvailableTableIdForReservation`, `reserveTableForReservation` | Table assignment. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `PUT /api/reservations/{id}` | `ReservationController.java` | Update. |
| API | `POST /api/reservations/{id}/confirm` | `ReservationController.java` | Confirm. |

#### Flow: Check-in reservation

##### Mục đích
Seat guest, occupy table, mở table session cho ordering/billing.

##### Trigger
FE check-in action trên confirmed reservation.

##### Step-by-step
1. FE gọi `POST /api/reservations/{reservationId}/check-in` với actual party/replacement table optional.
2. `ReservationController.checkIn` require `tables.assign`.
3. `ReservationCheckInService.checkIn` load reservation row.
4. Service tính actual party size, validate > 0.
5. Lấy `reservationGraceMinutes` từ identity policy; nếu ngoài grace window cần `approveRecovery` và actor manager/admin.
6. Resolve table ID, align reservation assignment.
7. `JdbcReservationCheckInCommandAdapter.openReservationSession` tạo table session.
8. Mark table occupied, mark reservation seated, link assignment-session.
9. Audit status seated, response `ReservationView`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.reservations.tsx` | check-in mutation | FE. |
| 2 | `reservation/api/ReservationController.java` | `checkIn` | API. |
| 3 | `reservation/application/ReservationCheckInService.java` | `checkIn` | Business check-in. |
| 5 | `common/identity/RemoteSharedIdentityPolicyClient.java` | `getPolicySnapshot` | Grace window policy. |
| 7 | `JdbcReservationCheckInCommandAdapter.java` | `openReservationSession` | Create session. |
| 8 | `JdbcReservationCheckInCommandAdapter.java` | `markTableOccupied`, `markReservationSeated`, `linkReservationAssignmentSession` | DB updates. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/reservations/{id}/check-in` | `ReservationController.java` | Public. |
| Data | table_session | `JdbcReservationCheckInCommandAdapter.java` | Used later by ordering/billing. |

#### Flow: Assign table

##### Mục đích
Reserve/align table for reservation or table status update.

##### Trigger
Create/confirm/check-in reservation, table status action.

##### Step-by-step
1. Create/confirm reservation uses `ReservationTableAssignmentService` to reserve table.
2. Check-in uses `resolveCheckInTableId` and `alignReservationAssignment`.
3. Table status update calls `POST /api/tables/{tableId}/status`.
4. `TableController.updateTableStatus` calls `ReservationTableService.updateTableStatus`.
5. `ReservationTableStatusService` handles status-specific behavior.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `ReservationLifecycleService.java` | `createReservation`, `confirmReservation` | Reserve table. |
| 2 | `ReservationCheckInService.java` | `checkIn` | Align table for seating. |
| 3 | `frontend/src/routes/_app.reservations.tsx` | table status mutation | FE. |
| 4 | `reservation/api/TableController.java` | `updateTableStatus` | API. |
| 5 | `reservation/application/ReservationTableStatusService.java` | `updateTableStatus` | Status workflow. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/tables/{tableId}/status` | `TableController.java` | Table status. |
| Data | table assignment/session | `JdbcReservationTableAssignmentRepository.java`, `JdbcReservationTableCommandAdapter.java` | Assignment/session lifecycle. |

#### Flow: No-show/cancel

##### Mục đích
Mark reservation no-show, release table, notify staff.

##### Trigger
FE no-show button.

##### Step-by-step
1. FE gọi `POST /api/reservations/{reservationId}/no-show`.
2. `ReservationController.noShow` require `reservations.manage`.
3. `ReservationLifecycleService.markNoShow` mark reservation no-show.
4. `ReservationTableAssignmentService.releaseReservationTable` release table assignment.
5. Audit `reservation.no_show`.
6. `ReservationNotificationCoordinator.queueStaffReservationNotification` enqueue staff notification `RESERVATION_CANCELLED`.
7. Response updated reservation.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.reservations.tsx` | no-show action | FE. |
| 2 | `ReservationController.java` | `noShow` | API. |
| 3 | `ReservationLifecycleService.java` | `markNoShow` | Business. |
| 4 | `ReservationTableAssignmentService.java` | `releaseReservationTable` | Release table. |
| 6 | `ReservationNotificationCoordinator.java` | `queueStaffReservationNotification` | Notification. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/reservations/{id}/no-show` | `ReservationController.java` | No-show. |
| API cancel | Reservation cancel endpoint | Chưa tìm thấy trong source code | Có no-show; chưa thấy `/cancel`. |

#### Flow: Waitlist create/notify/skip/seat

##### Mục đích
Quản lý khách chờ và seat khi có bàn.

##### Trigger
FE waitlist tab.

##### Step-by-step
1. Create: FE gọi `POST /api/waitlist`.
2. `ReservationWaitlistService.createWaitlistEntry` tạo waitlist entry.
3. Notify: FE gọi `POST /api/waitlist/{id}/notify`.
4. `ReservationWaitlistNotificationFlowService.notifyWaitlist` mark notified, queue notification.
5. Skip/prioritize gọi các endpoint tương ứng và update status/priority.
6. Seat: FE gọi `POST /api/waitlist/{id}/seat`.
7. `ReservationWaitlistSeatingService.seatWaitlist` chọn table, tạo table session, mark table occupied, mark waitlist seated.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.reservations.tsx` | waitlist create mutation | FE. |
| 2 | `ReservationWaitlistCreationService.java` | `createWaitlistEntry` | Create waitlist. |
| 3 | `WaitlistController.java` | `notifyWaitlist`, `skipWaitlist`, `prioritizeWaitlist`, `seatWaitlist` | API. |
| 4 | `ReservationWaitlistNotificationFlowService.java` | `notifyWaitlist` | Notify. |
| 7 | `ReservationWaitlistSeatingService.java` | `seatWaitlist` | Seat. |
| 7 | `JdbcReservationWaitlistCommandAdapter.java` | `createTableSession`, `markTableOccupied`, `markWaitlistSeated` | DB writes. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `/api/waitlist/**` | `WaitlistController.java` | Public. |
| Event | `WaitlistUpdatedEvent` | event class exists | Publish location cần đọc sâu hơn nếu cần. |

#### Flow: Reservation notification

##### Mục đích
Queue notification tới reservation/waitlist target.

##### Trigger
FE `_app.reservations.tsx` gọi special `POST /api/notifications`.

##### Step-by-step
1. FE gọi `POST /api/notifications` với `reservationId` hoặc `waitlistEntryId`, title/body/channel.
2. Gateway special route `POST /api/notifications` sang reservation-service.
3. `ReservationNotificationController.sendNotification` require `reservations.manage`.
4. `ReservationNotificationCoordinator.sendNotification` validate phải có target.
5. Repository load reservation/waitlist notification target.
6. `ReservationNotificationMessageFactory.customerNotification` build command.
7. `NotificationCommandPublisher.enqueue` đưa command vào outbox/notification pipeline.
8. Response `queued`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.reservations.tsx` | `ENDPOINTS.notifications` mutation | FE. |
| 2 | `gateway/GatewayRouteLocator.java` | special POST route | Route to reservation. |
| 3 | `ReservationNotificationController.java` | `sendNotification` | API. |
| 4 | `ReservationNotificationCoordinator.java` | `sendNotification` | Business. |
| 5 | `JdbcReservationNotificationTargetRepository.java` | target load methods | DB read. |
| 6 | `ReservationNotificationMessageFactory.java` | `customerNotification` | Build message. |
| 7 | `common/notification/NotificationCommandPublisher.java` | `enqueue` | Outbox. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/notifications` | `ReservationNotificationController.java` | Special ownership. |
| Notification | customer reservation/waitlist notification | `ReservationNotificationCoordinator.java` | Queued. |

## 5. Notification Service

### 5.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Notification inbox API, mark read, consume notification/event queues, map event payload thành notification command, queue messages/deliveries. |
| API public chính | `GET /api/notifications`, `POST /api/notifications/{messageId}/read`. |
| Internal API nếu có | Chưa tìm thấy `/internal/notification/**` controller trong source code. |
| Service khác gọi vào | Services publish/enqueue notification command/events; FE đọc inbox qua gateway. |
| Service này gọi ra | Không thấy remote REST call chính; ghi DB notification_messages/deliveries. |
| FE màn hình/flow liên quan | `TopBar.tsx` notification inbox/read; reservation special POST không vào notification-service do gateway route sang reservation. |

### 5.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `TopBar.tsx` | Load inbox | `GET /api/notifications` | `/api/notifications` → notification | `NotificationController.java` | `list` | `NotificationInboxService.loadInbox` | Uses current user. |
| `TopBar.tsx` | Mark read | `POST /api/notifications/{id}/read` | `/api/notifications` → notification | `NotificationController.java` | `markRead` | `NotificationInboxService.markRead` | Updates message read state. |
| `_app.reservations.tsx` | Send reservation notification | `POST /api/notifications` | special POST → reservation | `ReservationNotificationController.java` | `sendNotification` | `ReservationNotificationCoordinator.sendNotification` | Không vào notification-service. |
| Chưa tìm thấy FE caller trực tiếp trong frontend source. | Consume service event | RabbitMQ queue | Không qua gateway | `NotificationRequestedConsumer.java` | queue listener methods | `NotificationRequestMaterializer.materialize` | Event-driven. |

### 5.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| GET | `/api/notifications` | `NotificationController` | `list` | bearer user | `List<NotificationView>` | Public via gateway, authenticated. |
| POST | `/api/notifications/{messageId}/read` | `NotificationController` | `markRead` | path | `NotificationView` | Mark read. |
| RabbitMQ | `irms.notification.requested.q` | `NotificationRequestedConsumer` | `consume` | `EventEnvelope` | ack/nack | General notification requested. |
| RabbitMQ | `irms.notification.kitchen-dish-status.q` | `NotificationRequestedConsumer` | `kitchenDishNotification` | `EventEnvelope` | ack/nack | Kitchen dish notification. |
| RabbitMQ | `irms.notification.payment-completed.q` | `NotificationRequestedConsumer` | `paymentNotification` | `EventEnvelope` | ack/nack | Payment notification. |
| RabbitMQ | `irms.notification.refund-issued.q` | `NotificationRequestedConsumer` | `refundNotification` | `EventEnvelope` | ack/nack | Refund notification. |
| RabbitMQ | `irms.notification.receipt-generated.q` | `NotificationRequestedConsumer` | `receiptNotification` | `EventEnvelope` | ack/nack | Receipt notification. |
| RabbitMQ | `irms.notification.order-cancelled.q` | `NotificationRequestedConsumer` | `orderCancelledNotification` | `EventEnvelope` | ack/nack | Order cancelled notification. |
| RabbitMQ | `irms.notification.low-stock.q` | `NotificationRequestedConsumer` | `lowStockNotification` | `EventEnvelope` | ack/nack | Low stock notification. |
| RabbitMQ | reservation/menu queues | `NotificationRequestedConsumer` | reservation/menu methods | `EventEnvelope` | ack/nack | Reservation/waitlist/menu events. |

### 5.4 Các file quan trọng và chức năng của chúng

#### 5.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `runtime/notification/NotificationServiceApplication.java` | Entry service, scan notification + common. |
| `application-notification-service.properties` | Port `8086`, identity URL, RabbitMQ enabled. |

#### 5.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `notification/api/NotificationController.java` | `list`, `markRead` | Inbox APIs. |

#### 5.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `NotificationInboxService.java` | `loadInbox`, `markRead` | Inbox read/update. |
| `NotificationRequestMaterializer.java` | `materialize`, `genericCommand` | Map event envelope to command and queue. |
| `DishReadyNotificationMapper.java` | `supports`, `map` | Typed mapper for dish ready/kitchen status. |
| `LowStockNotificationMapper.java` | `supports`, `map` | Typed mapper for low stock. |
| `PaymentCompletedNotificationMapper.java` | `supports`, `map` | Payment notification. |
| `RefundIssuedNotificationMapper.java` | `supports`, `map` | Refund notification. |
| `ReservationSeatedNotificationMapper.java` | `supports`, `map` | Reservation seated notification. |
| `OrderCancelledNotificationMapper.java` | `supports`, `map` | Order cancelled notification. |
| channel adapters | `validate` | Validate `in_app`, email, sms, phone, print channels. |

#### 5.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `JdbcNotificationInboxRepository.java` | notification messages | Load inbox and mark read. |
| `JdbcNotificationQueueRepository.java` | `notification_messages`, `notification_deliveries` | Queue notification and delivery row. |

#### 5.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `NotificationRequestedConsumer.java` | Event consumer | Consumes notification-related queues. |
| `AuditFollowUpRequestedConsumer.java` | Event consumer | Creates notification on audit follow-up request. |
| `common/notification/OutboxNotificationCommandPublisher.java` | Event publisher/adapter | Other services enqueue notification command through outbox. |

### 5.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `NotificationController.java` | `list` | current user | inbox list | API load inbox | `NotificationInboxService.loadInbox`. |
| `NotificationController.java` | `markRead` | messageId, current user | `NotificationView` | Mark message read | `NotificationInboxService.markRead`. |
| `NotificationRequestMaterializer.java` | `materialize` | `EventEnvelope` | void | Find typed mapper or generic command, queue notification | `NotificationQueuePort.queue`. |
| `JdbcNotificationQueueRepository.java` | `queue` | `NotificationCommand` | `QueuedNotification` | Validate channel, insert message and delivery | DB. |
| `NotificationRequestedConsumer.java` | listener methods | RabbitMQ message | ack/nack | Manual ack wrapper for materializer | `ManualAckConsumerSupport.handle`. |

### 5.6 Các flow chính của service

#### Flow: List notification inbox

##### Mục đích
Hiển thị notification trong TopBar.

##### Trigger
`frontend/src/components/layout/TopBar.tsx`.

##### Step-by-step
1. FE gọi `GET /api/notifications`.
2. Gateway route non-POST-root `/api/notifications` sang notification-service.
3. `NotificationController.list` lấy `currentUser.require()`.
4. `NotificationInboxService.loadInbox` gọi repository.
5. `JdbcNotificationInboxRepository.loadInbox` lọc theo user/role và trả view.
6. FE hiển thị unread count và link theo notification type.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/components/layout/TopBar.tsx` | notification query | FE. |
| 2 | `gateway/GatewayRouteLocator.java` | notification route | Gateway. |
| 3 | `notification/api/NotificationController.java` | `list` | API. |
| 4 | `notification/application/NotificationInboxService.java` | `loadInbox` | Service. |
| 5 | `notification/infrastructure/persistence/JdbcNotificationInboxRepository.java` | `loadInbox` | DB read. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/notifications` | `NotificationController.java` | Public. |
| Data | `notification_messages` | `JdbcNotificationInboxRepository.java` | Inbox. |

#### Flow: Mark notification as read

##### Mục đích
Đánh dấu message đã đọc.

##### Trigger
Click notification trong TopBar.

##### Step-by-step
1. FE gọi `POST /api/notifications/{messageId}/read`.
2. Gateway route sang notification-service.
3. `NotificationController.markRead` lấy current user.
4. `NotificationInboxService.markRead` gọi repository.
5. Repository update message/read state và trả `NotificationView`.
6. FE invalidate query và navigate theo payload/type.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `TopBar.tsx` | `markReadMutation` | FE. |
| 3 | `NotificationController.java` | `markRead` | API. |
| 4 | `NotificationInboxService.java` | `markRead` | Service. |
| 5 | `JdbcNotificationInboxRepository.java` | `markRead` | DB update. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/notifications/{id}/read` | `NotificationController.java` | Public. |

#### Flow: Nhận notification request/event từ service khác

##### Mục đích
Materialize event/command thành notification message.

##### Trigger
RabbitMQ queues hoặc notification command outbox từ service khác.

##### Step-by-step
1. Service khác enqueue/publish notification event qua outbox/RabbitMQ.
2. `NotificationRequestedConsumer` lắng nghe các queue như `irms.notification.requested.q`, `irms.notification.low-stock.q`, `irms.notification.payment-completed.q`.
3. Consumer gọi `ManualAckConsumerSupport.handle`.
4. `NotificationRequestMaterializer.materialize` nhận `EventEnvelope`.
5. Materializer tìm typed mapper phù hợp, nếu không có dùng `genericCommand`.
6. `JdbcNotificationQueueRepository.queue` insert `notification_messages` và `notification_deliveries`.
7. Ack message nếu thành công.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | various services | `NotificationCommandPublisher.enqueue` | Source command/event. |
| 2 | `NotificationRequestedConsumer.java` | listener methods | Consumer. |
| 3 | `common/messaging/ManualAckConsumerSupport.java` | `handle` | Ack/retry support. |
| 4 | `NotificationRequestMaterializer.java` | `materialize` | Materializer. |
| 6 | `JdbcNotificationQueueRepository.java` | `queue` | DB insert. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Event queues | `irms.notification.*.q` | `NotificationRequestedConsumer.java` | RabbitMQ only when enabled. |
| Data | `notification_messages`, `notification_deliveries` | `JdbcNotificationQueueRepository.java` | Queue/inbox. |

#### Flow: Map event thành notification message

##### Mục đích
Chọn message title/body/type/payload đúng theo event.

##### Trigger
`NotificationRequestMaterializer.materialize`.

##### Step-by-step
1. Materializer nhận `EventEnvelope`.
2. Iterate `typedMappers`.
3. Mapper nào `supports(envelope)` thì `map(envelope)`.
4. Nếu không mapper nào support, `genericCommand` dùng payload fields hoặc fallback theo event type.
5. Queue command.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 2 | `NotificationRequestMaterializer.java` | `typedMappers.stream()` | Mapper selection. |
| 3 | `DishReadyNotificationMapper.java` | `supports`, `map` | Kitchen ready. |
| 3 | `LowStockNotificationMapper.java` | `supports`, `map` | Low stock. |
| 3 | `PaymentCompletedNotificationMapper.java` | `supports`, `map` | Payment. |
| 3 | `RefundIssuedNotificationMapper.java` | `supports`, `map` | Refund. |
| 3 | `ReservationSeatedNotificationMapper.java` | `supports`, `map` | Reservation seated. |
| 4 | `NotificationRequestMaterializer.java` | `genericCommand` | Fallback. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Event | `ServiceEventTypes.*` | `NotificationRequestMaterializer.java` | Used in fallback title. |

#### Flow: Channel xử lý notification

##### Mục đích
Validate channel trước khi queue notification.

##### Trigger
`JdbcNotificationQueueRepository.queue`.

##### Step-by-step
1. Queue repository normalize channel.
2. Lookup adapter theo channel.
3. Adapter validate command.
4. Repository insert message/delivery with status `queued`/`QUEUED`.
5. Chưa thấy worker gửi email/SMS thật trong source code đọc được; channel adapters hiện chủ yếu validate.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `JdbcNotificationQueueRepository.java` | `normalizeChannel` | Validate required. |
| 2 | `JdbcNotificationQueueRepository.java` | adapter map | Resolve adapter. |
| 3 | `InAppNotificationChannelAdapter.java`, `EmailNotificationChannelAdapter.java`, `SmsNotificationChannelAdapter.java`, `PrintNotificationChannelAdapter.java`, `PhoneNotificationChannelAdapter.java` | `validate` | Channel validation. |
| 4 | `JdbcNotificationQueueRepository.java` | `queue` | Insert DB rows. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Channel | `in_app`, `email`, `sms`, `print`, `phone` | channel adapter files | Chưa tìm thấy external delivery provider trong source code. |

## 6. Billing Service

### 6.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Billing overview, create/update bill, apply promotion, split bill, process payment, receipt/PDF, refund request/review/execute, publish billing events/notifications/reporting. |
| API public chính | `/api/billing/**`, `/api/bills/**`, `/api/payments/**`, `/api/refunds/**`. |
| Internal API nếu có | Chưa tìm thấy `/internal/billing/**` controller trong source code. |
| Service khác gọi vào | Gateway FE APIs; billing consumes order cancelled events. |
| Service này gọi ra | Ordering internal promotion API; identity policy/default branch; notification command publisher; outbox events. |
| FE màn hình/flow liên quan | `_app.billing.tsx`, `_app.tables.$tableId.tsx` links to billing session. |

### 6.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `_app.billing.tsx` | Load billing overview | `GET /api/billing/overview?billId=&sessionId=` | billing route → billing | `BillingController.java` | `overview` | `BillingService.load` | Loads current bill, sessions, recent bills, refund queue. |
| `_app.billing.tsx` | Create bill | `POST /api/bills` | `/api/bills` → billing | `BillsController.java` | `createBill` | `BillingBillCreationService.createBill` | From table session. |
| `_app.billing.tsx` | Update bill | `PATCH /api/bills/{billId}` | billing | `BillsController.java` | `updateBill` | `BillingAdjustmentService.updateBill` | Tip/discount. |
| `_app.billing.tsx` | Apply promotion | `POST /api/bills/{billId}/promotions` | billing | `BillsController.java` | `applyPromotion` | `BillingAdjustmentService.applyPromotion` | Calls ordering internal promotion apply. |
| `_app.billing.tsx` | Split bill | `POST /api/bills/{billId}/splits` | billing | `BillsController.java` | `splitBill` | `BillingSplitService.splitBill` | Split strategies. |
| `_app.billing.tsx` | Process payment | `POST /api/bills/{billId}/payments` | billing | `BillsController.java` | `pay` | `BillingPaymentService.processPayment` | Gateway selected by method. |
| `_app.billing.tsx` | Issue receipt | `POST /api/payments/{paymentId}/receipt` | billing | `PaymentsController.java` | `receipt` | `BillingReceiptService.issueReceipt` | Print/email/etc. |
| `_app.billing.tsx` | Download PDF receipt | `GET /api/payments/{paymentId}/receipt/document` | billing | `PaymentsController.java` | `receiptDocument` | `BillingReceiptService.buildReceiptDocument` | Direct `fetch`, returns PDF. |
| `_app.billing.tsx` | Request refund | `POST /api/payments/{paymentId}/refunds` | billing | `PaymentsController.java` | `refund` | `BillingRefundService.refundPayment` | May execute or queue review. |
| `_app.billing.tsx` | Pending refunds | `GET /api/refunds/pending` | billing | `RefundsController.java` | `pendingRefunds` | `BillingRefundService.pendingRefunds` | Manager queue. |
| `_app.billing.tsx` | Approve/reject refund | `POST /api/refunds/{refundId}/approval` | billing | `RefundsController.java` | `approveRefund` | `BillingRefundReviewService.reviewRefund` | Executes or rejects. |

### 6.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| GET | `/api/billing/overview` | `BillingController` | `overview` | `billId?`, `sessionId?` | `BillingOverview` | Public. |
| POST | `/api/bills` | `BillsController` | `createBill` | `CreateBillBody(tableSessionId)` | `BillView` | Create/finalize bill. |
| PATCH | `/api/bills/{billId}` | `BillsController` | `updateBill` | `UpdateBillBody` | `BillView` | Tip/discount. |
| POST | `/api/bills/{billId}/promotions` | `BillsController` | `applyPromotion` | `PromotionBody(code)` | `BillView` | Calls ordering. |
| POST | `/api/bills/{billId}/splits` | `BillsController` | `splitBill` | `SplitBody` | `BillView` | Split bill. |
| POST | `/api/bills/{billId}/payments` | `BillsController` | `pay` | `PaymentBody` | `PaymentView` | Payment. |
| POST | `/api/payments/{paymentId}/receipt` | `PaymentsController` | `receipt` | `ReceiptBody` | `ReceiptView` | Issue receipt. |
| GET | `/api/payments/{paymentId}/receipt/document` | `PaymentsController` | `receiptDocument` | path | PDF bytes | Download receipt PDF. |
| POST | `/api/payments/{paymentId}/refunds` | `PaymentsController` | `refund` | `RefundBody` | `RefundView` | Request/execute refund. |
| GET | `/api/refunds/pending` | `RefundsController` | `pendingRefunds` | none | refund list | Manager queue. |
| POST | `/api/refunds/{refundId}/approval` | `RefundsController` | `approveRefund` | `RefundApprovalBody` | `RefundView` | Approve/reject. |

### 6.4 Các file quan trọng và chức năng của chúng

#### 6.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `runtime/billing/BillingServiceApplication.java` | Entry service, scan billing/common/pdf adapter. |
| `application-billing-service.properties` | Port `8083`, ordering/identity URLs. |

#### 6.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `billing/api/BillingController.java` | `overview` | Billing overview. |
| `billing/api/BillsController.java` | `createBill`, `updateBill`, `applyPromotion`, `splitBill`, `pay` | Bill operations. |
| `billing/api/PaymentsController.java` | `receipt`, `receiptDocument`, `refund` | Payment receipt/refund. |
| `billing/api/RefundsController.java` | `pendingRefunds`, `approveRefund` | Refund review. |

#### 6.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `BillingService.java` | facade methods | Billing facade. |
| `BillingOverviewService.java` | `load` | Overview read composition. |
| `BillingBillCreationService.java` | `createBill` | Create bill from table session/order items. |
| `BillingAdjustmentService.java` | `updateBill`, `applyPromotion` | Tip/discount/promotion. |
| `BillingSplitService.java` | `splitBill` | Split strategies. |
| `BillingPaymentService.java` | `processPayment` | Payment processing and event. |
| `BillingReceiptService.java` | `issueReceipt`, `buildReceiptDocument` | Receipt DB/delivery/PDF/event. |
| `BillingRefundService.java` | refund facade | Refund request/review. |
| `BillingRefundRequestService.java` | `refundPayment` | Execute or queue review. |
| `BillingRefundReviewService.java` | `reviewRefund` | Approve/reject queued refund. |
| `BillingRefundExecutionService.java` | `executeRefund` | Refund execution and status updates. |

#### 6.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `JdbcBillingOverviewQueryRepository.java` | bills/table sessions | Load billable sessions/recent bills. |
| `JdbcBillIssueRepository.java` | bills, bill lines, splits, table sessions | Create bill. |
| `JdbcBillingBillQueryRepository.java` | bill read model | Bill detail/current bill resolution. |
| `JdbcBillingAdjustmentRepository.java` | bill totals/splits | Tip/discount sync. |
| `JdbcBillingSplitRepository.java` | bill splits/allocations | Split bill. |
| `JdbcBillPaymentRepository.java` | payments/bill status | Record payment and settlement. |
| `JdbcBillingPaymentQueryRepository.java` | payments/receipts | Payment/receipt reads. |
| `JdbcBillingReceiptRepository.java` | receipts | Insert/update receipt. |
| `JdbcBillingRefundRequestRepository.java` | refunds | Create pending refund. |
| `JdbcBillingRefundReviewRepository.java` | refunds | Mark rejected. |
| `JdbcRefundExecutionRepository.java` | refunds/payments/bills | Insert/update refund, refunded statuses. |

#### 6.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `billing/integration/RemotePromotionApplicationClient.java` | Remote client | Calls ordering `/internal/ordering/promotions/apply`. |
| `billing/infrastructure/payment/CashPaymentGateway.java` | Adapter | Cash payment/refund support. |
| `billing/infrastructure/payment/ExternalReferencePaymentGateway.java` | Adapter | Card/mobile/gift external reference style payment. |
| `adapters/pdf/ReceiptPdfRenderer.java` | Adapter | Render receipt PDF bytes. |
| `billing/application/workflow/PaymentCompletedConsumer.java` | Event consumer | Consumes payment/refund events. |
| `billing/infrastructure/workflow/JdbcBillingOrderCancelledConsumer.java` | Event consumer | Handles order cancelled. |
| `BillingNotificationRequestStep.java`, `BillingReportingProjectionStep.java`, `BillingAuditRequestStep.java`, `BillingReceiptRequestStep.java` | Event workflow step | Settlement follow-up actions. |

### 6.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `BillingBillCreationService.java` | `createBill` | tableSessionId | `BillView` | Ensure no unready items, load order lines, calculate tax/service fee, insert bill/lines/split, publish `BillIssuedEvent` | `BillIssueRepository`, identity tax policy, outbox. |
| `BillingAdjustmentService.java` | `applyPromotion` | billId, code | `BillView` | Apply discount from ordering promotion | `RemotePromotionApplicationClient`, `BillAdjustmentRepository`. |
| `BillingSplitService.java` | `splitBill` | billId, split request | `BillView` | Clear/rebuild bill splits | `BillSplitRepository`, split strategies. |
| `BillingPaymentService.java` | `processPayment` | billId, payment request, actor | `PaymentView` | Validate outstanding, authorize gateway, record payment, update bill status, publish event | `BillPaymentRepository`, `PaymentGateway`, outbox. |
| `BillingReceiptService.java` | `issueReceipt` | paymentId, receipt request | `ReceiptView` | Insert/update receipt, deliver, publish event | `BillingReceiptRepository`, delivery adapters, outbox. |
| `BillingReceiptService.java` | `buildReceiptDocument` | paymentId | PDF bytes | Build receipt lines and render PDF | `ReceiptPdfRenderer`, identity default branch. |
| `BillingRefundRequestService.java` | `refundPayment` | paymentId, amount/reason, actor | `RefundView` | Normalize amount, enforce window/limit, execute or queue review | `BillingRefundExecutionService`, `RefundRequestRepository`. |
| `BillingRefundReviewService.java` | `reviewRefund` | refundId, approve/reject | `RefundView` | Manager approve/reject pending refund | `BillingRefundExecutionService`, `RefundReviewRepository`. |

### 6.6 Các flow chính của service

#### Flow: Billing overview

##### Mục đích
Load current bill, billable sessions, recent bills, refund queue.

##### Trigger
FE `_app.billing.tsx` query.

##### Step-by-step
1. FE gọi `GET /api/billing/overview?billId=&sessionId=`.
2. Gateway route sang billing.
3. `BillingController.overview` require `billing.manage`.
4. `BillingService.load` gọi `BillingOverviewService.load`.
5. Service resolve current bill by `billId` or `tableSessionId`, load overview data.
6. Response được FE map và render billing screen.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | billing query | FE. |
| 3 | `billing/api/BillingController.java` | `overview` | API. |
| 4 | `billing/application/BillingService.java` | `load` | Facade. |
| 5 | `BillingOverviewService.java` | `load` | Overview service. |
| 5 | `JdbcBillingOverviewQueryRepository.java` | `loadBillableSessions`, `loadRecentBills` | DB reads. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/billing/overview` | `BillingController.java` | Public. |

#### Flow: Create bill

##### Mục đích
Finalize bill từ table session/order items.

##### Trigger
FE create bill from billable session.

##### Step-by-step
1. FE gọi `POST /api/bills` với `tableSessionId`.
2. `BillsController.createBill` require `billing.manage`.
3. `BillingBillCreationService.createBill` return existing bill nếu đã có.
4. Service lấy tax policy từ identity.
5. Repository đếm unready kitchen items; nếu còn pending/cooking/blocked/held thì conflict.
6. Load unsettled order item sources.
7. Calculate subtotal, tax, service fee, grand total.
8. Insert bill, bill lines, full split; update table session to billing.
9. Publish `BillIssuedEvent`, return bill view.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | `createBillMutation` | FE. |
| 2 | `billing/api/BillsController.java` | `createBill` | API. |
| 3 | `BillingBillCreationService.java` | `createBill` | Business. |
| 4 | `common/identity/RemoteSharedIdentityPolicyClient.java` | `getPolicySnapshot` | Tax policy. |
| 5 | `JdbcBillIssueRepository.java` | `countUnreadyItems`, `loadBillLineSources` | DB reads. |
| 8 | `JdbcBillIssueRepository.java` | `insertBill`, `insertBillLine`, `insertFullBillSplit`, `updateTableSessionToBilling` | DB writes. |
| 9 | `BillIssuedEvent.java` | event | Outbox. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/bills` | `BillsController.java` | Public. |
| Event | `BillIssuedEvent` | `BillingBillCreationService.java` | Reporting/audit. |

#### Flow: Update bill

##### Mục đích
Manual tip/discount adjustment.

##### Trigger
FE edit bill dialog.

##### Step-by-step
1. FE gọi `PATCH /api/bills/{billId}` với `tipAmount`, `discountAmount`.
2. `BillsController.updateBill` require `billing.manage`.
3. `BillingAdjustmentService.updateBill` update totals.
4. Repository sync split if needed.
5. Audit/event behavior đọc được trong service; response bill view.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | `updateBillMutation` | FE. |
| 2 | `BillsController.java` | `updateBill` | API. |
| 3 | `BillingAdjustmentService.java` | `updateBill` | Business. |
| 4 | `JdbcBillingAdjustmentRepository.java` | `updateBillTotals`, `syncSingleSplit` | DB. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `PATCH /api/bills/{billId}` | `BillsController.java` | Public. |

#### Flow: Apply promotion

##### Mục đích
Apply promo code từ ordering service vào bill.

##### Trigger
FE billing promo dialog.

##### Step-by-step
1. FE gọi `POST /api/bills/{billId}/promotions` với code.
2. `BillsController.applyPromotion` gọi `BillingService.applyPromotion`.
3. `BillingAdjustmentService.applyPromotion` gọi `PromotionGatewayPort`.
4. `RemotePromotionApplicationClient.applyPromotion` gọi `POST /internal/ordering/promotions/apply`.
5. `OrderingIntegrationController.applyPromotion` gọi `MenuService.applyPromotion`, trả discount.
6. Billing update bill discount/totals and return bill view.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | `applyPromoMutation` | FE. |
| 2 | `BillsController.java` | `applyPromotion` | API. |
| 3 | `BillingAdjustmentService.java` | `applyPromotion` | Billing logic. |
| 4 | `billing/integration/RemotePromotionApplicationClient.java` | `applyPromotion` | Remote call. |
| 5 | `ordering/api/OrderingIntegrationController.java` | `applyPromotion` | Ordering receiver. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/bills/{billId}/promotions` | `BillsController.java` | Public. |
| Internal API | `POST /internal/ordering/promotions/apply` | `RemotePromotionApplicationClient.java` | Billing → ordering. |

#### Flow: Split bill

##### Mục đích
Tạo splits theo method/count/amounts/tipAmounts.

##### Trigger
FE split bill action.

##### Step-by-step
1. FE gọi `POST /api/bills/{billId}/splits`.
2. `BillsController.splitBill` gọi `BillingService.splitBill`.
3. `BillingSplitService.splitBill` validate no paid splits conflict, clear old splits.
4. Strategy tương ứng (`EqualSplitStrategy`, `AmountSplitStrategy`, `ItemSplitStrategy`, `SeatSplitStrategy`) tính allocations.
5. Repository insert splits/allocations.
6. Response bill view.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | split mutation | FE. |
| 2 | `BillsController.java` | `splitBill` | API. |
| 3 | `BillingSplitService.java` | `splitBill` | Split business. |
| 4 | `billing/domain/*SplitStrategy.java` | strategy methods | Domain split. |
| 5 | `JdbcBillingSplitRepository.java` | `clearSplits`, `insertSplit`, `insertSplitAllocation` | DB. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/bills/{billId}/splits` | `BillsController.java` | Public. |

#### Flow: Process payment

##### Mục đích
Ghi nhận thanh toán và cập nhật bill settlement.

##### Trigger
FE process payment dialog.

##### Step-by-step
1. FE gọi `POST /api/bills/{billId}/payments`.
2. `BillsController.pay` require `payments.process`.
3. `BillingPaymentService.processPayment` normalize method.
4. Repository calculate outstanding cho bill/split.
5. Validate cash received nếu cash.
6. Resolve `PaymentGateway` theo method, authorize.
7. Repository record payment; mark split paid nếu có.
8. Recalculate remaining; update bill settlement `paid` hoặc `partially_paid`.
9. Nếu authorization status `completed`, publish `PaymentCompletedEvent`.
10. Response `PaymentView`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | `paymentMutation` | FE. |
| 2 | `BillsController.java` | `pay` | API. |
| 3 | `BillingPaymentService.java` | `processPayment` | Business. |
| 6 | `CashPaymentGateway.java`, `ExternalReferencePaymentGateway.java` | `authorize` | Payment adapter. |
| 7 | `JdbcBillPaymentRepository.java` | `recordPayment`, `markSplitPaid` | DB. |
| 9 | `PaymentCompletedEvent.java` | event | Outbox. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/bills/{billId}/payments` | `BillsController.java` | Public. |
| Event | `PaymentCompletedEvent` | `BillingPaymentService.java` | Reporting/notification/audit. |

#### Flow: Generate receipt

##### Mục đích
Issue receipt, deliver via channel, optionally generate PDF document.

##### Trigger
FE print/email receipt hoặc PDF download.

##### Step-by-step
1. FE issue receipt: `POST /api/payments/{paymentId}/receipt`.
2. `PaymentsController.receipt` calls `BillingReceiptService.issueReceipt`.
3. Service resolve receipt delivery adapter, validate recipient/channel.
4. Insert or update receipt.
5. Adapter `deliver`, then publish `ReceiptGeneratedEvent`.
6. FE PDF: direct fetch `GET /api/payments/{paymentId}/receipt/document`.
7. `BillingReceiptService.buildReceiptDocument` loads receipt/payment/bill/default branch, builds text lines.
8. `ReceiptPdfRenderer.render` returns PDF bytes.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | `receiptMutation` | FE issue. |
| 2 | `PaymentsController.java` | `receipt` | API. |
| 3 | `BillingReceiptService.java` | `issueReceipt` | Receipt workflow. |
| 4 | `JdbcBillingReceiptRepository.java` | `insertReceipt`, `updateReceiptDelivery` | DB. |
| 5 | `ReceiptGeneratedEvent.java` | event | Outbox. |
| 6 | `_app.billing.tsx` | `downloadReceiptDocument` | FE PDF. |
| 7 | `BillingReceiptService.java` | `buildReceiptDocument` | PDF data. |
| 8 | `adapters/pdf/ReceiptPdfRenderer.java` | `render` | PDF render. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/payments/{paymentId}/receipt` | `PaymentsController.java` | Issue. |
| API | `GET /api/payments/{paymentId}/receipt/document` | `PaymentsController.java` | PDF. |
| Event | `ReceiptGeneratedEvent` | `BillingReceiptService.java` | Notification/reporting/audit. |

#### Flow: Request/review/execute refund

##### Mục đích
Hoàn tiền trực tiếp hoặc queue manager review nếu vượt policy.

##### Trigger
FE refund/refund review.

##### Step-by-step
1. FE request refund: `POST /api/payments/{paymentId}/refunds`.
2. `BillingRefundRequestService.refundPayment` load payment, calculate refundable, normalize amount.
3. Lấy authorization policy từ identity, enforce refund window.
4. Nếu amount > max refund limit và actor không có quyền approve, create pending refund và enqueue notifications cho manager/admin.
5. Nếu được phép, `BillingRefundExecutionService.executeRefund` thực thi refund, update payment/bill status, publish `RefundIssuedEvent`.
6. FE manager load `GET /api/refunds/pending`.
7. FE approve/reject gọi `POST /api/refunds/{refundId}/approval`.
8. `BillingRefundReviewService.reviewRefund` require manager/admin; reject thì mark rejected + notification; approve thì execute refund.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.billing.tsx` | `refundMutation` | FE request. |
| 2 | `BillingRefundRequestService.java` | `refundPayment` | Refund request. |
| 4 | `JdbcBillingRefundRequestRepository.java` | `createPendingRefund` | Pending refund. |
| 4 | `NotificationCommandPublisher.java` | `enqueue` | Manager/admin notification. |
| 5 | `BillingRefundExecutionService.java` | `executeRefund` | Execute refund. |
| 6 | `RefundsController.java` | `pendingRefunds` | Queue API. |
| 7 | `_app.billing.tsx` | `refundReviewMutation` | FE review. |
| 8 | `BillingRefundReviewService.java` | `reviewRefund` | Approve/reject. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `POST /api/payments/{paymentId}/refunds` | `PaymentsController.java` | Public. |
| API | `GET /api/refunds/pending` | `RefundsController.java` | Public. |
| API | `POST /api/refunds/{refundId}/approval` | `RefundsController.java` | Public. |
| Event | `RefundIssuedEvent` | `BillingRefundExecutionService.java` | Published on execution. |
| Notification | `REFUND_REVIEW_REQUIRED`, `REFUND_REJECTED` | `BillingRefundRequestService.java`, `BillingRefundReviewService.java` | Queued. |

## 7. Inventory Service

### 7.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Inventory overview, ingredient CRUD, stock transaction, kitchen consumption, low stock evaluation, reorder suggestion, low-stock notification/event. |
| API public chính | `/api/inventory`, `/api/inventory/items/**`, `/api/inventory/alerts/{alertId}/acknowledge`. |
| Internal API nếu có | `/internal/inventory/consumption/kitchen-start`. |
| Service khác gọi vào | Kitchen calls consumption when item starts cooking. |
| Service này gọi ra | Publishes `InventoryStockChangedEvent`, `LowStockDetectedEvent`, manager notification command. |
| FE màn hình/flow liên quan | `_app.inventory.tsx`; dashboard/reporting consume inventory projections. |

### 7.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `_app.inventory.tsx` | Load inventory overview | `GET /api/inventory` | `/api/inventory` → inventory | `InventoryController.java` | `inventory` | `InventoryService.load` | Items/transactions/reorder/alerts. |
| `_app.inventory.tsx` | Create item | `POST /api/inventory/items` | inventory | `InventoryController.java` | `createIngredient` | `InventoryIngredientService.createIngredient` | Ingredient create. |
| `_app.inventory.tsx` | Update item/stock | `PUT /api/inventory/items/{id}` | inventory | `InventoryController.java` | `updateIngredient` | `InventoryIngredientService.updateIngredient` | May record manual adjustment. |
| `_app.inventory.tsx` | Delete item | `DELETE /api/inventory/items/{id}` | inventory | `InventoryController.java` | `deleteIngredient` | `InventoryIngredientService.deleteIngredient` | Checks dependent recipes. |
| `_app.inventory.tsx` | Acknowledge alert | `PATCH /api/inventory/alerts/{id}/acknowledge` | inventory | `InventoryController.java` | `acknowledgeAlert` | `InventoryStockService.acknowledgeAlert` | Alert status update. |
| Chưa tìm thấy FE caller trực tiếp trong frontend source. | Kitchen consumption | `POST /internal/inventory/consumption/kitchen-start` | Internal | `InventoryIntegrationController.java` | `consumeForKitchenStart` | `InventoryStockService.consumeForKitchenStart` | Kitchen → inventory. |

### 7.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| GET | `/api/inventory` | `InventoryController` | `inventory` | none | `InventoryOverview` | Public. |
| POST | `/api/inventory/items` | `InventoryController` | `createIngredient` | `InventoryItemBody` | `IngredientView` | Create. |
| PUT | `/api/inventory/items/{inventoryItemId}` | `InventoryController` | `updateIngredient` | `InventoryItemBody` | `IngredientView` | Update. |
| DELETE | `/api/inventory/items/{inventoryItemId}` | `InventoryController` | `deleteIngredient` | path | `EntityReferenceResponse` | Delete. |
| PATCH | `/api/inventory/alerts/{alertId}/acknowledge` | `InventoryController` | `acknowledgeAlert` | path | `EntityStatusResponse` | Alert ack. |
| POST | `/internal/inventory/consumption/kitchen-start` | `InventoryIntegrationController` | `consumeForKitchenStart` | `KitchenStartConsumptionRequest` | 202 | Kitchen start consumption. |

### 7.4 Các file quan trọng và chức năng của chúng

#### 7.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `runtime/inventory/InventoryServiceApplication.java` | Entry service, scan inventory + common. |
| `application-inventory-service.properties` | Port `8085`, identity URL. |

#### 7.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `inventory/api/InventoryController.java` | inventory/items/alerts methods | Public inventory APIs. |
| `inventory/api/InventoryIntegrationController.java` | `consumeForKitchenStart` | Internal kitchen consumption API. |

#### 7.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `InventoryService.java` | facade methods | Inventory facade. |
| `InventoryReadService.java` | `load`, `findIngredient` | Overview/detail reads. |
| `InventoryIngredientService.java` | `createIngredient`, `updateIngredient`, `deleteIngredient` | Ingredient CRUD and manual adjustment. |
| `InventoryStockService.java` | `recordManualAdjustment`, `consumeForKitchenStart`, `acknowledgeAlert` | Stock facade. |
| `InventoryKitchenConsumptionService.java` | `consumeForKitchenStart` | Deduct recipe usage. |
| `InventoryStockTransactionService.java` | `recordTransaction` | Lock stock, update on hand, insert transaction, publish event. |
| `InventoryLowStockAlertService.java` | `evaluateLowStock`, `acknowledgeAlert` | Low stock alert. |
| `inventory/application/workflow/InventoryAlertMediator.java` | `handleStockChanged`, `handleReorderSuggestion` | Low-stock workflow. |

#### 7.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `JdbcInventoryQueryRepository.java` | inventory read model | Overview. |
| `JdbcInventoryIngredientRepository.java` | inventory items/reorder rules | Ingredient CRUD. |
| `JdbcInventoryKitchenConsumptionRepository.java` | recipes/stock consumption | Load recipe usages, idempotency check. |
| `JdbcInventoryStockTransactionRepository.java` | stock transactions/inventory item | Lock stock, update on hand, insert transaction. |
| `JdbcInventoryLowStockAlertRepository.java` | low stock alerts | Snapshot/open alert/ack. |
| `JdbcReorderSuggestionAdapter.java` | reorder suggestions | Upsert suggestion. |
| `JdbcStockTransactionQueryRepository.java`, `JdbcLowStockAlertQueryRepository.java`, `JdbcReorderRecommendationQueryRepository.java` | read models | Overview supporting data. |

#### 7.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `InventoryStockChangedConsumer.java` | Event consumer | Handles stock changed/reorder suggestion queues. |
| `PublishLowStockDetectedStep.java` | Event publisher | Publishes low stock detected. |
| `RequestManagerNotificationStep.java` | Event publisher/adapter | Requests manager notification. |
| `InventoryStockChangedEvent.java` | Event | Stock changed. |
| `LowStockDetectedEvent.java` | Event | Low stock detected. |
| `inventory/infrastructure/workflow/JdbcInventoryOrderCancelledConsumer.java` | Event consumer | Handles order cancelled inventory workflow. |

### 7.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `InventoryService.java` | `load` | none | `InventoryOverview` | Facade overview | `InventoryReadService.load`. |
| `InventoryIngredientService.java` | `updateIngredient` | itemId, upsert, actor | `IngredientView` | Update item/reorder, may record stock adjustment/audit | `InventoryStockService.recordManualAdjustment`. |
| `InventoryStockService.java` | `consumeForKitchenStart` | orderItemId, actor, correlation | void | Delegates kitchen recipe deduction | `InventoryKitchenConsumptionService`. |
| `InventoryKitchenConsumptionService.java` | `consumeForKitchenStart` | orderItemId | void | Load recipe usages and record negative stock transactions | `InventoryStockTransactionService.recordTransaction`. |
| `InventoryStockTransactionService.java` | `recordTransaction` | itemId, delta, reason, source | boolean | Idempotent stock change, publish event | `InventoryStockTransactionRepository`, `DomainEventPublisher`. |
| `InventoryAlertMediator.java` | `handleStockChanged` | event | void | Evaluate low stock, publish low-stock, request notification | workflow steps. |

### 7.6 Các flow chính của service

#### Flow: Inventory overview

##### Mục đích
Hiển thị items, transactions, reorder recommendations, alerts.

##### Trigger
FE `_app.inventory.tsx`.

##### Step-by-step
1. FE gọi `GET /api/inventory`.
2. Gateway route sang inventory.
3. `InventoryController.inventory` require `inventory.manage`.
4. `InventoryService.load` gọi `InventoryReadService.load`.
5. Repository load overview data.
6. Response `InventoryOverview`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.inventory.tsx` | inventory query | FE. |
| 3 | `inventory/api/InventoryController.java` | `inventory` | API. |
| 4 | `InventoryService.java` | `load` | Facade. |
| 5 | `JdbcInventoryQueryRepository.java` | `load` | DB. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/inventory` | `InventoryController.java` | Public. |

#### Flow: Stock update

##### Mục đích
Create/update/delete ingredient và cập nhật stock level.

##### Trigger
FE inventory item form/actions.

##### Step-by-step
1. Create: FE gọi `POST /api/inventory/items`.
2. `InventoryIngredientService.createIngredient` tạo ingredient/reorder rule.
3. Update: FE gọi `PUT /api/inventory/items/{id}`.
4. `InventoryIngredientService.updateIngredient` cập nhật metadata/current/min/max/cost/category.
5. Nếu current thay đổi, service có thể record manual adjustment qua stock transaction service.
6. Delete: FE gọi `DELETE /api/inventory/items/{id}`, service kiểm tra dependent recipes rồi delete.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `_app.inventory.tsx` | item mutation | FE. |
| 2 | `InventoryController.java` | `createIngredient` | API. |
| 2 | `InventoryIngredientService.java` | `createIngredient` | Business. |
| 4 | `InventoryIngredientService.java` | `updateIngredient` | Business. |
| 5 | `InventoryStockTransactionService.java` | `recordTransaction` | Stock transaction. |
| 6 | `JdbcInventoryIngredientRepository.java` | `countDependentRecipes`, `deleteIngredient` | DB. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `/api/inventory/items/**` | `InventoryController.java` | Public. |
| Event | `InventoryStockChangedEvent` | `InventoryStockTransactionService.java` | On stock transaction. |

#### Flow: Kitchen consumption

##### Mục đích
Trừ stock khi kitchen bắt đầu nấu món.

##### Trigger
Kitchen item chuyển `cooking`.

##### Step-by-step
1. Kitchen gọi `POST /internal/inventory/consumption/kitchen-start`.
2. `InventoryIntegrationController.consumeForKitchenStart` gọi `InventoryService.consumeForKitchenStart`.
3. `InventoryStockService.consumeForKitchenStart` gọi `InventoryKitchenConsumptionService`.
4. Consumption service load recipe usages by `orderItemId`.
5. Với từng ingredient, record negative stock transaction với source ref/order item để idempotent.
6. `InventoryStockTransactionService` lock stock, check duplicate source, update on hand, insert transaction, publish `InventoryStockChangedEvent`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `kitchen/integration/RemoteInventoryConsumptionClient.java` | `consumeForKitchenStart` | Remote caller. |
| 2 | `inventory/api/InventoryIntegrationController.java` | `consumeForKitchenStart` | Internal API. |
| 3 | `InventoryStockService.java` | `consumeForKitchenStart` | Facade. |
| 4 | `InventoryKitchenConsumptionService.java` | `consumeForKitchenStart` | Recipe deduction. |
| 4 | `JdbcInventoryKitchenConsumptionRepository.java` | `loadRecipeUsages`, `hasRecordedKitchenStartConsumption` | DB. |
| 6 | `InventoryStockTransactionService.java` | `recordTransaction` | Stock transaction/event. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Internal API | `/internal/inventory/consumption/kitchen-start` | `InventoryIntegrationController.java` | Kitchen → inventory. |
| Event | `InventoryStockChangedEvent` | `InventoryStockTransactionService.java` | Triggers low-stock/reporting. |

#### Flow: Low-stock detection

##### Mục đích
Tạo alert/reorder suggestion khi stock thấp.

##### Trigger
`InventoryStockChangedEvent` consumer.

##### Step-by-step
1. Stock transaction publishes `InventoryStockChangedEvent`.
2. `InventoryStockChangedConsumer.lowStock` consumes queue.
3. `InventoryAlertMediator.handleStockChanged` parse inventory item ID.
4. `EvaluateLowStockStep.evaluate` reads stock level/threshold.
5. Nếu không low stock thì return.
6. Workflow start, publish low stock detected, request manager notification, mark detected.
7. Reorder suggestion flow uses `handleReorderSuggestion` and `CreateReorderSuggestionStep`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `InventoryStockTransactionService.java` | `publishStockChanged` | Event source. |
| 2 | `InventoryStockChangedConsumer.java` | `lowStock`, `reorderSuggestion` | Consumer. |
| 3 | `InventoryAlertMediator.java` | `handleStockChanged` | Workflow. |
| 4 | `EvaluateLowStockStep.java` | `evaluate` | Threshold evaluation. |
| 6 | `PublishLowStockDetectedStep.java` | `execute` | Low stock event. |
| 6 | `RequestManagerNotificationStep.java` | `execute` | Manager notification. |
| 7 | `CreateReorderSuggestionStep.java` | `execute` | Reorder suggestion. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Event | `InventoryStockChangedEvent` | `InventoryStockTransactionService.java` | Trigger. |
| Event | `LowStockDetectedEvent` | `PublishLowStockDetectedStep.java` | Low-stock event. |
| Data | low_stock_alert/reorder suggestion | `JdbcInventoryLowStockAlertRepository.java`, `JdbcReorderSuggestionAdapter.java` | Alert/recommendation. |

#### Flow: Publish low-stock notification/event

##### Mục đích
Thông báo manager/admin khi tồn kho thấp.

##### Trigger
Low-stock detection workflow.

##### Step-by-step
1. `InventoryAlertMediator.handleStockChanged` có evaluation low stock.
2. `PublishLowStockDetectedStep.execute` publish low-stock detected event.
3. `RequestManagerNotificationStep.execute` enqueue notification command.
4. Notification service consumes/queues message.
5. Reporting consumes inventory/low-stock events for projection.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `InventoryAlertMediator.java` | `handleStockChanged` | Workflow. |
| 2 | `PublishLowStockDetectedStep.java` | `execute` | Event publish. |
| 3 | `RequestManagerNotificationStep.java` | `execute` | Notification command. |
| 4 | `notification/infrastructure/messaging/NotificationRequestedConsumer.java` | `lowStockNotification` | Notification consumer. |
| 5 | `reporting/infrastructure/messaging/ReportingProjectionConsumer.java` | `lowStock`, `inventoryStockChanged` | Reporting projection. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Event | `LowStockDetectedEvent` | `inventory/application/events/LowStockDetectedEvent.java` | Notification/reporting/audit. |
| Notification | low stock | `RequestManagerNotificationStep.java` | Manager notification. |

## 8. Reporting Service

### 8.1 Service này làm gì?

| Nội dung | Mô tả |
| -------- | ----- |
| Trách nhiệm chính | Dashboard, operations report, typed reports, export CSV/JSON/PDF, consume service events to materialize reporting projections/snapshots. |
| API public chính | `/api/dashboard`, `/api/reports/**`. |
| Internal API nếu có | Chưa tìm thấy `/internal/reporting/**` controller trong source code. |
| Service khác gọi vào | Gateway FE APIs; RabbitMQ event streams from ordering/kitchen/billing/reservation/inventory/menu/staff. |
| Service này gọi ra | Identity directory for server display names in dashboard. |
| FE màn hình/flow liên quan | `_app.index.tsx` dashboard, `_app.reports.tsx` reports/export. |

### 8.2 API mapping: FE → Gateway → BE

| FE file/component/hook | FE action | API gọi | Gateway route | BE Controller | Controller method | Service method chính | Ghi chú |
| ---------------------- | --------- | ------- | ------------- | ------------- | ----------------- | -------------------- | ------- |
| `_app.index.tsx` | Load dashboard | `GET /api/dashboard` | `/api/dashboard` → reporting | `DashboardController.java` | `dashboard` | `DashboardService.load` | Dashboard KPIs, alerts, active orders. |
| `_app.reports.tsx` | Operations report | `GET /api/reports/operations` | reporting | `ReportingController.java` | `operations` | `ReportingQueryService.operationsReport` | Operations snapshot + live metrics. |
| `_app.reports.tsx` | Sales/peak/best/revenue/kitchen/combo reports | `/api/reports/*` | reporting | `ReportingController.java` | typed methods | `ReportingQueryService.get*Report` | Typed projection rows. |
| `_app.reports.tsx` | Export | `GET /api/reports/export?type=&format=` | reporting | `ReportingController.java` | `export` | `ReportingQueryService.export` | Direct fetch for file. |
| Chưa tìm thấy FE caller trực tiếp trong frontend source. | Projection consumer | RabbitMQ queues | Không qua gateway | `ReportingProjectionConsumer.java` | listener methods | `ReportingProjectionService.recordEventProjection` | Event-driven projections. |

### 8.3 Danh sách API của service

| Method | Path | Controller | Method xử lý | Request | Response | Ghi chú |
| ------ | ---- | ---------- | ------------ | ------- | -------- | ------- |
| GET | `/api/dashboard` | `DashboardController` | `dashboard` | none | `DashboardView` | Public. |
| GET | `/api/reports/operations` | `ReportingController` | `operations` | none | `OperationsReportView` | Public. |
| GET | `/api/reports/sales` | `ReportingController` | `sales` | none | `SalesReportView` | Public. |
| GET | `/api/reports/peak-hours` | `ReportingController` | `peakHours` | none | `PeakHourReportView` | Public. |
| GET | `/api/reports/best-selling-items` | `ReportingController` | `bestSellingItems` | none | `BestSellingItemReportView` | Public. |
| GET | `/api/reports/revenue` | `ReportingController` | `revenue` | none | `RevenueReportView` | Public. |
| GET | `/api/reports/kitchen-bottlenecks` | `ReportingController` | `kitchenBottlenecks` | none | `KitchenBottleneckReportView` | Public. |
| GET | `/api/reports/staff-efficiency` | `ReportingController` | `staffEfficiency` | none | `StaffEfficiencyReportView` | Public. |
| GET | `/api/reports/inventory-usage` | `ReportingController` | `inventoryUsage` | none | `InventoryUsageReportView` | Public. |
| GET | `/api/reports/combo-sales` | `ReportingController` | `comboSales` | none | `ComboSalesReportView` | Public. |
| GET | `/api/reports/export` | `ReportingController` | `export` | `type`, `format` | file bytes | Export CSV/JSON/PDF depending adapter. |
| RabbitMQ | reporting queues | `ReportingProjectionConsumer` | listener methods | `EventEnvelope` | ack/nack | Projection input. |

### 8.4 Các file quan trọng và chức năng của chúng

#### 8.4.1 Runtime/config

| File | Chức năng |
| ---- | --------- |
| `runtime/reporting/ReportingServiceApplication.java` | Entry service, scan reporting + common. |
| `application-reporting-service.properties` | Port `8087`, identity URL. |

#### 8.4.2 Controller/API layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `reporting/api/DashboardController.java` | `dashboard` | Dashboard API. |
| `reporting/api/ReportingController.java` | operations/sales/.../export | Report APIs. |

#### 8.4.3 Service/Application layer

| File | Class/method chính | Chức năng |
| ---- | ------------------ | --------- |
| `DashboardService.java` | `load` | Compose dashboard from operations snapshot, live financials, alerts, active orders. |
| `ReportingQueryService.java` | `operationsReport`, `getSalesReport`, `export` | Query typed reports and export. |
| `ReportingProjectionService.java` | `recordEventProjection`, `refreshOperationsSnapshot` | Event projection recorder. |
| `SalesProjectionHandler.java`, `RevenueProjectionHandler.java`, `PeakHourProjectionHandler.java`, etc. | `supports`, `project` | Typed projection handlers. |
| `CsvReportExporter.java`, `JsonReportExporter.java`, `PdfReportExporter.java` | `export` | File export adapters. |
| `OperationsSnapshotViewMapper.java` | `compose` | Compose operations report snapshot/live metrics. |
| `ReportTableViewFactory.java` | `create` | Converts report views to export table. |

#### 8.4.4 Repository/Persistence layer

| File | Table/entity liên quan | Chức năng |
| ---- | ---------------------- | --------- |
| `JdbcReportSnapshotRepository.java` | report snapshots/projection rows | Snapshot CRUD, record event projection, materialize typed projections, report queries. |
| `JdbcOperationsDashboardQueryRepository.java` | live dashboard tables | Low-stock alerts, audit alerts, active orders, financials, revenue trend. |
| `Jdbc*ProjectionMaterializer.java` | reporting projection tables | Sales, revenue, peak hour, best selling, kitchen bottleneck, staff efficiency, inventory usage, combo sales. |
| `JdbcReportSnapshotCrudRepository.java` | snapshot table | Snapshot persistence. |
| `JdbcReportProjectionQueryRepository.java` | projection rows | Query projection tables. |
| `JdbcProjectionRefreshLogRepository.java` | refresh log | Idempotent snapshot refresh. |

#### 8.4.5 Integration/Event layer nếu có

| File | Loại | Chức năng |
| ---- | ---- | --------- |
| `ReportingProjectionConsumer.java` | Event consumer | Consumes order/kitchen/inventory/billing/reservation/menu/staff queues. |
| `common/identity/RemoteSharedIdentityDirectoryClient.java` | Remote client | Dashboard resolves server display names. |

### 8.5 Function/method quan trọng

| File | Method/function | Input | Output | Chức năng | Gọi tiếp tới đâu |
| ---- | --------------- | ----- | ------ | --------- | ---------------- |
| `DashboardService.java` | `load` | none | `DashboardView` | Load latest operations snapshot, live metrics, financials, alerts, active orders | `ReportingProjectionRepository`, `OperationsDashboardQueryRepository`, identity directory. |
| `ReportingQueryService.java` | `operationsReport` | none | `OperationsReportView` | Load latest operations snapshot and live metrics | `ReportingProjectionRepository`. |
| `ReportingQueryService.java` | `getSalesReport`, `getRevenueReport`, etc. | none | typed report view | Query projection rows and metrics | `ReportingProjectionRepository`. |
| `ReportingQueryService.java` | `export` | type, format | `ExportedReport` | Build table view and export with chosen adapter | `ReportTableViewFactory`, `ReportExporter`. |
| `ReportingProjectionService.java` | `recordEventProjection` | `EventEnvelope` | void | Dispatch to projection handlers, fallback record event, refresh operations snapshot if due | projection handlers, repository. |
| `ReportingProjectionConsumer.java` | listener methods | RabbitMQ message | ack/nack | Consume event queues | `ManualAckConsumerSupport`, `ReportingProjectionService`. |

### 8.6 Các flow chính của service

#### Flow: Dashboard/reporting overview

##### Mục đích
Hiển thị dashboard operations hiện tại.

##### Trigger
FE dashboard page.

##### Step-by-step
1. FE gọi `GET /api/dashboard`.
2. Gateway route sang reporting.
3. `DashboardController.dashboard` require `dashboard.view`.
4. `DashboardService.load` load latest `operations` snapshot.
5. Repository load live operations metrics/today financials/revenue trend/open alerts/active orders.
6. Identity directory resolves active order server names.
7. Service compose `DashboardView` response.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.index.tsx` | dashboard query | FE. |
| 3 | `reporting/api/DashboardController.java` | `dashboard` | API. |
| 4 | `DashboardService.java` | `load` | Dashboard service. |
| 5 | `JdbcReportSnapshotRepository.java` | `findLatestSnapshot`, `loadOperationsMetrics` | Snapshot/live metrics. |
| 5 | `JdbcOperationsDashboardQueryRepository.java` | dashboard query methods | Live dashboard reads. |
| 6 | `common/identity/RemoteSharedIdentityDirectoryClient.java` | `findDisplayNames` | Identity names. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/dashboard` | `DashboardController.java` | Public. |
| Data | operations snapshot | `JdbcReportSnapshotRepository.java` | Required; if absent throws not found. |

#### Flow: Report API

##### Mục đích
Trả các typed report cho UI.

##### Trigger
FE `_app.reports.tsx`.

##### Step-by-step
1. FE gọi các endpoint `/api/reports/operations`, `/sales`, `/peak-hours`, `/best-selling-items`, `/revenue`, `/kitchen-bottlenecks`, `/combo-sales`.
2. Gateway route sang reporting.
3. `ReportingController` require `reports.view`.
4. `ReportingQueryService` method tương ứng đọc projection rows.
5. Service build typed view + metrics.
6. Response render in reports page.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.reports.tsx` | report queries | FE. |
| 3 | `reporting/api/ReportingController.java` | report methods | API. |
| 4 | `ReportingQueryService.java` | `get*Report` | Query service. |
| 4 | `JdbcReportSnapshotRepository.java` | `findSalesRows`, `findRevenueRows`, etc. | DB reads. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `/api/reports/**` | `ReportingController.java` | Public. |
| Data | projection rows | `Jdbc*ProjectionMaterializer.java`, `JdbcReportSnapshotRepository.java` | Typed reports. |

#### Flow: Export report

##### Mục đích
Download report file.

##### Trigger
FE export action.

##### Step-by-step
1. FE direct fetch `GET /api/reports/export?type=&format=` with bearer token and correlation ID.
2. `ReportingController.export` require `reports.view`.
3. `ReportingQueryService.export` normalize type/format.
4. Query service builds report view for requested type.
5. Resolve exporter by file extension (`csv`, `json`, `pdf`).
6. Return bytes with `Content-Disposition`.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `frontend/src/routes/_app.reports.tsx` | export fetch | FE. |
| 2 | `ReportingController.java` | `export` | API. |
| 3 | `ReportingQueryService.java` | `export` | Export selection. |
| 4 | `ReportTableViewFactory.java` | `create` | Table view. |
| 5 | `CsvReportExporter.java`, `JsonReportExporter.java`, `PdfReportExporter.java` | `export` | Bytes output. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| API | `GET /api/reports/export` | `ReportingController.java` | Public file response. |

#### Flow: Đọc projection/table nào

##### Mục đích
Hiểu source data của report.

##### Trigger
Report/dashboard query.

##### Step-by-step
1. Reporting reads latest `operations` snapshot for dashboard/operations.
2. Dashboard additionally reads live low-stock alerts, audit alerts, active orders, today financials, revenue trend.
3. Typed reports read projection rows from repository methods.
4. Projection materializers maintain sales, revenue, peak hour, best selling item, kitchen bottleneck, staff efficiency, inventory usage, combo sales projection data.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 1 | `JdbcReportSnapshotRepository.java` | `findLatestSnapshot` | Snapshot read. |
| 2 | `JdbcOperationsDashboardQueryRepository.java` | dashboard read methods | Live dashboard. |
| 3 | `JdbcReportSnapshotRepository.java` | `find*Rows` | Projection reads. |
| 4 | `JdbcSalesProjectionMaterializer.java`, `JdbcRevenueProjectionMaterializer.java`, etc. | materialize methods | Projection writes. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Data | report snapshots/projections | `reporting/infrastructure/persistence/*` | Tables defined in Flyway migrations. |

#### Flow: Consume event

##### Mục đích
Cập nhật reporting projection từ events của services.

##### Trigger
RabbitMQ queues.

##### Step-by-step
1. Services publish order/kitchen/inventory/billing/reservation/menu/staff events.
2. `ReportingProjectionConsumer` lắng nghe queues như `irms.reporting.order-confirmed.q`, `irms.reporting.payment-completed.q`, `irms.reporting.inventory-stock.q`.
3. Consumer calls `ManualAckConsumerSupport.handle`.
4. `ReportingProjectionService.recordEventProjection` dispatch event to all `ReportingProjectionHandler` supporting it.
5. Nếu không handler nào support, repository `recordEventProjection`.
6. Service refresh operations snapshot if due and not already refreshed for event.

##### File/function mapping
| Step | File | Function/method | Vai trò |
|---|---|---|---|
| 2 | `ReportingProjectionConsumer.java` | listener methods | Event consumer. |
| 3 | `common/messaging/ManualAckConsumerSupport.java` | `handle` | Ack/retry. |
| 4 | `ReportingProjectionService.java` | `recordEventProjection` | Projection coordinator. |
| 4 | `SalesProjectionHandler.java`, `RevenueProjectionHandler.java`, etc. | `supports`, `project` | Typed projection. |
| 6 | `ReportingProjectionService.java` | `refreshOperationsSnapshotIfDue`, `refreshOperationsSnapshot` | Snapshot refresh. |

##### Data/API/Event liên quan
| Loại | Tên | File liên quan | Ghi chú |
|---|---|---|---|
| Event queues | `irms.reporting.*.q` | `ReportingProjectionConsumer.java` | RabbitMQ enabled. |
| Data | projection refresh log | `JdbcProjectionRefreshLogRepository.java` | Avoid duplicate refresh. |

## 9. Các luồng quan trọng đi qua nhiều service

### Cross-service Flow: FE tạo/confirm order → ordering → kitchen

#### Step-by-step
1. FE `_app.orders.tsx` gọi `POST /api/orders` hoặc `POST /api/orders/{orderId}/confirm`.
2. Gateway route `/api/orders/**` sang ordering-service.
3. `OrdersController.createOrder` hoặc `confirmDraftOrder` gọi `OrdersService`.
4. `OrderCreationService.createOrder` hoặc `OrderDraftConfirmationService.confirmDraftOrder` persist order và publish `OrderConfirmedEvent`.
5. `OrderConfirmedConsumer.consumeOrderConfirmed` nhận event.
6. `OrderFulfillmentMediator.handleOrderConfirmed` chạy `OrderKitchenRoutingStep`.
7. `RemoteKitchenOrderRoutingClient.queueConfirmedItems` gọi kitchen internal API.
8. `KitchenIntegrationController.queueConfirmedItems` gọi `KitchenOrderRoutingService.queueConfirmedItems`.
9. Kitchen tạo route plan/tickets/items theo station và publish `KitchenTicketCreatedEvent`.
10. FE refresh orders/kitchen/dashboard/billing/reports queries sau mutation.

#### Mapping
| Step | Service | File | Method/API | Chức năng |
|---|---|---|---|---|
| 1 | Frontend | `_app.orders.tsx` | `createOrderMutation` | Submit order/draft confirm. |
| 2 | Gateway | `GatewayRouteLocator.java` | `/api/orders` route | Route to ordering. |
| 3 | Ordering | `OrdersController.java` | `createOrder`, `confirmDraftOrder` | API boundary. |
| 4 | Ordering | `OrderCreationService.java`, `OrderDraftConfirmationService.java` | `createOrder`, `confirmDraftOrder` | Persist + publish `OrderConfirmedEvent`. |
| 5 | Ordering | `OrderConfirmedConsumer.java` | `consumeOrderConfirmed` | Consume event. |
| 6 | Ordering | `OrderKitchenRoutingStep.java` | `execute` | Build kitchen commands. |
| 7 | Ordering | `RemoteKitchenOrderRoutingClient.java` | `POST /internal/kitchen/orders/{orderId}/tickets` | Remote call. |
| 8 | Kitchen | `KitchenIntegrationController.java` | `queueConfirmedItems` | Receive internal call. |
| 9 | Kitchen | `KitchenOrderRoutingService.java` | `queueConfirmedItems` | Create tickets. |

#### FE/API liên quan
| FE/API | Ghi chú |
|---|---|
| `_app.orders.tsx` | Create order/draft/confirm. |
| `POST /api/orders` | Create order, `draft` flag decides event. |
| `POST /api/orders/{orderId}/confirm` | Confirm draft. |
| `POST /internal/kitchen/orders/{orderId}/tickets` | Internal ordering → kitchen. |

#### Data/Event/Remote call liên quan
| Loại | Source | Target | File | Ghi chú |
|---|---|---|---|---|
| Event | ordering | ordering consumer/reporting/audit | `OrderConfirmedEvent.java` | Drives fulfillment. |
| Remote call | ordering | kitchen | `RemoteKitchenOrderRoutingClient.java` | Create kitchen tickets. |
| Event | kitchen | reporting/audit | `KitchenTicketCreatedEvent.java` | Published after ticket created. |

### Cross-service Flow: Kitchen update món → sync về ordering

#### Step-by-step
1. FE `_app.kitchen.tsx` gọi `PATCH /api/kitchen/items/{ticketItemId}/status`.
2. Gateway route sang kitchen-service.
3. `KitchenController.updateItemStatus` gọi `KitchenService.updateItemStatus`.
4. `KitchenTicketItemWorkflowService` chuyển item sang cooking/ready/blocked.
5. Kitchen publish `KitchenDishStatusChangedEvent`.
6. `JdbcKitchenTicketStateCoordinator.reconcileTicket` tính ticket/order line statuses.
7. `RemoteOrderStateUpdateClient.applyKitchenLineStatuses` gọi ordering internal API.
8. `OrderingIntegrationController.applyKitchenLineStatuses` gọi `OrderStateCoordinator.applyKitchenLineStatuses`.
9. Ordering cập nhật line status/order status.
10. FE orders/kitchen refresh sau mutation.

#### Mapping
| Step | Service | File | Method/API | Chức năng |
|---|---|---|---|---|
| 1 | Frontend | `_app.kitchen.tsx` | item status mutation | Trigger. |
| 2 | Gateway | `GatewayRouteLocator.java` | `/api/kitchen` route | Route to kitchen. |
| 3 | Kitchen | `KitchenController.java` | `updateItemStatus` | API. |
| 4 | Kitchen | `KitchenService.java` | `updateItemStatus` | Validate/apply transition. |
| 4 | Kitchen | `KitchenTicketItemWorkflowService.java` | `moveItemToCooking`, `markItemReady`, `blockItem` | Item state change. |
| 5 | Kitchen | `KitchenTicketEventNotifier.java` | `publishDishStatus` | Publish dish event. |
| 6 | Kitchen | `JdbcKitchenTicketStateCoordinator.java` | `reconcileTicket` | Compute status projection. |
| 7 | Kitchen | `RemoteOrderStateUpdateClient.java` | `applyKitchenLineStatuses` | Remote sync. |
| 8 | Ordering | `OrderingIntegrationController.java` | `/internal/ordering/orders/{orderId}/line-statuses` | Receive sync. |
| 9 | Ordering | `OrderStateCoordinator.java` | `applyKitchenLineStatuses` | Update order state. |

#### FE/API liên quan
| FE/API | Ghi chú |
|---|---|
| `PATCH /api/kitchen/items/{ticketItemId}/status` | Public status update. |
| `POST /internal/ordering/orders/{orderId}/line-statuses` | Kitchen → ordering internal sync. |
| `POST /internal/ordering/orders/{orderId}/refresh-status` | Kitchen can refresh overall order. |

#### Data/Event/Remote call liên quan
| Loại | Source | Target | File | Ghi chú |
|---|---|---|---|---|
| Event | kitchen | reporting/notification/audit | `KitchenDishStatusChangedEvent.java` | Status event. |
| Remote call | kitchen | ordering | `RemoteOrderStateUpdateClient.java` | Sync line statuses. |
| Data | ordering | order item line status | `OrderStateCoordinator.java` | Status update. |

### Cross-service Flow: Reservation check-in → table/session

#### Step-by-step
1. FE `_app.reservations.tsx` gọi `POST /api/reservations/{reservationId}/check-in`.
2. Gateway route sang reservation-service.
3. `ReservationController.checkIn` require `tables.assign`.
4. `ReservationCheckInService.checkIn` load reservation row và validate actual party.
5. Service lấy `reservationGraceMinutes` từ identity policy, check late recovery approval nếu cần.
6. Service resolve/align table assignment.
7. `JdbcReservationCheckInCommandAdapter.openReservationSession` tạo table session.
8. Adapter mark table occupied, mark reservation seated, link assignment-session.
9. Ordering overview sau đó thấy active table session qua shared DB read.
10. Notification/event: check-in method đọc được có audit; event `ReservationSeatedEvent` tồn tại nhưng publish trực tiếp trong method này chưa tìm thấy trong source code đọc được.

#### Mapping
| Step | Service | File | Method/API | Chức năng |
|---|---|---|---|---|
| 1 | Frontend | `_app.reservations.tsx` | check-in mutation | Trigger. |
| 2 | Gateway | `GatewayRouteLocator.java` | `/api/reservations` route | Route. |
| 3 | Reservation | `ReservationController.java` | `checkIn` | API. |
| 4 | Reservation | `ReservationCheckInService.java` | `checkIn` | Check-in business. |
| 5 | Identity | `IdentityInternalController.java` | `/internal/identity/policy-snapshot` | Policy source. |
| 6 | Reservation | `ReservationTableAssignmentService.java` | `resolveCheckInTableId`, `alignReservationAssignment` | Table assignment. |
| 7 | Reservation | `JdbcReservationCheckInCommandAdapter.java` | `openReservationSession` | Create session. |
| 8 | Reservation | `JdbcReservationCheckInCommandAdapter.java` | `markTableOccupied`, `markReservationSeated`, `linkReservationAssignmentSession` | DB updates. |
| 9 | Ordering | `JdbcOrderQueryRepository.java` | `loadTableSessions` | Ordering reads active session. |

#### FE/API liên quan
| FE/API | Ghi chú |
|---|---|
| `POST /api/reservations/{reservationId}/check-in` | Main check-in endpoint. |
| `GET /api/orders/overview?sessionId=` | Can use session after check-in. |
| `GET /api/billing/overview?sessionId=` | Can create bill later from session. |

#### Data/Event/Remote call liên quan
| Loại | Source | Target | File | Ghi chú |
|---|---|---|---|---|
| Remote call | reservation | identity | `RemoteSharedIdentityPolicyClient.java` | Grace window policy. |
| Data | reservation | table_session | `JdbcReservationCheckInCommandAdapter.java` | Opens session for ordering/billing. |
| Event | reservation seated | reporting/notification | `ReservationSeatedEvent.java` | Event class exists; publish direct in check-in flow chưa tìm thấy trong source code đọc được. |

### Cross-service Flow: Billing payment → receipt/notification

#### Step-by-step
1. FE `_app.billing.tsx` gọi `POST /api/bills/{billId}/payments`.
2. Gateway route sang billing-service.
3. `BillsController.pay` calls `BillingPaymentService.processPayment`.
4. Billing calculates outstanding, authorizes gateway, records payment, updates settlement.
5. Billing publishes `PaymentCompletedEvent`.
6. FE can issue receipt via `POST /api/payments/{paymentId}/receipt`.
7. `BillingReceiptService.issueReceipt` insert/update receipt, adapter deliver, publish `ReceiptGeneratedEvent`.
8. Notification service consumes payment/receipt queues and materializes notification messages.
9. Reporting service consumes payment/receipt queues and updates projections.
10. FE can download `GET /api/payments/{paymentId}/receipt/document` to get PDF.

#### Mapping
| Step | Service | File | Method/API | Chức năng |
|---|---|---|---|---|
| 1 | Frontend | `_app.billing.tsx` | `paymentMutation` | Trigger payment. |
| 2 | Gateway | `GatewayRouteLocator.java` | billing route | Route. |
| 3 | Billing | `BillsController.java` | `pay` | API. |
| 4 | Billing | `BillingPaymentService.java` | `processPayment` | Payment business. |
| 4 | Billing | `JdbcBillPaymentRepository.java` | `recordPayment`, `updateBillSettlementStatus` | DB updates. |
| 5 | Billing | `PaymentCompletedEvent.java` | event | Payment event. |
| 6 | Frontend | `_app.billing.tsx` | `receiptMutation` | Issue receipt. |
| 7 | Billing | `BillingReceiptService.java` | `issueReceipt` | Receipt workflow. |
| 8 | Notification | `NotificationRequestedConsumer.java` | `paymentNotification`, `receiptNotification` | Notification consume. |
| 9 | Reporting | `ReportingProjectionConsumer.java` | `paymentCompleted`, `receiptGenerated` | Reporting projection. |
| 10 | Billing | `PaymentsController.java` | `receiptDocument` | PDF bytes. |

#### FE/API liên quan
| FE/API | Ghi chú |
|---|---|
| `POST /api/bills/{billId}/payments` | Process payment. |
| `POST /api/payments/{paymentId}/receipt` | Issue receipt. |
| `GET /api/payments/{paymentId}/receipt/document` | Receipt PDF. |

#### Data/Event/Remote call liên quan
| Loại | Source | Target | File | Ghi chú |
|---|---|---|---|---|
| Event | billing | notification/reporting/audit | `PaymentCompletedEvent.java` | Payment completed. |
| Event | billing | notification/reporting/audit | `ReceiptGeneratedEvent.java` | Receipt generated. |
| Notification | billing | notification | `DigitalReceiptDeliveryAdapter.java`, `BillingNotificationRequestStep.java` | Receipt/payment notification path. |
| Data | billing | payments/receipts | `JdbcBillPaymentRepository.java`, `JdbcBillingReceiptRepository.java` | Persist payment/receipt. |

### Cross-service Flow: Kitchen consumption → inventory update

#### Step-by-step
1. FE/kitchen automation moves kitchen item to `cooking`.
2. `KitchenTicketItemWorkflowService.moveItemToCooking` calls inventory before marking item cooking.
3. `RemoteInventoryConsumptionClient.consumeForKitchenStart` calls `POST /internal/inventory/consumption/kitchen-start`.
4. `InventoryIntegrationController.consumeForKitchenStart` calls `InventoryService.consumeForKitchenStart`.
5. `InventoryKitchenConsumptionService.consumeForKitchenStart` loads recipe usages for `orderItemId`.
6. For each ingredient, `InventoryStockTransactionService.recordTransaction` deducts stock idempotently.
7. Inventory publishes `InventoryStockChangedEvent`.
8. `InventoryStockChangedConsumer` may detect low stock and publish/request low-stock notification.
9. Reporting consumes inventory stock/low-stock events.

#### Mapping
| Step | Service | File | Method/API | Chức năng |
|---|---|---|---|---|
| 1 | Kitchen | `KitchenController.java` | `PATCH /api/kitchen/items/{ticketItemId}/status` | Trigger cooking. |
| 2 | Kitchen | `KitchenTicketItemWorkflowService.java` | `moveItemToCooking` | Calls inventory. |
| 3 | Kitchen | `RemoteInventoryConsumptionClient.java` | `/internal/inventory/consumption/kitchen-start` | Remote call. |
| 4 | Inventory | `InventoryIntegrationController.java` | `consumeForKitchenStart` | Internal API. |
| 5 | Inventory | `InventoryKitchenConsumptionService.java` | `consumeForKitchenStart` | Load recipe usage. |
| 6 | Inventory | `InventoryStockTransactionService.java` | `recordTransaction` | Deduct stock. |
| 7 | Inventory | `InventoryStockChangedEvent.java` | event | Stock changed. |
| 8 | Inventory | `InventoryAlertMediator.java` | `handleStockChanged` | Low-stock workflow. |
| 9 | Reporting | `ReportingProjectionConsumer.java` | `inventoryStockChanged`, `lowStock` | Projection. |

#### FE/API liên quan
| FE/API | Ghi chú |
|---|---|
| `PATCH /api/kitchen/items/{ticketItemId}/status` | With status `cooking`. |
| `POST /internal/inventory/consumption/kitchen-start` | Kitchen → inventory internal. |
| `GET /api/inventory` | FE sees updated stock/transactions. |

#### Data/Event/Remote call liên quan
| Loại | Source | Target | File | Ghi chú |
|---|---|---|---|---|
| Remote call | kitchen | inventory | `RemoteInventoryConsumptionClient.java` | Kitchen start consumption. |
| Data | inventory | stock transactions | `JdbcInventoryStockTransactionRepository.java` | Idempotent by source. |
| Event | inventory | notification/reporting/audit | `InventoryStockChangedEvent.java` | Stock changed. |
| Event | inventory | notification/reporting/audit | `LowStockDetectedEvent.java` | If below threshold. |

