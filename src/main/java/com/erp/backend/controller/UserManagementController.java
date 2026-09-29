package com.erp.backend.controller;

import com.erp.backend.dto.LockUserRequest;
import com.erp.backend.dto.UserAccountResponse;
import com.erp.backend.dto.user.*;
import com.erp.backend.entity.RoleName;
import com.erp.backend.security.UserDetailsImpl;
import com.erp.backend.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * S1-08 + S1-09 + Lock/Unlock: API quản trị tài khoản — CHỈ Quản trị hệ thống (ADMIN) được gọi.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /** Danh sách người dùng đơn giản (phục vụ chức năng khoá/mở khoá nhanh). */
    @GetMapping("/list")
    public List<UserAccountResponse> getUsers() {
        return userManagementService.getUsers();
    }

    @PatchMapping("/{userId}/lock")
    public UserAccountResponse lockUser(@PathVariable Long userId, @RequestBody(required = false) LockUserRequest request) {
        return userManagementService.lockUser(userId, request);
    }

    @PatchMapping("/{userId}/unlock")
    public UserAccountResponse unlockUser(@PathVariable Long userId) {
        return userManagementService.unlockUser(userId);
    }

    /** Tìm kiếm + lọc + phân trang (mặc định 20 dòng). Vd: ?keyword=minh&role=ROLE_SALES_REP&status=ACTIVE&page=0 */
    @GetMapping
    public PageResponse<UserResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userManagementService.search(keyword, role, status, page, size);
    }

    /** Dữ liệu cho các ô chọn trên form: danh sách vai trò, kho, địa bàn. */
    @GetMapping("/form-options")
    public UserFormOptionsResponse formOptions() {
        return userManagementService.getFormOptions();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userManagementService.getById(id);
    }

    /** Tạo tài khoản: sinh mật khẩu tạm + gửi email kích hoạt. Trả 201 Created. */
    @PostMapping
    public ResponseEntity<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userManagementService.create(request));
    }

    /** Sửa họ tên, email, số điện thoại. */
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userManagementService.update(id, request);
    }

    /** S1-09: Gán vai trò, kho, địa bàn (ghi đè danh sách cũ). */
    @PutMapping("/{id}/assignments")
    public UserResponse updateAssignments(@PathVariable Long id,
                                          @Valid @RequestBody UserAssignmentRequest request,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long currentUserId = (currentUser != null) ? currentUser.getId() : null;
        return userManagementService.updateAssignments(id, request, currentUserId);
    }
}
