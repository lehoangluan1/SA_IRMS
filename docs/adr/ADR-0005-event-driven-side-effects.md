# ADR-0005: Dùng event-driven processing cho hệ quả bất đồng bộ

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Thông báo, báo cáo, kiểm toán, cập nhật read model và cảnh báo tồn kho được xử lý qua event sau giao dịch chính.

## Căn cứ

Quyết định này hỗ trợ độ phản hồi, khả năng mở rộng và độ tin cậy bằng cách tách hệ quả nền khỏi tuyến giao dịch nóng.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
