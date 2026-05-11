# ADR-0001: Chọn modular architecture cho phân rã ban đầu

## Trạng thái

Thay thế bởi ADR-0002.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Dùng modular architecture/module-based decomposition làm định hướng phân rã ban đầu để tổ chức các vùng đặt bàn, gọi món, bếp, thanh toán, tồn kho, báo cáo, định danh và kiểm toán.

## Căn cứ

Quyết định này phục vụ bảo trì và mở rộng chức năng trong giai đoạn đầu, nhưng chưa đủ để mô tả runtime độc lập, gateway, event/outbox và mở rộng từng vùng chức năng.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
