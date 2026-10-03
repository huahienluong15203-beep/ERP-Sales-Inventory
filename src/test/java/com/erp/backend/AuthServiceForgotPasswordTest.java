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
import com.erp.backend.service.ForgotPasswordRateLimiter;
import com.erp.backend.service.ForgotPasswordFailureLimiter;
import com.erp.backend.exception.TooManyRequestsException;
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

    @Mock
    private ForgotPasswordRateLimiter forgotPasswordRateLimiter;

    @Mock
    private ForgotPasswordFailureLimiter forgotPasswordFailureLimiter;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "resetTokenExpirationMs", 1800000L);
        ReflectionTestUtils.setField(authService, "resetPasswordUrl", "http://localhost:5173/reset-password");
    }

    private ForgotPasswordRequest request(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    @Test
    @DisplayName("Bỏ trống email -> báo yêu cầu nhập, không truy vấn DB")
    void testForgotPassword_EmptyEmail_Rejected() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.forgotPassword(request("   ")));
        assertTrue(ex.getMessage().contains("Vui lòng nhập"));
        verifyNoInteractions(userRepository, emailService);
    }

    @Test
    @DisplayName("Email sai định dạng -> báo lỗi định dạng, không truy vấn DB, không gửi mail")
    void testForgotPassword_InvalidFormat_Rejected() {
        for (String bad : new String[]{"abc", "abc@", "abc@gmail", "a b@gmail.com"}) {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.forgotPassword(request(bad)));
            assertTrue(ex.getMessage().contains("không đúng định dạng"), "Email: " + bad);
        }
        verifyNoInteractions(userRepository, emailService);
    }

    @Test
    @DisplayName("Email chưa có trong hệ thống -> báo rõ để người dùng điều chỉnh, không gửi mail")
    void testForgotPassword_EmailNotFound_Rejected() {
        when(userRepository.findByEmailIgnoreCase("unknown@erp.com")).thenReturn(Optional.empty());
        when(forgotPasswordFailureLimiter.recordFailure("1.2.3.4")).thenReturn(4);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.forgotPassword(request("unknown@erp.com"), "1.2.3.4"));

        assertTrue(ex.getMessage().contains("chưa được đăng ký"));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC1: Email tồn tại -> sinh token, ghi nhận lượt gửi và gửi link đặt lại mật khẩu")
    void testForgotPassword_EmailExists_GeneratesTokenAndSendsEmail() {
        User user = User.builder().id(1L).email("user@erp.com").build();
        when(userRepository.findByEmailIgnoreCase("User@erp.com")).thenReturn(Optional.of(user));

        String message = authService.forgotPassword(request(" User@erp.com "));

        assertTrue(message.contains("Đã gửi liên kết"));
        verify(tokenRepository, times(1)).deleteByUser(user);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(forgotPasswordRateLimiter).recordSent("User@erp.com");
        verify(forgotPasswordFailureLimiter).reset(null); // nhập đúng -> xoá bộ đếm sai
        verify(emailService, times(1)).sendPasswordResetEmail(eq("user@erp.com"), contains("token="));
    }

    @Test
    @DisplayName("Nhập email chưa đăng ký lần thứ 5 -> khoá 1 phút (429)")
    void testForgotPassword_FifthWrongEmail_Locked() {
        when(userRepository.findByEmailIgnoreCase("unknown@erp.com")).thenReturn(Optional.empty());
        when(forgotPasswordFailureLimiter.recordFailure("1.2.3.4")).thenReturn(0);
        when(forgotPasswordFailureLimiter.secondsLocked("1.2.3.4")).thenReturn(0L, 60L);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> authService.forgotPassword(request("unknown@erp.com"), "1.2.3.4"));

        assertEquals(60L, ex.getRetryAfterSeconds());
        assertTrue(ex.getMessage().contains("1 phút"));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Đang bị khoá do nhập sai nhiều lần -> từ chối ngay, không tra DB, không gửi mail")
    void testForgotPassword_WhileLocked_Rejected() {
        when(forgotPasswordFailureLimiter.secondsLocked("1.2.3.4")).thenReturn(120L);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> authService.forgotPassword(request("user@erp.com"), "1.2.3.4"));

        assertEquals(120L, ex.getRetryAfterSeconds());
        assertTrue(ex.getMessage().contains("2 phút"));
        verifyNoInteractions(userRepository, emailService);
    }

    @Test
    @DisplayName("Chống spam: yêu cầu lại quá nhanh -> 429, không sinh token, không gửi mail")
    void testForgotPassword_TooSoon_Throttled() {
        User user = User.builder().id(1L).email("user@erp.com").build();
        when(userRepository.findByEmailIgnoreCase("user@erp.com")).thenReturn(Optional.of(user));
        when(forgotPasswordRateLimiter.secondsUntilAllowed("user@erp.com")).thenReturn(45L);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> authService.forgotPassword(request("user@erp.com")));

        assertEquals(45L, ex.getRetryAfterSeconds());
        assertTrue(ex.getMessage().contains("45 giây"));
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(forgotPasswordRateLimiter, never()).recordSent(anyString());
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
