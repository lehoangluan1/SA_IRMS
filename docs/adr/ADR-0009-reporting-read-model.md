# ADR-0009: Dùng read model logic cho báo cáo

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Reporting dùng projection/read model tables trong cùng PostgreSQL để phục vụ dashboard và báo cáo.

## Căn cứ

Read model hỗ trợ hiệu năng và khả năng mở rộng vì truy vấn báo cáo không làm nặng dữ liệu giao dịch chính, đổi lại có độ trễ có kiểm soát.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
