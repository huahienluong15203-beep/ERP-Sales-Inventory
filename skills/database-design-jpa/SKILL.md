---
name: database-design-jpa
description: Quy chuẩn thiết kế CSDL PostgreSQL và Entity JPA cho hệ thống ERP Sales & Inventory (khóa ngoại, quan hệ OneToMany, chống N+1 query, audit timestamps).
---

# Database Design & JPA Skill

## 1. Quy Chuẩn Đặt Tên Trong PostgreSQL & Entity JPA
- **Tên bảng:** Viết thường số nhiều, dùng gạch dưới `snake_case` (vd: `users`, `products`, `orders`, `order_items`, `warehouses`, `stock_lots`).
- **Tên cột:** Viết thường `snake_case` (vd: `user_id`, `created_at`, `expired_date`, `unit_price`).
- **Khóa chính (PK):** Đặt tên là `id` kiểu `BIGSERIAL` / `Long`.
- **Khóa ngoại (FK):** Đặt tên theo `<tên_bảng_số_ít>_id` (vd: `customer_id`, `warehouse_id`, `order_id`).

---

## 2. Quy Chuẩn Thiết Kế Entity JPA

### 2.1. Lớp Base Entity (Audit Timestamps)
Mọi Entity nghiệp vụ nên kế thừa lớp cơ sở để tự động theo dõi thời gian tạo và cập nhật:
```java
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 2.2. Thiết Lập Quan Hệ Giữa Các Bảng
- **Quy tắc vàng:** Luôn dùng `FetchType.LAZY` cho `@ManyToOne` và `@OneToMany` để tránh tải thừa dữ liệu gây chậm hệ thống.
```java
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", unique = true, nullable = false, length = 50)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
```

---

## 3. Khắc Phục Lỗi N+1 Query Kinh Điển
Khi truy vấn danh sách đơn hàng hoặc sản phẩm kèm chi tiết:
- **KHÔNG NÊN:** Dùng hàm `findAll()` mặc định vì Hibernate sẽ chạy 1 query lấy Orders và thêm N queries lấy Customer/Items.
- **NÊN:** Sử dụng `JOIN FETCH` trong JPQL hoặc `@EntityGraph`:
```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.status = :status")
    List<Order> findAllByStatusWithCustomer(@Param("status") String status);
}
```

---

## 4. Bảng Tra Cứu Thực Thể Nghiệp Vụ Cốt Lõi
1. **`users` & `roles` & `user_roles`:** Quản lý người dùng và 7 vai trò hệ thống.
2. **`products` & `categories`:** Sản phẩm, danh mục, đơn vị tính (cái, hộp, thùng).
3. **`warehouses` & `stock_lots`:** Quản lý kho, lô hàng, ngày sản xuất và hạn sử dụng (phục vụ xuất kho FEFO).
4. **`orders` & `order_items`:** Đơn hàng, trạng thái (PENDING, APPROVED, EXPORTED, CANCELLED), số lượng, đơn giá, chiết khấu.
5. **`stock_movements` (Phiếu kho):** Lịch sử nhập kho, xuất kho, điều chuyển kho.
