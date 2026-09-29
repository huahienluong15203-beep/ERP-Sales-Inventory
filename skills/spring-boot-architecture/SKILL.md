---
name: spring-boot-architecture
description: Hướng dẫn cấu trúc phân lớp chuẩn trong Spring Boot 3 (Controller -> Service -> Repository -> Entity/DTO), xử lý Transactional và tập trung ngoại lệ toàn cục.
---

# Spring Boot Architecture Skill

## 1. Cấu Trúc Phân Lớp Chuẩn (Layered Architecture)
Mọi mã nguồn Backend bắt buộc phải tuân theo luồng phân tầng nghiêm ngặt:
`Client -> Controller -> Service Layer -> Repository Layer -> PostgreSQL Database`

```text
src/main/java/com/erp/backend/
├── config/             # Cấu hình Spring (Security, Swagger, WebMvc, DataInitializer)
├── controller/         # Tiếp nhận HTTP Request, validate DTO, gọi Service, trả ResponseEntity
├── dto/                # Data Transfer Objects (Request/Response payload, không chứa logic)
│   ├── request/        # LoginRequest, CreateOrderRequest, StockImportRequest
│   └── response/       # LoginResponse, OrderDetailResponse, ApiResponse<T>
├── entity/             # JPA Entities ánh xạ trực tiếp với bảng trong CSDL
├── repository/         # Spring Data JPA Interfaces truy vấn database
├── security/           # JWT, UserDetailsImpl, JwtFilter, AuthenticationEntryPoint
└── service/            # Chứa 100% logic nghiệp vụ, tính toán, xử lý Transaction
    └── impl/           # (Tùy chọn) Triển khai interface nếu cần đa hình
```

---

## 2. Quy Tắc Cốt Lõi Cho Từng Tầng

### 2.1. Controller Layer
- **Trách nhiệm:** Chỉ nhận dữ liệu từ Client, gọi Service tương ứng và trả về `ResponseEntity<ApiResponse<T>>`.
- **Tuyệt đối KHÔNG:** 
  - Không viết logic tính toán, cộng trừ tồn kho, tính tiền trong Controller.
  - Không inject `Repository` trực tiếp vào `Controller`.
  - Không nhận trực tiếp `Entity` làm `@RequestBody`, bắt buộc dùng DTO có `@Valid`.
- **Ví dụ chuẩn:**
```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SALES_REP', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        OrderResponse result = orderService.createOrder(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Tạo đơn hàng thành công"));
    }
}
```

### 2.2. Service Layer
- **Trách nhiệm:** Nơi chứa toàn bộ nghiệp vụ (kiểm tra hạn mức nợ, kiểm tra tồn kho FEFO, tính chiết khấu, trừ kho).
- **Quy tắc Transaction:** Bắt buộc dùng `@Transactional` cho bất kỳ phương thức nào có thao tác ghi/cập nhật nhiều hơn một bảng để đảm bảo tính toàn vẹn (ACID). Nếu có lỗi xảy ra phải tự động Rollback.
```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        // 1. Kiểm tra tồn kho và khóa tạm thời (Reservation)
        inventoryService.reserveStock(request.getItems());

        // 2. Tạo đơn hàng và chi tiết đơn hàng
        Order order = new Order(...);
        orderRepository.save(order);

        return OrderResponse.fromEntity(order);
    }
}
```

### 2.3. Repository Layer
- Kế thừa `JpaRepository<Entity, ID>`.
- Ưu tiên sử dụng Spring Data Derived Queries (vd: `findByStatusAndWarehouseId`).
- Đối với truy vấn phức tạp hoặc báo cáo, viết `@Query` với JPQL hoặc Native Query có đánh Index.

---

## 3. Quản Lý Lỗi Tập Trung (Global Exception Handler)
Không dùng `try-catch` bừa bãi tại Controller. Sử dụng `@RestControllerAdvice` để bắt ngoại lệ:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Dữ liệu không hợp lệ", errors));
    }
}
```
