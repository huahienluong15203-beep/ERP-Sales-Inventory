package com.erp.backend.controller;

import com.erp.backend.dto.LoginRequest;
import com.erp.backend.dto.LoginResponse;
import com.erp.backend.dto.MessageResponse;
import com.erp.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import com.erp.backend.dto.ChangePasswordRequest;
import com.erp.backend.security.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. API ĐĂNG NHẬP (Story S1-01)
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Trả về thông báo lỗi rõ ràng (ví dụ: sai pass hoặc bị khoá 15 phút)
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // 2. API ĐĂNG XUẤT (Story S1-02)
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok(new MessageResponse("Đăng xuất thành công!"));
    }

    // 3. API ĐỔI MẬT KHẨU KHI ĐANG ĐĂNG NHẬP
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody ChangePasswordRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Vui lòng đăng nhập để thực hiện đổi mật khẩu!"));
        }

        try {
            String message = authService.changePassword(userDetails.getId(), request);
            return ResponseEntity.ok(new MessageResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
