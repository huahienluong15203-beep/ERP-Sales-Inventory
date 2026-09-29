---
name: backend-testing
description: Hướng dẫn viết Unit Test và Integration Test trong Spring Boot bằng JUnit 5 và Mockito cho các tầng Service và Controller.
---

# Backend Testing Skill

## 1. Mục Tiêu & Phạm Vi Kiểm Thử
- **Unit Test (Service Layer):** Kiểm tra tính đúng đắn của logic tính toán tiền tệ, trừ tồn kho, duyệt nợ độc lập với database thật.
- **Integration Test (Controller / API):** Kiểm tra đường truyền HTTP, mã phản hồi Status Code, quyền truy cập `@PreAuthorize` và bộ lọc JWT.

---

## 2. Viết Unit Test Cho Service Bằng Mockito
Sử dụng `@ExtendWith(MockitoExtension.class)` để giả lập (mock) Repository:

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("Nên tạo đơn hàng thành công khi hàng tồn kho đủ đáp ứng")
    void shouldCreateOrderSuccessfully_WhenStockIsSufficient() {
        // 1. Given (Dữ liệu giả định)
        CreateOrderRequest request = new CreateOrderRequest(...);
        doNothing().when(inventoryService).reserveStock(anyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. When (Thực thi hàm cần test)
        OrderResponse response = orderService.createOrder(request, 1L);

        // 3. Then (Kiểm tra kết quả kỳ vọng)
        assertNotNull(response);
        verify(inventoryService, times(1)).reserveStock(anyList());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Nên ném ngoại lệ khi hàng trong kho không đủ")
    void shouldThrowException_WhenStockIsNotEnough() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(...);
        doThrow(new BusinessException("Hết hàng trong kho")).when(inventoryService).reserveStock(anyList());

        // When & Then
        assertThrows(BusinessException.class, () -> orderService.createOrder(request, 1L));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
```

---

## 3. Viết Test Controller Với MockMvc
Kiểm tra endpoint, status HTTP và validate input:

```java
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt tạm filter bảo mật nếu test riêng Controller
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/login thành công trả về 200 OK và token")
    void shouldReturn200_WhenLoginValid() throws Exception {
        LoginRequest req = new LoginRequest("admin", "admin123");
        LoginResponse res = LoginResponse.builder().token("mocked-jwt-token").username("admin").build();

        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"));
    }
}
```

---

## 4. Lệnh Chạy Kiểm Thử Nhanh
- Chạy toàn bộ test trong project:
  ```bash
  ./mvnw test
  ```
- Chạy riêng 1 class test:
  ```bash
  ./mvnw test -Dtest=OrderServiceTest
  ```
