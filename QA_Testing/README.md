# KHO LƯU TRỮ TÀI LIỆU KIỂM THỬ CHẤT LƯỢNG (QA & TESTING)
## Dự án: ERP Sales & Inventory System — Backend Testing

Thư mục này chứa tài liệu và kết quả kiểm thử thực tế phân hệ Backend cho các User Story thuộc Sprint 1.

---

## 📑 Danh mục Tài liệu Kiểm thử Sprint 1

### 1. SCRUM-6: Đăng nhập, Khóa tài khoản & Phân quyền Giá vốn
* **User Story:** *Là người dùng của hệ thống, tôi muốn đăng nhập bằng tài khoản và mật khẩu, để truy cập được phần việc của mình mà dữ liệu giá vốn không lọt ra ngoài.*
* **Scope chuẩn:** 19 Test Cases (Từ `TC_AUTH_001` đến `TC_RBAC_019`).
* **Tài liệu:**
  * [**`QA_Testing/TEST_CASES_SCRUM_6.md`**](./TEST_CASES_SCRUM_6.md) (Bảng kết quả chuẩn 8 cột)
  * [**`QA_Testing/TEST_CASES_SCRUM_6.csv`**](./TEST_CASES_SCRUM_6.csv) (File CSV import Jira/Excel)

---

### 2. SCRUM-7: Phân quyền Menu Điều hướng & Thông tin Người dùng
* **User Story:** *Là người dùng của hệ thống, tôi muốn thấy menu điều hướng đúng theo quyền của mình, để không bị rối bởi những chức năng mình không được dùng.*
* **Acceptance Criteria:**
  1. Mục menu không thuộc quyền thì không hiển thị.
  2. Hiển thị tên, vai trò và kho hoặc địa bàn đang làm việc.
  3. Dùng được thuận tiện trên màn hình 360px.
* **Scope chuẩn:** 8 Test Cases (7 Backend API test cases + 1 Frontend Responsive test case).
* **Tài liệu:**
  * [**`QA/Sprint-1/SCRUM-7/TEST_CASES_SCRUM_7.md`**](../QA/Sprint-1/SCRUM-7/TEST_CASES_SCRUM_7.md)
  * [**`QA/Sprint-1/SCRUM-7/TEST_CASES_SCRUM_7.csv`**](../QA/Sprint-1/SCRUM-7/TEST_CASES_SCRUM_7.csv)
  * [**`QA_Testing/Sprint-1/SCRUM-7/TEST_CASES_SCRUM_7.md`**](./Sprint-1/SCRUM-7/TEST_CASES_SCRUM_7.md)

---

## 📊 Tóm tắt Kết quả Thực tế
* **SCRUM-6:** 19 TCs (PASS: 17, BLOCKED: 2, FAIL: 0, NOT RUN: 0).
* **SCRUM-7:** 8 TCs (PASS: 7 BE, NOT RUN: 1 FE Responsive, FAIL: 0, BLOCKED: 0).
