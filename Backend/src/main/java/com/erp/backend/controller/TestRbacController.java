package com.erp.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestRbacController {

    // 1. Phục vụ STORY S1-05: Endpoint Giá Vốn & Biên Lợi Nhuận
    // Chỉ có Quản lý kinh doanh (SALES_MANAGER) hoặc Quản trị viên (ADMIN) mới được
    // xem!
    // Các vai trò khác (như Kho, Đại lý...) truy cập vào sẽ bị chặn 403 Forbidden
    // ngay lập tức.
    @GetMapping("/cost-price")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getCostPriceAndMargin() {
        return ResponseEntity.ok(Map.of(
                "message", "Truy cập dữ liệu giá vốn thành công!",
                "productCode", "SP001",
                "costPrice", 150000,
                "profitMargin", "25%"));
    }

    // 2. Endpoint chỉ dành cho Quản trị viên (ADMIN)
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminOnlyAccess() {
        return ResponseEntity.ok(Map.of(
                "message", "Chào Quản trị viên, bạn có toàn quyền hệ thống!"));
    }
}
