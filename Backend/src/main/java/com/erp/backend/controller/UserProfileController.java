package com.erp.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/navigation")
public class UserProfileController {

    /**
     * API trả về thông tin người dùng và danh sách menu theo quyền (SCRUM-7)
     * - Hiển thị tên, vai trò, kho / địa bàn làm việc
     * - Menu phân quyền theo Role
     */
    @GetMapping("/user-context")
    public ResponseEntity<Map<String, Object>> getUserNavigationContext(
            @RequestParam(defaultValue = "ROLE_STAFF") String role) {

        Map<String, Object> response = new HashMap<>();

        // 1. Thông tin người dùng, vai trò và kho / địa bàn làm việc
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("fullName", "Nguyễn Văn Minh");
        userInfo.put("role", role);
        userInfo.put("warehouse", "Kho Tổng Miền Bắc");
        userInfo.put("workLocation", "Khu công nghiệp Tiên Sơn, Bắc Ninh");
        response.put("user", userInfo);

        // 2. Danh sách menu lọc theo quyền
        List<Map<String, String>> menus = new ArrayList<>();

        menus.add(createMenuItem("Trang chủ", "/dashboard", "home"));
        menus.add(createMenuItem("Hồ sơ cá nhân", "/profile", "user"));

        if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
            menus.add(createMenuItem("Quản lý tài khoản", "/admin/users", "users-cog"));
            menus.add(createMenuItem("Phân quyền hệ thống", "/admin/roles", "shield-alt"));
            menus.add(createMenuItem("Quản lý kho hàng", "/warehouse/manage", "warehouse"));
            menus.add(createMenuItem("Báo cáo doanh thu", "/reports/sales", "chart-line"));
        } else if ("ROLE_WAREHOUSE_MANAGER".equalsIgnoreCase(role)) {
            menus.add(createMenuItem("Quản lý kho hàng", "/warehouse/manage", "warehouse"));
            menus.add(createMenuItem("Nhập / Xuất kho", "/warehouse/stock", "boxes"));
            menus.add(createMenuItem("Kiểm kê hàng tồn", "/warehouse/inventory", "clipboard-list"));
        } else {
            // Nhân viên thông thường (STAFF / SALES)
            menus.add(createMenuItem("Tra cứu tồn kho", "/warehouse/lookup", "search"));
            menus.add(createMenuItem("Tạo đơn xuất hàng", "/sales/orders/create", "file-invoice"));
        }

        response.put("menus", menus);

        return ResponseEntity.ok(response);
    }

    private Map<String, String> createMenuItem(String title, String path, String icon) {
        Map<String, String> item = new HashMap<>();
        item.put("title", title);
        item.put("path", path);
        item.put("icon", icon);
        return item;
    }
}
