# IRMS Software Architecture Report

Repo này chứa báo cáo kiến trúc phần mềm cho đề tài `Intelligent Restaurant Management System (IRMS)` của môn `Kiến trúc Phần mềm (CO3017)`.

README này hướng dẫn từ đầu đến cuối để một thành viên mới có thể:
- cài đúng công cụ cần thiết
- mở đúng file chính
- biên dịch PDF bằng TeXworks hoặc dòng lệnh
- sửa sơ đồ `.dot` và render lại ảnh
- hiểu rõ cấu trúc thư mục của dự án

## 1. File chạy chính của dự án

File nên mở và biên dịch chính là:

- `irms_architecture_report.tex`

Đây là entry point chính của dự án.

Repo vẫn giữ thêm:

- `main.tex`

`main.tex` là file lõi chứa preamble, cấu hình chung và danh sách `\input{...}`.  
Khi làm việc nhóm, nên thống nhất mở và compile `irms_architecture_report.tex` để tránh nhầm lẫn.

## 2. Cần cài những gì

### 2.1. Bắt buộc để biên dịch báo cáo PDF

Bạn cần một bản phân phối LaTeX có `XeLaTeX`.

Khuyến nghị trên Windows:

1. `MiKTeX`
2. `TeXworks`

Lý do phải dùng `XeLaTeX`:

- dự án dùng `fontspec`
- dự án dùng `polyglossia`
- báo cáo có tiếng Việt nên `pdfLaTeX` không phù hợp

Nếu cài MiKTeX bản đầy đủ, TeXworks thường đi kèm sẵn.

### 2.2. Cần thêm nếu muốn sửa sơ đồ `.dot`

Bạn cần cài:

1. `Graphviz`

Mục đích:

- dùng lệnh `dot` để render các file `.dot` thành `.png`
- có thể render thêm `.pdf` khi cần đối chiếu hoặc xuất sơ đồ riêng

Lưu ý:

- script `scripts/render-diagrams.ps1` hiện đã tự dò Graphviz trong các thư mục mặc định của Windows
- tuy vậy, tốt nhất vẫn nên thêm Graphviz vào `PATH` để dùng thuận tiện hơn

## 3. Kiểm tra sau khi cài

Mở PowerShell và chạy:

```powershell
xelatex --version
dot -V
```

Nếu:

- `xelatex` chạy được thì phần LaTeX đã ổn
- `dot` chạy được thì phần render sơ đồ đã ổn

Nếu `dot` chưa có trong `PATH` nhưng bạn đã cài Graphviz ở thư mục mặc định, script render của repo vẫn có thể chạy.

## 4. Cấu trúc thư mục của dự án

### 4.1. Các file ở thư mục gốc

- `irms_architecture_report.tex`
  - entry point chính để compile báo cáo
- `main.tex`
  - file lõi chứa phần khai báo package, macro, header/footer, bìa và `\input` các section
- `README.md`
  - tài liệu hướng dẫn dự án
- `.gitignore`
  - bỏ qua file build trung gian của LaTeX

### 4.2. Thư mục `sections/`

Chứa nội dung báo cáo đã tách theo từng phần lớn:

- `01_system_overview.tex`
  - tổng quan hệ thống, bối cảnh, mục tiêu, phạm vi, yêu cầu chức năng và phi chức năng
- `02_system_modeling.tex`
  - mô hình hóa hệ thống, use case, sequence, activity và đặc tả ca sử dụng
- `03_software_architecture.tex`
  - các góc nhìn kiến trúc, module view, component-and-connector view, allocation view, nguyên tắc thiết kế, SOLID, future extensibility
- `04_detailed_design.tex`
  - UML class diagram, phân tích cụm lớp, activity diagram và phần phản tư
- `05_appendix.tex`
  - phụ lục, ADR và phân công công việc

### 4.3. Thư mục `assets/diagrams/source/`

Chứa toàn bộ sơ đồ nguồn dạng Graphviz `.dot`, chia theo nhóm:

- `assets/diagrams/source/modeling/`
  - context diagram, use case overview, sequence diagram, activity overview
- `assets/diagrams/source/architecture/`
  - logical view, module view, component view, deployment view, future extensibility
- `assets/diagrams/source/design/`
  - class diagram và các sơ đồ thiết kế chi tiết
- `assets/diagrams/source/activity/`
  - activity diagram cho từng use case

Khi muốn sửa cấu trúc hoặc nội dung sơ đồ, bạn sửa file `.dot` ở đây.

### 4.4. Thư mục `assets/diagrams/rendered/`

Chứa các ảnh `.png` đã render từ file `.dot`.

Cấu trúc song song với thư mục `source/`:

- `assets/diagrams/rendered/modeling/`
- `assets/diagrams/rendered/architecture/`
- `assets/diagrams/rendered/design/`
- `assets/diagrams/rendered/activity/`

Các ảnh này được LaTeX dùng trực tiếp khi biên dịch báo cáo.

### 4.5. Thư mục `assets/images/`

Chứa ảnh không phải Graphviz:

- `assets/images/branding/`
  - logo trường, logo thương hiệu, ảnh nhận diện
  - bìa và header hiện đang tìm logo tại:
    - `assets/images/branding/bachkhoa_logo.png`
- `assets/images/misc/`
  - ảnh minh họa phụ khác của repo

### 4.6. Thư mục `references/`

Chứa tài liệu tham chiếu đầu vào, ví dụ:

- `SA_hk252_Assignment.pdf`

### 4.7. Thư mục `scripts/`

Chứa script hỗ trợ:

- `render-diagrams.ps1`
  - render toàn bộ sơ đồ `.dot` thành `.png`
  - có thể render `.pdf` với tham số `-Format pdf`

### 4.8. Thư mục `legacy/`

Chứa các bản cũ để đối chiếu:

- file `.tex` kiểu monolithic
- file `.pdf` cũ

Không nên tiếp tục sửa trực tiếp trong thư mục này.

## 5. Cách chạy bằng TeXworks

Đây là cách khuyến nghị cho người dùng Windows.

### Bước 1. Mở đúng file

Trong TeXworks, mở:

- `irms_architecture_report.tex`

Không mở trực tiếp các file trong `sections/` rồi bấm compile, vì đó chỉ là file con.

### Bước 2. Chọn engine đúng

Ở góc trên của TeXworks, chọn:

- `XeLaTeX`

Không chọn `pdfLaTeX`.

### Bước 3. Biên dịch

Bấm nút chạy màu xanh hoặc dùng:

- `Ctrl + T`

### Bước 4. Nếu mục lục hoặc tham chiếu chưa cập nhật

Biên dịch thêm một lần nữa.

Thông thường:

1. Lần 1: tạo file phụ trợ
2. Lần 2: cập nhật mục lục, số hình, tham chiếu chéo

## 6. Cách chạy bằng dòng lệnh

Trong PowerShell:

```powershell
xelatex irms_architecture_report.tex
xelatex irms_architecture_report.tex
```

PDF đầu ra là:

- `irms_architecture_report.pdf`

## 7. Quy trình sửa nội dung `.tex`

Nếu bạn chỉ sửa chữ trong báo cáo:

1. sửa file trong `sections/` hoặc `main.tex`
2. compile lại bằng `XeLaTeX`
3. compile thêm lần thứ hai nếu cần cập nhật tham chiếu chéo

Trong trường hợp này:

- không cần Graphviz
- không cần render lại sơ đồ

## 8. Quy trình sửa sơ đồ `.dot`

### Bước 1. Sửa file nguồn

Sửa đúng file `.dot` trong:

- `assets/diagrams/source/modeling/`
- `assets/diagrams/source/architecture/`
- `assets/diagrams/source/design/`
- `assets/diagrams/source/activity/`

### Bước 2. Render lại ảnh

Chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\render-diagrams.ps1
```

Nếu cần render PDF của sơ đồ:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\render-diagrams.ps1 -Format pdf
```

Script sẽ:

- quét toàn bộ file `.dot`
- render lại các file `.png`
- hoặc render các file `.pdf` nếu dùng `-Format pdf`
- ghi vào đúng thư mục `assets/diagrams/rendered/` tương ứng

### Bước 3. Compile lại báo cáo

```powershell
xelatex irms_architecture_report.tex
xelatex irms_architecture_report.tex
```

## 9. Quy trình ngắn gọn cho người mới clone repo

Nếu bạn chỉ muốn clone repo về rồi build báo cáo:

1. cài `MiKTeX`
2. cài hoặc mở `TeXworks`
3. mở file `irms_architecture_report.tex`
4. chọn engine `XeLaTeX`
5. compile 2 lần

Nếu bạn còn muốn sửa sơ đồ:

6. cài `Graphviz`
7. sửa file `.dot`
8. chạy `scripts/render-diagrams.ps1`
9. compile lại 2 lần

## 10. Một số lỗi thường gặp

### Lỗi 1. Compile không chạy trong TeXworks

Nguyên nhân thường gặp:

- đang chọn sai engine
- đang dùng `pdfLaTeX` thay vì `XeLaTeX`

Cách xử lý:

- đổi engine sang `XeLaTeX`

### Lỗi 2. Mục lục hoặc số hình chưa cập nhật

Nguyên nhân:

- mới compile 1 lần

Cách xử lý:

- compile lại lần 2

### Lỗi 3. Sơ đồ `.dot` đã sửa nhưng ảnh không đổi

Nguyên nhân:

- chưa render lại `.png`

Cách xử lý:

- chạy `powershell -ExecutionPolicy Bypass -File .\scripts\render-diagrams.ps1`
- sau đó compile lại báo cáo

### Lỗi 4. Script render báo không tìm thấy `dot`

Cách xử lý:

1. kiểm tra Graphviz đã được cài chưa
2. nếu đã cài, thử thêm Graphviz vào `PATH`
3. nếu cài Graphviz ở thư mục mặc định của Windows, script hiện tại vẫn có thể tự dò:
   - `C:\Program Files\Graphviz\bin\dot.exe`
   - `C:\Program Files (x86)\Graphviz\bin\dot.exe`

## 11. Quy ước làm việc nhóm nên thống nhất

1. Chỉ compile từ `irms_architecture_report.tex`
2. Chỉ sửa nội dung trong `sections/`, `main.tex`, `assets/diagrams/source/`
3. Không sửa trực tiếp file render trong `assets/diagrams/rendered/`
4. Không sửa trực tiếp file cũ trong `legacy/`
5. Sau khi sửa `.dot`, phải render lại `.png`
6. Nếu thay logo trường, đặt đúng file:

```text
assets/images/branding/bachkhoa_logo.png
```

## 12. Trạng thái hiện tại của dự án

Hiện tại repo đã có:

- cấu trúc LaTeX tách section rõ ràng
- sơ đồ nguồn `.dot` tách theo nhóm
- ảnh render `.png` tách theo nhóm
- bìa, header, appendix và phân công công việc
- file compile chính thống nhất cho toàn nhóm

Khi triển khai phần mã nguồn, nên giữ nguyên ranh giới mô-đun đã khóa trong báo cáo để phần code và phần tài liệu không bị lệch nhau.
