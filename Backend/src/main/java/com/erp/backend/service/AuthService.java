package com.erp.backend.service;

import com.erp.backend.dto.LoginRequest;
import com.erp.backend.dto.LoginResponse;
import com.erp.backend.entity.User;
import com.erp.backend.repository.UserRepository;
import com.erp.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION_MINUTES = 15;

    @Transactional(noRollbackFor = RuntimeException.class)
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        // 1. Tìm user trong DB theo username
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!"));

        // 2. Kiểm tra xem tài khoản có đang bị khoá tạm 15 phút không (S1-01)
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            if (user.getLockUntil() != null) {
                // Nếu vẫn chưa hết 15 phút
                if (user.getLockUntil().isAfter(LocalDateTime.now())) {
                    throw new RuntimeException(
                            "Tài khoản đang bị tạm khoá do nhập sai quá 5 lần. Vui lòng thử lại sau!");
                } else {
                    // Đã qua 15 phút -> Tự động mở khoá lại
                    user.setStatus("ACTIVE");
                    user.setFailedLoginAttempts(0);
                    user.setLockUntil(null);
                    userRepository.save(user);
                }
            } else {
                // Bị Admin khoá vĩnh viễn (S1-10)
                throw new RuntimeException("Tài khoản đã bị khoá bởi Quản trị viên!");
            }
        }

        // 3. So khớp mật khẩu nhập vào với mật khẩu đã băm BCrypt trong DB
        boolean isPasswordMatch = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!isPasswordMatch) {
            // Mật khẩu sai: Tăng số lần đăng nhập sai
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            // TIÊU CHÍ S1-01: Nếu nhập sai đủ 5 lần liên tiếp -> Khoá tạm 15 phút
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus("LOCKED");
                user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
                userRepository.save(user);
                throw new RuntimeException("Bạn đã nhập sai 5 lần liên tiếp. Tài khoản bị tạm khoá trong 15 phút!");
            } else {
                userRepository.save(user);
                int remaining = MAX_FAILED_ATTEMPTS - attempts;
                throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác! (Còn " + remaining + " lần thử)");
            }
        }

        // 4. Mật khẩu ĐÚNG: Reset số lần sai và thời điểm khoá về mặc định
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);

        // 5. Sinh chuỗi Token JWT (S1-02)
        String jwtToken = jwtUtils.generateTokenFromUsername(user.getUsername());

        // 6. Lấy danh sách Role dạng String
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        // 7. Trả về thông tin đăng nhập thành công cho Frontend
        return LoginResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
