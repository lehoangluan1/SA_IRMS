# ADR-0002: Chuyển sang service-based architecture làm kiến trúc chủ đạo

## Trạng thái

Được chấp nhận; thay thế ADR-0001.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Chọn service-based architecture làm kiến trúc chủ đạo; modular decomposition vẫn được giữ trong Module View; event-driven processing chỉ là cơ chế bổ trợ.

## Căn cứ

Quyết định này gắn với yêu cầu mở rộng, độ phản hồi và bảo trì: các service giữ ranh giới trách nhiệm rõ, còn luồng nền giảm tải cho tuyến giao dịch nóng.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
