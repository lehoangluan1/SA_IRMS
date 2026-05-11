# ADR-0003: Dùng gateway boundary cho frontend và REST entry point

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Frontend đi qua Frontend Node Server và API Gateway thay vì gọi trực tiếp từng service.

## Căn cứ

Gateway hỗ trợ bảo trì, bảo mật và quan sát vận hành bằng cách chuẩn hóa entry point, route, timeout và lỗi ở biên hệ thống.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
