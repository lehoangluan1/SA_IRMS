# Architecture Decision Records

Thư mục này lưu hồ sơ quyết định kiến trúc của IRMS. Nội dung phân tích chính nằm trong report; mỗi ADR ghi lại một quyết định quan trọng, trạng thái, bối cảnh, căn cứ gắn với yêu cầu phi chức năng và hệ quả để tránh mất bối cảnh khi tài liệu hoặc hệ thống tiếp tục thay đổi.

## Danh sách ADR

- ADR-0001: Chọn modular architecture cho phân rã ban đầu. Trạng thái: Thay thế bởi ADR-0002.
- ADR-0002: Chuyển sang service-based architecture làm kiến trúc chủ đạo. Trạng thái: Được chấp nhận; thay thế ADR-0001.
- ADR-0003: Dùng gateway boundary cho frontend và REST entry point.
- ADR-0004: Dùng REST đồng bộ cho thao tác cần phản hồi trực tiếp.
- ADR-0005: Dùng event-driven processing cho hệ quả bất đồng bộ.
- ADR-0006: Dùng outbox pattern để phát event sau giao dịch nghiệp vụ.
- ADR-0007: Dùng RabbitMQ làm broker cho luồng event nội bộ.
- ADR-0008: Dùng một PostgreSQL vật lý duy nhất, phân tách theo nhóm bảng logic.
- ADR-0009: Dùng read model logic cho báo cáo.
- ADR-0010: Dùng idempotency/inbox tracking cho consumer nền.
- ADR-0011: Dùng boundary adapters cho thanh toán, thông báo và biên nhận.
- ADR-0012: Dùng IAM/RBAC và audit log cho thao tác nhạy cảm.
- ADR-0013: Dùng domain/application layering trong từng service.
- ADR-0014: Dùng Docker Compose cho runtime cục bộ và trình bày học phần.
- ADR-0015: Dùng correlation id/logging cho truy vết vận hành.