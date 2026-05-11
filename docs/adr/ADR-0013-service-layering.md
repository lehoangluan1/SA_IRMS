# ADR-0013: Dùng domain/application layering trong từng service

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Từng service tổ chức theo các lớp api, application, domain, infrastructure và integration khi phù hợp.

## Căn cứ

Layering hỗ trợ bảo trì bằng cách tách controller, use case, domain policy và adapter hạ tầng.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
