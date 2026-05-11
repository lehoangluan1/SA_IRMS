# ADR-0006: Dùng outbox pattern để phát event sau giao dịch nghiệp vụ

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Event bất đồng bộ được ghi vào outbox_events trong cùng transaction với thay đổi nghiệp vụ, sau đó relay sang RabbitMQ.

## Căn cứ

Outbox gắn với độ tin cậy và toàn vẹn giao dịch vì giảm nguy cơ mất event khi giao dịch chính thành công nhưng publish thất bại.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
