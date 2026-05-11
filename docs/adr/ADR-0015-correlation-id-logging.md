# ADR-0015: Dùng correlation id/logging cho truy vết vận hành

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Request/event quan trọng mang correlation id qua gateway, service, consumer và audit/log.

## Căn cứ

Quyết định này hỗ trợ quan sát vận hành và kiểm toán, giúp phân tích lỗi và đối chiếu thao tác sau ca vận hành.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
