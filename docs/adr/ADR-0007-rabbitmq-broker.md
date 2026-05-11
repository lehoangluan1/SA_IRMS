# ADR-0007: Dùng RabbitMQ làm broker cho luồng event nội bộ

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Dùng RabbitMQ để chuyển event giữa outbox relay và consumer nền nội bộ.

## Căn cứ

Broker hỗ trợ mở rộng, retry và vận hành độc lập giữa producer và consumer.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
