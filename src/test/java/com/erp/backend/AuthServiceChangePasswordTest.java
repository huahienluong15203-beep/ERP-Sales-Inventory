package com.erp.backend;

import com.erp.backend.dto.ChangePasswordRequest;
import com.erp.backend.entity.User;
import com.erp.backend.repository.UserRepository;
import com.erp.backend.security.JwtUtils;
import com.erp.backend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceChangePasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("AC1: Mật khẩu hiện tại bị sai -> Bị từ chối")
    void testChangePassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpass");
        request.setNewPassword("NewPass123");

        User user = User.builder().id(1L).password("encoded_old_pass").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded_old_pass")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.changePassword(1L, request));
        assertTrue(exception.getMessage().contains("Mật khẩu hiện tại không chính xác"));
    }

    @Test
    @DisplayName("AC2: Mật khẩu mới không đủ 8 ký tự hoặc thiếu chữ/số -> Bị từ chối")
    void testChangePassword_WeakNewPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("correctpass");
        request.setNewPassword("12345"); // quá ngắn

        User user = User.builder().id(1L).password("encoded_old_pass").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctpass", "encoded_old_pass")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.changePassword(1L, request));
        assertTrue(exception.getMessage().contains("tối thiểu 8 ký tự"));
    }

    @Test
    @DisplayName("AC3: Mật khẩu mới trùng với mật khẩu cũ -> Bị từ chối")
    void testChangePassword_SameAsOldPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("OldPass123");

        User user = User.builder().id(1L).password("encoded_old_pass").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "encoded_old_pass")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.changePassword(1L, request));
        assertTrue(exception.getMessage().contains("không được trùng"));
    }

    @Test
    @DisplayName("AC4: Đổi mật khẩu thành công: Hash BCrypt mật khẩu mới và lưu vào DB")
    void testChangePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("BrandNewPass456");
        request.setConfirmPassword("BrandNewPass456");

        User user = User.builder().id(1L).password("encoded_old_pass").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "encoded_old_pass")).thenReturn(true);
        when(passwordEncoder.matches("BrandNewPass456", "encoded_old_pass")).thenReturn(false);
        when(passwordEncoder.encode("BrandNewPass456")).thenReturn("encoded_new_pass");

        String result = authService.changePassword(1L, request);

        assertTrue(result.contains("thành công"));
        assertEquals("encoded_new_pass", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }
}
