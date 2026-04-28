# ADR-0001: Service-based and event-driven architecture

Date: 2026-04-28

## Status
Accepted

## Context
IRMS hiện có React/TanStack frontend và Spring Boot backend chạy theo nhiều runtime: gateway, ordering, kitchen, billing, reservation, inventory, notification, reporting và identity/audit. Các luồng gọi món, thanh toán và xếp bàn cần phản hồi đồng bộ; các hệ quả như báo cáo, thông báo, audit và tồn kho có thể xử lý nền.

Rationale inferred from current implementation and constraints.

## Decision
Sử dụng service-based architecture theo miền nghiệp vụ, kết hợp xử lý đồng bộ cho tuyến người dùng và event-driven processing cho hệ quả nền.

## Consequences
Positive:
- Ranh giới nghiệp vụ rõ hơn giữa ordering, kitchen, billing, reservation, inventory, notification, reporting và identity/audit.
- Các hệ quả nền không cần chen trực tiếp vào tuyến giao dịch nóng.

Negative:
- Cần giữ kỷ luật hợp đồng API/event để tránh phụ thuộc chéo.
- Tăng nhu cầu quan sát và xử lý lỗi bất đồng bộ.

Neutral / trade-offs:
- Đây không phải microservices đầy đủ với database-per-service riêng biệt; tài liệu phải mô tả đúng mức hiện trạng.

## Alternatives considered
- Layered monolith: đơn giản hơn nhưng dễ làm mờ ranh giới nghiệp vụ.
- Full microservices: tách mạnh hơn nhưng tăng chi phí vận hành và giao dịch phân tán.
- Fully event-driven system: không phù hợp với thao tác cần phản hồi ngay như thanh toán.

## Evidence from code/config
- `irms_project/backend/src/main/java/SA/irms/IrmsApplication.java`
- `irms_project/docker-compose.yml`
- `irms_project/backend/src/main/resources/service-topology.yml`
- `irms_project/frontend/src/lib/api/endpoints.ts`

## Related documentation and diagrams
- `sections/03_software_architecture.tex`
- `sections/05_appendix.tex`
- `assets/diagrams/source/architecture/33_architecture_overview_react_nitro_spring_postgresql.dot`
- `assets/diagrams/source/architecture/09_component_and_connector_view_overall_runtime_structure.dot`
