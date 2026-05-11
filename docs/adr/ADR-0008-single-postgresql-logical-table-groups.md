# ADR-0008: Dùng một PostgreSQL vật lý duy nhất, phân tách theo nhóm bảng logic

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Runtime hiện tại chỉ có một PostgreSQL vật lý; outbox_events, processed_events, read model, audit và identity/session/policy là nhóm bảng logic.

## Căn cứ

Quyết định này giữ vận hành đơn giản và nhất quán dữ liệu trong phạm vi học phần, nhưng yêu cầu giữ ranh giới logic bằng module, repository và naming.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
