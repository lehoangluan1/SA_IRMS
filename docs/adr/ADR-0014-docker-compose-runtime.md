# ADR-0014: Dùng Docker Compose cho runtime cục bộ và trình bày học phần

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Dùng Docker Compose để khởi động frontend, gateway, service Spring Boot, PostgreSQL, RabbitMQ và migration.

## Căn cứ

Compose hỗ trợ vận hành và kiểm thử tích hợp cục bộ; không đại diện cho production cluster nếu chưa có cấu hình tương ứng.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
