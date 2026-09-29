---
name: security-rbac
description: Hướng dẫn bảo mật Spring Security 6, cơ chế JWT Bearer Token, kiểm soát phân quyền phương thức (@PreAuthorize) dựa trên 7 vai trò người dùng trong ERP.
---

# Security & RBAC Skill

## 1. Tổng Quan Kiến Trúc Bảo Mật
Dự án sử dụng cơ chế bảo mật phi trạng thái (Stateless) với **Spring Security** và **JSON Web Token (JWT)**:
- Người dùng gửi username/password đến `/api/auth/login`.
- Hệ thống kiểm tra thông tin, tạo chuỗi JWT chứa thông tin định danh và danh sách quyền (Roles).
- Mỗi request tiếp theo từ Client gửi kèm header: `Authorization: Bearer <token>`.
- `JwtAuthenticationFilter` giải mã token và nạp `UserDetails` vào `SecurityContextHolder`.

---

## 2. 7 Vai Trò Nghiệp Vụ Chuẩn
Trong file `RoleName.java`:
1. `ROLE_ADMIN`: Toàn quyền hệ thống.
2. `ROLE_SALES_REP`: Nhân viên kinh doanh.
3. `ROLE_SALES_MANAGER`: Quản lý kinh doanh (xem giá vốn, duyệt chiết khấu/nợ).
4. `ROLE_WAREHOUSE`: Nhân viên thủ kho (nhập, xuất, kiểm hàng).
5. `ROLE_WH_MANAGER`: Quản lý kho (duyệt kiểm kê, điều chuyển kho).
6. `ROLE_ACCOUNTANT`: Kế toán công nợ.
7. `ROLE_CUSTOMER`: Đại lý / Khách hàng thân thiết.

---

## 3. Quy Tắc Phân Quyền Bằng Annotations (@PreAuthorize)
Khai báo phân quyền trực tiếp trên từng phương thức trong Controller:

### Ví dụ 1: Chỉ Admin được phép cấu hình tài khoản
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api/v1/users")
public ResponseEntity<?> createUser(...) { ... }
```

### Ví dụ 2: Quản lý hoặc Nhân viên kinh doanh được xem danh sách đơn hàng
```java
@PreAuthorize("hasAnyRole('SALES_REP', 'SALES_MANAGER', 'ADMIN')")
@GetMapping("/api/v1/orders")
public ResponseEntity<?> getOrders(...) { ... }
```

### Ví dụ 3: Chỉ Quản lý kinh doanh hoặc Admin mới được xem giá vốn sản phẩm (S1-05)
```java
@PreAuthorize("hasAnyRole('SALES_MANAGER', 'ADMIN')")
@GetMapping("/api/v1/products/{id}/cost-price")
public ResponseEntity<?> getProductCostPrice(@PathVariable Long id) { ... }
```

---

## 4. Cơ Chế Khóa Tài Khoản Tự Động (Brute Force Protection)
- Trong `AuthService.java`:
  - Mỗi lần đăng nhập sai: Tăng `failedLoginAttempts += 1`.
  - Nếu `failedLoginAttempts >= 5`: Thiết lập `lockUntil = LocalDateTime.now().plusMinutes(15)` và trả về thông báo lỗi "Tài khoản tạm thời bị khóa 15 phút do nhập sai quá nhiều lần".
  - Khi đăng nhập thành công: Reset `failedLoginAttempts = 0` và `lockUntil = null`.

---

## 5. Lấy Thông Tin Người Dùng Hiện Tại (Current User Context)
Trong Controller, sử dụng `@AuthenticationPrincipal`:
```java
@GetMapping("/me")
public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
    Long userId = userDetails.getId();
    String username = userDetails.getUsername();
    Collection<? extends GrantedAuthority> roles = userDetails.getAuthorities();
    return ResponseEntity.ok(...);
}
```
