# ADR-0004: Dùng REST đồng bộ cho thao tác cần phản hồi trực tiếp

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Các thao tác check-in, tạo order, cập nhật bếp, lập hóa đơn, thanh toán và hoàn tiền dùng REST/API đồng bộ.

## Căn cứ

Quyết định này đáp ứng yêu cầu hiệu năng và độ phản hồi cho UI phục vụ, bếp và thu ngân; event không thay thế command cần phản hồi tức thời.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
