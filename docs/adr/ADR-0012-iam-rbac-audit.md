# ADR-0012: Dùng IAM/RBAC và audit log cho thao tác nhạy cảm

## Trạng thái

Được chấp nhận.

## Bối cảnh

IRMS cần quyết định này để giữ nhất quán giữa yêu cầu nghiệp vụ, yêu cầu phi chức năng và các góc nhìn kiến trúc trong report.

## Quyết định

Các thao tác hoàn tiền, hủy món, ghi đè giá, thay đổi quyền và điều chỉnh tồn kho cần kiểm quyền và ghi audit.

## Căn cứ

Quyết định này đáp ứng yêu cầu bảo mật và kiểm toán về truy vết người thực hiện, thời điểm, giá trị và lý do thao tác.

## Hệ quả

Quyết định này cần được phản ánh nhất quán trong kiến trúc tổng quan, Module View, Component-and-Connector View, Deployment View hoặc thiết kế chi tiết tùy phạm vi liên quan. Các giới hạn phạm vi hiện tại không được diễn giải thành năng lực production chưa được mô tả.
