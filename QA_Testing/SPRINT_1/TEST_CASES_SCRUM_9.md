# BẢNG KẾT QUẢ KIỂM THỬ QA — SCRUM-9
## User Story: Quản lý Phiên làm việc (Session) & Đăng xuất an toàn
**Sprint: Sprint 1 | Epic / User Story: SCRUM-9 (Subtasks: SCRUM-100, SCRUM-101, SCRUM-102)**  
**Dự án: ERP Sales & Inventory System**

---

### 1. BẢNG TEST CASE CHI TIẾT (FORMAT 8 CỘT CHUẨN)

| Test ID | Chức năng | Mô tả | Điều kiện trước | Dữ liệu Test | Kết quả mong muốn | Status | Ghi chú |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| **TC-100-01** | Gia hạn phiên tự động (SCRUM-100) | Gọi API khi phiên sắp hết hạn thì phiên được gia hạn | Có tài khoản hợp lệ; timeout phiên T được cấu hình ngắn (VD 1–2 phút) ở môi trường test; đã login lấy token (Authorize trong Swagger) | GET /api/v1/navigation/user-context khi còn ~1/3 T trước khi hết hạn | HTTP 200; thời gian hết hạn được cập nhật mới hơn (token mới / header / cookie) | **FAIL** | Thiếu chức năng gia hạn: code dùng JWT tĩnh hạn 24 giờ (jwtExpirationMs=86400000), response không trả token/header mới. |
| **TC-100-02** | Gia hạn phiên tự động (SCRUM-100) | Gọi API liên tục lâu hơn thời hạn phiên ban đầu | Có tài khoản hợp lệ; timeout phiên T được cấu hình ngắn (VD 1–2 phút) ở môi trường test; đã login | GET /api/v1/navigation/user-context lặp lại mỗi (T-30 giây), tổng thời gian > T | Tất cả request trả 200, không bị 401 | **FAIL** | Cùng lý do TC-100-01: thiếu chức năng gia hạn. Sau khi dev xác nhận thì đổi thành Fail và tạo bug; test lại khi có gia hạn hoặc chạy app với timeout ngắn. |
| **TC-100-03** | Gia hạn phiên tự động (SCRUM-100) | Token đã hết hạn thì không được gia hạn | Có tài khoản hợp lệ; timeout phiên T được cấu hình ngắn (VD 1–2 phút) ở môi trường test; đã login, chờ quá T không gọi API | GET /api/v1/navigation/user-context bằng token đã hết hạn | HTTP 401; không cấp token/thời hạn mới | **BLOCKED** | Timeout hiện 24 giờ nên chưa chờ hết hạn được. Cách test: chạy app với --erp.app.jwtExpirationMs=120000 (không sửa src/main) rồi test lại. |
| **TC-100-04** | Gia hạn phiên tự động (SCRUM-100) | Ngừng thao tác ngắn (mạng chập chờn) thì phiên vẫn còn | Đã login, có token hợp lệ; thời gian ngừng ngắn hơn timeout T | Login, không gọi API trong 1–2 phút, sau đó GET /api/v1/navigation/user-context | HTTP 200, phiên vẫn hợp lệ, không bị văng | **PASS** | Thực tế: Ngừng thao tác 2 phút sau login, gọi user-context trả về HTTP 200 OK. Đạt yêu cầu không bị mất phiên khi gián đoạn ngắn |
| **TC-101-01** | Đăng xuất hủy phiên phía server (SCRUM-101) | Đăng xuất thành công | Đã login, có token hợp lệ | POST /api/auth/logout với token hợp lệ | HTTP 200/204, đăng xuất thành công | **PASS** | Thực tế: HTTP 200, body {"message": "Đăng xuất thành công!"}. Ảnh: [TC-101-01.png](./evidence/scrum9/TC-101-01.png) |
| **TC-101-02** | Đăng xuất hủy phiên phía server (SCRUM-101) | Token cũ bị từ chối ngay sau khi đăng xuất | Vừa đăng xuất ở TC-101-01; giữ lại token cũ | GET /api/v1/navigation/user-context với token cũ, gọi ngay sau khi logout (không chờ) | HTTP 401 Unauthorized ngay lập tức | **FAIL** | LỖI NGHIÊM TRỌNG. Thực tế: sau logout, token cũ gọi user-context vẫn HTTP 200 kèm đầy đủ menu/user context. Nguyên nhân: JWT stateless, không có blacklist nên token sống đến hết 24 giờ. Gợi ý: thu hồi token phía server (blacklist theo jti hoặc tokenVersion). Ảnh: [TC-101-02.png](./evidence/scrum9/TC-101-02.png) |
| **TC-101-03** | Đăng xuất hủy phiên phía server (SCRUM-101) | Đăng xuất lại với token đã bị thu hồi | Đã đăng xuất; giữ token cũ | POST /api/auth/logout lần 2 với token cũ | HTTP 401 (hoặc 200 idempotent theo quy định), không lỗi 500 | **FAIL** | Thực tế: Gọi logout lần 2 với cùng token vẫn trả về HTTP 200 kèm body "Đăng xuất thành công!". Lỗi do Backend chưa hề thu hồi hay đưa token vào blacklist ở bước logout trước |
| **TC-101-04** | Đăng xuất hủy phiên phía server (SCRUM-101) | Đăng nhập lại sau đăng xuất: token mới dùng được, token cũ vẫn bị từ chối | Đã đăng xuất; giữ token cũ | POST /api/auth/login lấy token mới; gọi GET /api/v1/navigation/user-context bằng token mới, rồi bằng token cũ | Token mới: 200; token cũ: 401 | **FAIL** | Thực tế: token B khác token A, B gọi user-context được 200, nhưng token A cũ vẫn 200 (đúng ra phải 401). Cùng nguyên nhân với TC-101-02 (logout không thu hồi token). Ảnh: [TC-101-04-tokenB.png](./evidence/scrum9/TC-101-04-tokenB.png), [TC-101-04-tokenA.png](./evidence/scrum9/TC-101-04-tokenA.png) |
| **TC-102-01** | Phiên hết hạn & thông báo (SCRUM-102) | Phiên hết hạn thì API trả lỗi chưa xác thực | Có tài khoản hợp lệ; timeout phiên T được cấu hình ngắn (VD 1–2 phút) ở môi trường test; đã login, chờ quá T | GET /api/v1/navigation/user-context bằng token hết hạn | HTTP 401 (FE dựa vào mã này để chuyển về trang đăng nhập) | **BLOCKED** | Timeout hiện 24 giờ nên chưa test được. Test lại khi chạy app với timeout ngắn (xem TC-100-03). |
| **TC-102-02** | Phiên hết hạn & thông báo (SCRUM-102) | Response lỗi hết hạn có thông báo rõ ràng | Token đã hết hạn | GET /api/v1/navigation/user-context bằng token hết hạn; xem response body | Body có message rõ ràng (VD "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"), không lộ stack trace | **BLOCKED** | Như TC-102-01: chưa có token hết hạn để xem message. Cần test lại với timeout ngắn và chép nguyên body lỗi. |
| **TC-102-03** | Phiên hết hạn & thông báo (SCRUM-102) | Phân biệt được hết hạn với token không hợp lệ/không có token | Có token hết hạn, token giả, và trường hợp không gửi token | GET /api/v1/navigation/user-context lần lượt với: token hết hạn, token sửa ký tự, không gửi token | Cả 3 đều 401; thông báo hết hạn chỉ xuất hiện ở trường hợp token hết hạn (hoặc mã lỗi khác nhau) | **FAIL** | Thực tế: không gửi token và token bị sửa 5 ký tự cuối đều trả HTTP 403 với body rỗng (Content-Length: 0), không phải 401 kèm message. Nguyên nhân: thiếu AuthenticationEntryPoint. Trường hợp token hết hạn chưa test (timeout 24 giờ) |
| **TC-102-04** | Phiên hết hạn & thông báo (SCRUM-102) | Đăng nhập lại sau khi hết hạn | Phiên đã hết hạn | POST /api/auth/login với tài khoản hợp lệ; sau đó GET /api/v1/navigation/user-context bằng token mới | Login 200, token mới gọi API thành công | **PASS** | Thực tế: login HTTP 200 cấp token mới; user-context bằng token mới HTTP 200 đủ quyền và menu. Ảnh: [TC-102-04.png](./evidence/scrum9/TC-102-04.png) |

---

### 2. BẢNG TỔNG HỢP KẾT QUẢ KIỂM THỬ

| Trạng thái | Số lượng Test Case | Tỷ lệ (%) |
| :--- | :---: | :---: |
| **Pass** | 3 | 25.0% |
| **Fail** | 6 | 50.0% |
| **Blocked** | 3 | 25.0% |
| **Tổng cộng** | **12** | **100%** |

---

### 3. GHI CHÚ CHUNG & ĐÁNH GIÁ KỸ THUẬT

- **Phương thức kiểm thử:** Thực hiện trực tiếp trên giao diện Swagger UI (API). Sử dụng `POST /api/auth/login`, `POST /api/auth/logout` và `GET /api/v1/navigation/user-context` (endpoint yêu cầu đăng nhập) để kiểm thử vòng đời của phiên làm việc.
- **Dữ liệu đăng nhập:** Theo schema `LoginRequest` trong Swagger (`username: "sales_manager"`, `password: "manager123"`).
- **Thời gian hết hạn phiên (T):** Mặc định trong cấu hình Spring Boot là 24 giờ (`erp.app.jwtExpirationMs=86400000`), cần cấu hình ngắn ở môi trường test (ví dụ `--erp.app.jwtExpirationMs=120000`) để kiểm thử các case hết hạn tự nhiên mà không cần sửa code trong `src/main`.
- **Tổng kết các lỗi phát hiện (6 FAIL):**
  1. **TC-100-01 & TC-100-02 (Gia hạn phiên tự động):** Backend dùng JWT tĩnh, không trả token mới hoặc sliding expiration header khi người dùng gửi request, dẫn đến thiếu tính năng gia hạn phiên liên tục.
  2. **TC-101-02 & TC-101-04 (Vô hiệu hóa phiên sau logout):** Sau khi gọi API logout, token cũ vẫn dùng được bình thường để truy cập các API nghiệp vụ; khi login phiên mới thì phiên cũ vẫn không bị thu hồi. Backend dùng JWT Stateless thuần túy chưa có cơ chế Blacklist token phía server.
  3. **TC-101-03 (Logout lần 2):** Gọi logout lần 2 với cùng một token vẫn trả về HTTP 200 "Đăng xuất thành công!" do server không kiểm tra và không thu hồi token ở lần logout đầu tiên.
  4. **TC-102-03 (Phân biệt lỗi xác thực):** Khi không gửi token hoặc token bị sửa đổi, hệ thống trả về HTTP 403 Forbidden body rỗng thay vì HTTP 401 Unauthorized kèm thông báo lỗi rõ ràng (nguyên nhân do thiếu `AuthenticationEntryPoint` trong SecurityFilterChain).

---

### 4. 📸 HÌNH ẢNH BẰNG CHỨNG KIỂM THỬ TRÊN SWAGGER UI

#### TC-101-01: Đăng xuất thành công (HTTP 200 OK)
![TC-101-01](./evidence/scrum9/TC-101-01.png)

---

#### TC-101-02: LỖI NGHIÊM TRỌNG — Token cũ vẫn dùng được sau khi đăng xuất
![TC-101-02](./evidence/scrum9/TC-101-02.png)

---

#### TC-101-03: Đăng xuất lại với cùng token vẫn trả về HTTP 200
![TC-101-03](./evidence/scrum9/TC-101-03.png)

---

#### TC-101-04: Đăng nhập lại sau đăng xuất — Token B dùng được, Token A cũ vẫn dùng được
![TC-101-04-tokenB](./evidence/scrum9/TC-101-04-tokenB.png)

![TC-101-04-tokenA](./evidence/scrum9/TC-101-04-tokenA.png)

---

#### TC-102-03: Không gửi token & Token sửa ký tự (HTTP 403 Forbidden thay vì 401)
![TC-102-03-noauth](./evidence/scrum9/TC-102-03-noauth.png)

![TC-102-03-tampered](./evidence/scrum9/TC-102-03-tampered.png)

---

#### TC-102-04: Đăng nhập lại sau khi hết hạn / lỗi phiên
![TC-102-04](./evidence/scrum9/TC-102-04.png)
