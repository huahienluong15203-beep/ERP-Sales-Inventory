# BẢNG KẾT QUẢ KIỂM THỬ QA — SCRUM-7
## User Story: Phân quyền Menu Điều hướng & Thông tin Người dùng
**Sprint: Sprint 1 | Epic / User Story: SCRUM-7**
**Dự án: ERP Sales & Inventory System**

---

### 1. THÔNG TIN CHUNG (OVERVIEW)

* **User Story:** *“Là người dùng của hệ thống, tôi muốn thấy menu điều hướng đúng theo quyền của mình, để không bị rối bởi những chức năng mình không được dùng.”*
* **Tiêu chí nghiệm thu (Acceptance Criteria):**
  1. Mục menu không thuộc quyền thì không hiển thị.
  2. Hiển thị tên, vai trò và kho hoặc địa bàn đang làm việc.
  3. Dùng được thuận tiện trên màn hình 360px.
* **Phân tích kỹ thuật từ mã nguồn (`UserProfileController.java` & `SecurityConfig.java`):**
  * **Endpoint:** `GET /api/v1/navigation/user-context`
  * **Tham số:** `role` (Query param, mặc định: `"ROLE_STAFF"`)
  * **Xác thực:** Bắt buộc có JWT Bearer Token qua header `Authorization` (nếu không có sẽ bị chặn HTTP 403 Forbidden).
  * **Dữ liệu trả về:**
    * `user`: `fullName`, `role`, `warehouse`, `workLocation`.
    * `menus`: Danh sách các mục menu gồm `title`, `path`, `icon` được phân quyền linh hoạt theo vai trò (`ROLE_ADMIN`, `ROLE_WAREHOUSE_MANAGER`, `ROLE_STAFF`).

---

### 2. BẢNG TEST CASE CHI TIẾT (FORMAT 8 CỘT CHUẨN)

| Test ID | Chức năng | Mô tả | Điều kiện trước | Dữ liệu Test | Kết quả mong muốn | Status | Ghi chú |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| **TC_NAV_001** | Điều hướng / Phân quyền Menu | Kiểm tra hiển thị danh sách menu phân quyền cho Quản trị viên (ROLE_ADMIN) | Server đang chạy, đã đăng nhập tài khoản admin có Bearer token | GET `/api/v1/navigation/user-context?role=ROLE_ADMIN`, Header: `Bearer <token>` | HTTP 200 OK. Trả về đúng 6 menu quản trị (Trang chủ, Hồ sơ cá nhân, Quản lý tài khoản, Phân quyền hệ thống, Quản lý kho hàng, Báo cáo doanh thu). Không hiển thị menu của vai trò khác. | **PASS** | Thực tế: 200 OK. `menus` có độ dài 6 mục, đúng các path: `/dashboard`, `/profile`, `/admin/users`, `/admin/roles`, `/warehouse/manage`, `/reports/sales`. |
| **TC_NAV_002** | Điều hướng / Phân quyền Menu | Kiểm tra hiển thị danh sách menu phân quyền cho Quản lý kho (ROLE_WAREHOUSE_MANAGER) | Server đang chạy, có Bearer token hợp lệ | GET `/api/v1/navigation/user-context?role=ROLE_WAREHOUSE_MANAGER`, Header: `Bearer <token>` | HTTP 200 OK. Trả về đúng 5 menu kho (Trang chủ, Hồ sơ cá nhân, Quản lý kho hàng, Nhập/Xuất kho, Kiểm kê hàng tồn). Không hiển thị menu quản trị Admin và menu Bán hàng. | **PASS** | Thực tế: 200 OK. `menus` có 5 mục: `/dashboard`, `/profile`, `/warehouse/manage`, `/warehouse/stock`, `/warehouse/inventory`. Các mục ngoài quyền không hiển thị. |
| **TC_NAV_003** | Điều hướng / Phân quyền Menu | Kiểm tra hiển thị danh sách menu cho Nhân viên thông thường (ROLE_STAFF hoặc mặc định không truyền param) | Server đang chạy, có Bearer token hợp lệ | GET `/api/v1/navigation/user-context` (không truyền param role), Header: `Bearer <token>` | HTTP 200 OK. Hệ thống tự gán role mặc định là ROLE_STAFF, chỉ trả về 4 menu cơ bản (Trang chủ, Hồ sơ cá nhân, Tra cứu tồn kho, Tạo đơn xuất hàng). Không hiển thị menu Admin hay Quản lý kho. | **PASS** | Thực tế: 200 OK. `menus` có 4 mục: `/dashboard`, `/profile`, `/warehouse/lookup`, `/sales/orders/create`. `user.role` = "ROLE_STAFF". |
| **TC_NAV_004** | Điều hướng / Thông tin người dùng | Kiểm tra hiển thị đầy đủ thông tin tên, vai trò, kho và địa bàn làm việc của người dùng | Server đang chạy, có Bearer token hợp lệ | GET `/api/v1/navigation/user-context?role=ROLE_ADMIN`, Header: `Bearer <token>` | HTTP 200 OK. Đối tượng `user` trong response phải có đầy đủ 4 trường: `fullName`, `role`, `warehouse`, `workLocation` với dữ liệu hợp lệ không bị rỗng/null. | **PASS** | Thực tế: 200 OK. `fullName`: "Nguyễn Văn Minh", `role`: "ROLE_ADMIN", `warehouse`: "Kho Tổng Miền Bắc", `workLocation`: "Khu công nghiệp Tiên Sơn, Bắc Ninh". Đáp ứng 100% AC-2. |
| **TC_NAV_005** | Điều hướng / Xử lý tham số | Kiểm tra tính không phân biệt chữ hoa/chữ thường của tham số vai trò (role) | Server đang chạy, có Bearer token hợp lệ | GET `/api/v1/navigation/user-context?role=role_admin`, Header: `Bearer <token>` | HTTP 200 OK. Hệ thống so khớp không phân biệt hoa thường (`equalsIgnoreCase`), vẫn nhận diện đúng vai trò Admin và trả về đầy đủ 6 menu quản trị. | **PASS** | Thực tế: 200 OK. Trả về đúng 6 menu Admin như khi truyền chữ hoa `ROLE_ADMIN`. |
| **TC_NAV_006** | Điều hướng / Xử lý ngoại lệ | Kiểm tra xử lý an toàn khi truyền vai trò không xác định hoặc không hợp lệ (Negative) | Server đang chạy, có Bearer token hợp lệ | GET `/api/v1/navigation/user-context?role=ROLE_UNKNOWN_GUEST`, Header: `Bearer <token>` | HTTP 200 OK. Hệ thống fallback an toàn về danh sách menu nhân viên cơ bản, tuyệt đối không lộ bất kỳ menu quản trị cấp cao nào. | **PASS** | Thực tế: 200 OK. `menus` trả về 4 mục cơ bản (Trang chủ, Profile, Tra cứu tồn kho, Tạo đơn), không bị lộ menu của Admin hay Quản lý kho. |
| **TC_NAV_007** | Điều hướng / Bảo mật API | Chặn truy cập API context điều hướng khi không có Authorization Token hoặc Token không hợp lệ (Negative) | Server đang chạy | GET `/api/v1/navigation/user-context` không có Authorization header | HTTP 403 Forbidden hoặc 401 Unauthorized. Không trả về thông tin người dùng hay danh sách menu. | **PASS** | Thực tế: 403 Forbidden. Spring Security chặn truy cập khi chưa xác thực, bảo vệ API an toàn. |
| **TC_NAV_008** | Điều hướng / Giao diện Responsive (UI/UX) | Kiểm tra hiển thị và thao tác menu điều hướng thuận tiện trên màn hình kích thước 360px | Đã khởi chạy Frontend hoặc mở trình duyệt ở chế độ Responsive Device (Viewport width: 360px) | Viewport: 360 x 640px (Mobile portrait), tài khoản đăng nhập có quyền tương ứng | Menu điều hướng tự động thu gọn vào Hamburger / Drawer; thông tin tên, vai trò, kho hiển thị rõ ràng, không bị vỡ layout hay tràn viền ngang (overflow-x); thao tác đóng/mở menu mượt mà. | **NOT RUN** | **Lưu ý: Đây là kiểm thử Frontend/Responsive, không phải Backend test.** Cần thực hiện kiểm thử trên giao diện web/mobile ở phân hệ Frontend. |

---

### 3. THỐNG KÊ KẾT QUẢ KIỂM THỬ:
* **Tổng số Test Case:** **8** (7 Backend API test cases + 1 Frontend Responsive test case theo AC 3)
* **Backend Test Cases:**
  * **PASS:** **7 / 7** (100% test cases Backend đã chạy thực tế và đạt kết quả mong muốn)
  * **FAIL:** **0**
  * **BLOCKED:** **0**
* **Frontend Test Case (TC_NAV_008):**
  * **NOT RUN:** **1** (Dành riêng cho kiểm thử giao diện màn hình 360px trên phân hệ Frontend).
