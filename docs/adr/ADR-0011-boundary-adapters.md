# ADR-0011: Dùng boundary adapters cho thanh toán, thông báo và biên nhận

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Các kênh thanh toán, thông báo và biên nhận đi qua PaymentGateway, NotificationChannelAdapter và ReceiptDeliveryAdapter.

## Căn cứ

Boundary adapters hỗ trợ bảo trì và mở rộng chức năng; tài liệu không trình bày như đã tích hợp provider ngoài nếu chỉ có adapter nội bộ.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
