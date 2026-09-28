# ERP Sales & Inventory System

Dự án Hệ thống Quản trị Bán hàng & Quản lý Kho (ERP Sales & Inventory System).

## 📁 Cấu trúc Quản lý Dự án (2 Repository Độc lập)

Thư mục này đóng vai trò là workspace làm việc chung. Dự án được chia tách thành **2 kho lưu trữ (repository) riêng biệt** để đảm bảo độc lập khi phát triển:

| Phân hệ | Công nghệ | Repository GitHub |
| :--- | :--- | :--- |
| **Backend** | Spring Boot 4.x / Java 17, PostgreSQL, JWT, Spring Security | [ERP-Sales-Inventory-Backend](https://github.com/huahienluong15203-beep/ERP-Sales-Inventory-Backend.git) |
| **Frontend** | React 19, TypeScript, Vite, Tailwind/CSS | [ERP-Sales-Inventory-Frontend](https://github.com/huahienluong15203-beep/ERP-Sales-Inventory-Frontend.git) |
| **QA_Testing** | Kịch bản kiểm thử, tài liệu test case | Lưu trữ tài liệu test |

---

## 🚀 Hướng dẫn cho Thành viên Mới / Khởi tạo Môi trường Làm việc

### Cách 1: Clone riêng từng repository để phát triển độc lập
- **Thành viên làm Backend:**
  ```bash
  git clone https://github.com/huahienluong15203-beep/ERP-Sales-Inventory-Backend.git Backend
  cd Backend
  ./mvnw spring-boot:run
  ```
- **Thành viên làm Frontend:**
  ```bash
  git clone https://github.com/huahienluong15203-beep/ERP-Sales-Inventory-Frontend.git Frontend
  cd Frontend
  npm install
  npm run dev
  ```

### Cách 2: Thiết lập môi trường đầy đủ (Full Workspace)
Nếu muốn mở cả 2 dự án trong cùng 1 cửa sổ IDE như workspace hiện tại:
1. Tạo thư mục `ERP-Sales-Inventory`:
   ```bash
   mkdir ERP-Sales-Inventory
   cd ERP-Sales-Inventory
   ```
2. Clone Backend và Frontend vào 2 thư mục con tương ứng:
   ```bash
   git clone https://github.com/huahienluong15203-beep/ERP-Sales-Inventory-Backend.git Backend
   git clone https://github.com/huahienluong15203-beep/ERP-Sales-Inventory-Frontend.git Frontend
   ```
3. Mở thư mục gốc `ERP-Sales-Inventory` bằng VS Code / IDE. Trình biên tập sẽ tự động nhận diện cả 2 Git repositories độc lập.

---

## 🌿 Quy định Nhánh (Branching Strategy)
- `main`: Nhánh production ổn định.
- `develop`: Nhánh tích hợp tính năng chung của team.
- `feature/<tên-tính-năng>`: Nhánh làm việc cá nhân được tách ra từ `develop`. Sau khi hoàn thành tạo Pull Request vào `develop`.
