package com.erp.backend.controller;

import com.erp.backend.dto.user.PersonalProfileResponse;
import com.erp.backend.dto.user.UpdatePersonalProfileRequest;
import com.erp.backend.security.UserDetailsImpl;
import com.erp.backend.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller xử lý xem và cập nhật hồ sơ cá nhân (S2-02).
 * Dành cho mọi người dùng đã đăng nhập trong hệ thống (Tất cả 7 vai trò).
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "API Quản lý hồ sơ cá nhân người dùng (S2-02)")
public class PersonalProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "Xem hồ sơ cá nhân", description = "Lấy thông tin tài khoản của chính người dùng đang đăng nhập")
    public ResponseEntity<PersonalProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
        return ResponseEntity.ok(userProfileService.getProfile(userDetails.getId()));
    }

    @PutMapping
    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Cập nhật Họ tên và Số điện thoại (kiểm tra định dạng VN). Không thể tự đổi username, email, vai trò, kho, địa bàn.")
    public ResponseEntity<PersonalProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdatePersonalProfileRequest request) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
        return ResponseEntity.ok(userProfileService.updateProfile(userDetails.getId(), request));
    }
}
