package com.erp.backend.dto.user;

import java.util.List;

/** Dữ liệu cho các ô chọn trên form tạo/sửa tài khoản: vai trò, kho, địa bàn. */
public record UserFormOptionsResponse(List<String> roles, List<RefItem> warehouses, List<RefItem> regions) {
}
