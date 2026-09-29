package com.erp.backend;

import com.erp.backend.dto.ForgotPasswordRequest;
import com.erp.backend.dto.ResetPasswordRequest;
import com.erp.backend.entity.PasswordResetToken;
import com.erp.backend.entity.User;
import com.erp.backend.repository.PasswordResetTokenRepository;
import com.erp.backend.repository.UserRepository;
import com.erp.backend.security.JwtUtils;
import com.erp.backend.service.AuthService;
import com.erp.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceForgotPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "resetTokenExpirationMs", 1800000L);
        ReflectionTestUtils.setField(authService, "resetPasswordUrl", "http://localhost:5173/reset-password");
    }

    @Test
    @DisplayName("AC3: Email không tồn tại vẫn trả về cùng thông báo để bảo mật chống dò email")
    void testForgotPassword_EmailNotFound_StillReturnsGenericMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@erp.com");

        when(userRepository.findByEmail("unknown@erp.com")).thenReturn(Optional.empty());

        String message = authService.forgotPassword(request);

        assertNotNull(message);
        assertTrue(message.contains("Nếu email của bạn tồn tại trong hệ thống"));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC1: Email tồn tại sinh token và gửi link đặt lại mật khẩu")
    void testForgotPassword_EmailExists_GeneratesTokenAndSendsEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@erp.com");

        User user = User.builder().id(1L).email("user@erp.com").build();
        when(userRepository.findByEmail("user@erp.com")).thenReturn(Optional.of(user));

        String message = authService.forgotPassword(request);

        assertNotNull(message);
        verify(tokenRepository, times(1)).deleteByUser(user);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("user@erp.com"), contains("token="));
    }

    @Test
    @DisplayName("AC2: Link đã sử dụng một lần sẽ bị từ chối")
    void testResetPassword_TokenAlreadyUsed_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("SecurePass123");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("valid-token")
                .used(true) // Đã dùng
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.resetPassword(request));
        assertTrue(exception.getMessage().contains("đã được sử dụng"));
    }

    @Test
    @DisplayName("AC1: Link quá 30 phút (hết hạn) sẽ bị từ chối")
    void testResetPassword_TokenExpired_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("SecurePass123");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("expired-token")
                .used(false)
                .expiryDate(LocalDateTime.now().minusMinutes(5)) // Đã quá hạn
                .build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.resetPassword(request));
        assertTrue(exception.getMessage().contains("đã hết hạn"));
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công: Hash BCrypt, reset trạng thái khoá và đánh dấu token used")
    void testResetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("good-token");
        request.setNewPassword("NewPass123");

        User user = User.builder()
                .id(1L)
                .username("testuser")
                .status("LOCKED")
                .failedLoginAttempts(5)
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .token("good-token")
                .user(user)
                .used(false)
                .expiryDate(LocalDateTime.now().plusMinutes(20))
                .build();

        when(tokenRepository.findByToken("good-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass123")).thenReturn("hashed_new_password");

        String result = authService.resetPassword(request);

        assertTrue(result.contains("thành công"));
        assertEquals("hashed_new_password", user.getPassword());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockUntil());
        assertEquals("ACTIVE", user.getStatus());
        assertTrue(token.isUsed());

        verify(userRepository, times(1)).save(user);
        verify(tokenRepository, times(1)).save(token);
    }
}
