package com.erp.backend.controller;

import com.erp.backend.dto.LoginRequest;
import com.erp.backend.dto.LoginResponse;
import com.erp.backend.dto.MessageResponse;
import com.erp.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import com.erp.backend.dto.ForgotPasswordRequest;
import com.erp.backend.dto.ResetPasswordRequest;

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

    // 3. API QUÊN MẬT KHẨU (Gửi mail đặt lại mật khẩu)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            String message = authService.forgotPassword(request);
            return ResponseEntity.ok(new MessageResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // 4. API ĐẶT LẠI MẬT KHẨU MỚI
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            String message = authService.resetPassword(request);
            return ResponseEntity.ok(new MessageResponse(message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

}
