# ADR-0010: Dùng idempotency/inbox tracking cho consumer nền

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Consumer nền kiểm tra processed_events hoặc cơ chế tương đương để tránh xử lý lặp.

## Căn cứ

Quyết định này gắn với độ tin cậy vì event có thể retry hoặc trùng, còn tác dụng phụ lặp phải được kiểm soát.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
