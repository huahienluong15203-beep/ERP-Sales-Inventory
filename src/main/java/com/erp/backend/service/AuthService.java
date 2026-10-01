package com.erp.backend.service;

import com.erp.backend.dto.ForgotPasswordRequest;
import com.erp.backend.dto.LoginRequest;
import com.erp.backend.dto.LoginResponse;
import com.erp.backend.dto.ResetPasswordRequest;
import com.erp.backend.entity.PasswordResetToken;
import com.erp.backend.entity.User;
import com.erp.backend.exception.TooManyRequestsException;
import com.erp.backend.repository.PasswordResetTokenRepository;
import com.erp.backend.repository.UserRepository;
import com.erp.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    private final ForgotPasswordFailureLimiter forgotPasswordFailureLimiter;

    /** Định dạng email hợp lệ: ten@tenmien.duoi (vd: nguyenvana@gmail.com). */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    @Value("${erp.app.resetPasswordExpirationMs:1800000}")
    private long resetTokenExpirationMs;

    @Value("${erp.app.resetPasswordUrl:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION_MINUTES = 15;

    // ... (Giữ nguyên hàm authenticateUser cũ) ...
    @Transactional(noRollbackFor = RuntimeException.class)
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        // [Toàn bộ logic hàm authenticateUser cũ giữ nguyên 100%]
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!"));

        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            if (user.getLockUntil() != null) {
                if (user.getLockUntil().isAfter(LocalDateTime.now())) {
                    throw new RuntimeException(
                            "Tài khoản đang bị tạm khoá do nhập sai quá 5 lần. Vui lòng thử lại sau!");
                } else {
                    user.setStatus("ACTIVE");
                    user.setFailedLoginAttempts(0);
                    user.setLockUntil(null);
                    userRepository.save(user);
                }
            } else {
                throw new RuntimeException("Tài khoản đã bị khoá bởi Quản trị viên!");
            }
        }

        boolean isPasswordMatch = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!isPasswordMatch) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
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

        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);

        // Single Active Session (Đơn phiên): Cấp Session ID mới, vô hiệu hoá phiên cũ
        String newSessionId = UUID.randomUUID().toString();
        user.setActiveSessionId(newSessionId);
        userRepository.save(user);

        String jwtToken = jwtUtils.generateTokenFromUsernameAndSession(user.getUsername(), newSessionId);

        // Sắp xếp roles theo thứ tự ưu tiên để frontend luôn chọn đúng vai trò cao nhất
        // ROLE_ADMIN → ROLE_SALES_MANAGER → ROLE_WH_MANAGER → ROLE_ACCOUNTANT → ...
        List<String> roleOrder = List.of(
                "ROLE_ADMIN", "ROLE_SALES_MANAGER", "ROLE_WH_MANAGER",
                "ROLE_ACCOUNTANT", "ROLE_WAREHOUSE", "ROLE_SALES_REP", "ROLE_CUSTOMER"
        );
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted((a, b) -> {
                    int ia = roleOrder.indexOf(a);
                    int ib = roleOrder.indexOf(b);
                    if (ia < 0) ia = roleOrder.size();
                    if (ib < 0) ib = roleOrder.size();
                    return ia - ib;
                })
                .toList();

        return LoginResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(roles)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    // Đăng xuất: Vô hiệu hoá phiên làm việc hiện tại của người dùng
    @Transactional
    public void logout(String username) {
        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setActiveSessionId(null);
                userRepository.save(user);
            });
        }
    }

    // 8. TÍNH NĂNG QUÊN MẬT KHẨU (Gửi mail kèm link 30 phút)
    //    Thứ tự kiểm tra:
    //    (1) Email bắt buộc nhập  (2) Đúng định dạng
    //    (3) Chống dò email: nhập email chưa đăng ký sai 5 lần -> khoá 1 phút (theo từng máy)
    //    (4) Có tồn tại trong hệ thống
    //    (5) Chống spam: 2 lần gửi cách nhau >= 1 phút  (6) Gửi mail CHẠY NGẦM
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        return forgotPassword(request, null);
    }

    /**
     * @param clientKey định danh máy gửi yêu cầu (địa chỉ IP) - dùng để khoá khi nhập sai quá nhiều.
     */
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request, String clientKey) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();

        // (1) Bắt buộc nhập
        if (email.isEmpty()) {
            throw new RuntimeException("Vui lòng nhập địa chỉ email!");
        }

        // (2) Kiểm tra định dạng
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Email không đúng định dạng (ví dụ đúng: nguyenvana@gmail.com). Vui lòng kiểm tra lại!");
        }

        // (3) Đang bị khoá do nhập sai quá nhiều lần?
        long lockedSeconds = forgotPasswordFailureLimiter.secondsLocked(clientKey);
        if (lockedSeconds > 0) {
            throw new TooManyRequestsException(
                    "Bạn đã nhập sai email quá nhiều lần. Vui lòng thử lại sau " + formatWait(lockedSeconds) + ".",
                    lockedSeconds);
        }

        // (4) Email phải tồn tại trong hệ thống
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            int remaining = forgotPasswordFailureLimiter.recordFailure(clientKey);
            if (remaining == 0) {
                long lock = forgotPasswordFailureLimiter.secondsLocked(clientKey);
                throw new TooManyRequestsException(
                        "Bạn đã nhập sai email quá nhiều lần. Vui lòng thử lại sau " + formatWait(lock) + ".",
                        lock);
            }
            throw new RuntimeException("Email '" + email + "' chưa được đăng ký trong hệ thống. "
                    + "Vui lòng kiểm tra lại (còn " + remaining + " lần thử).");
        }
        forgotPasswordFailureLimiter.reset(clientKey);

        // (5) Chống spam gửi email liên tục
        long waitSeconds = forgotPasswordRateLimiter.secondsUntilAllowed(email);
        if (waitSeconds > 0) {
            throw new TooManyRequestsException(
                    "Bạn vừa yêu cầu gửi email đặt lại mật khẩu. Vui lòng đợi " + formatWait(waitSeconds)
                            + " rồi thử lại, và kiểm tra cả hộp thư Spam.",
                    waitSeconds);
        }

        // Xoá các token cũ chưa sử dụng của user này (nếu có)
        tokenRepository.deleteByUser(user);

        // Sinh token ngẫu nhiên UUID
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(resetTokenExpirationMs / 1000);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        tokenRepository.save(resetToken);
        forgotPasswordRateLimiter.recordSent(email);

        // (6) Gửi email CHẠY NGẦM, sau khi token đã lưu DB thành công -> API trả kết quả ngay
        String toEmail = user.getEmail();
        String resetLink = resetPasswordUrl + "?token=" + token;
        AfterCommit.run(() -> emailService.sendPasswordResetEmail(toEmail, resetLink));

        return "Đã gửi liên kết đặt lại mật khẩu tới " + toEmail
                + ". Vui lòng kiểm tra hộp thư (kể cả mục Spam). Liên kết có hiệu lực trong 30 phút.";
    }

    /** Hiển thị thời gian chờ dễ đọc: "45 giây" hoặc "12 phút". */
    private String formatWait(long seconds) {
        if (seconds < 60) {
            return seconds + " giây";
        }
        return ((seconds + 59) / 60) + " phút";
    }

    // 9. TÍNH NĂNG ĐẶT LẠI MẬT KHẨU MỚI (Từ link trong email)
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new RuntimeException("Mã token xác thực không hợp lệ!");
        }

        // Tiêu chí: Mật khẩu mới tối thiểu 8 ký tự, có cả chữ và số
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 8 || !newPassword.matches(".*[a-zA-Z].*")
                || !newPassword.matches(".*[0-9].*")) {
            throw new RuntimeException("Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và số!");
        }

        // Tìm token trong DB
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Liên kết đặt lại mật khẩu không hợp lệ hoặc không tồn tại!"));

        // Tiêu chí: Link chỉ dùng được một lần
        if (resetToken.isUsed()) {
            throw new RuntimeException("Liên kết đặt lại mật khẩu này đã được sử dụng rồi!");
        }

        // Tiêu chí: Link hiệu lực trong 30 phút
        if (resetToken.isExpired()) {
            throw new RuntimeException("Liên kết đặt lại mật khẩu đã hết hạn (quá 30 phút). Vui lòng yêu cầu lại!");
        }

        // Đổi mật khẩu thành công: Hash BCrypt và reset trạng thái khoá
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);

        // Đánh dấu token đã sử dụng
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return "Đặt lại mật khẩu thành công! Bây giờ bạn đã có thể đăng nhập bằng mật khẩu mới.";
    }

    // 10. TÍNH NĂNG ĐỔI MẬT KHẨU KHI ĐANG ĐĂNG NHẬP
    @Transactional
    public String changePassword(Long userId, com.erp.backend.dto.ChangePasswordRequest request) {
        if (userId == null) {
            throw new RuntimeException("Không tìm thấy thông tin phiên đăng nhập của người dùng!");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu hiện tại!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng!"));

        // Tiêu chí: Mật khẩu hiện tại phải chính xác
        boolean isPasswordMatch = passwordEncoder.matches(request.getCurrentPassword(), user.getPassword());
        if (!isPasswordMatch) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác!");
        }

        // Tiêu chí: Mật khẩu mới tối thiểu 8 ký tự, có cả chữ cái và số
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 8
                || !newPassword.matches(".*[a-zA-Z].*")
                || !newPassword.matches(".*[0-9].*")) {
            throw new RuntimeException("Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và số!");
        }

        // Tiêu chí: Mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu hiện tại!");
        }

        // Kiểm tra xác nhận mật khẩu (nếu có nhập)
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!newPassword.equals(request.getConfirmPassword())) {
                throw new RuntimeException("Xác nhận mật khẩu mới không trùng khớp!");
            }
        }

        // Băm mật khẩu mới bằng BCrypt và lưu vào DB
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        // S1-04: Ghi nhận thời điểm đổi mật khẩu -> JwtAuthenticationFilter sẽ thu hồi
        //        mọi token cũ (phiên đăng nhập khác) được cấp trước thời điểm này
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        return "Đổi mật khẩu thành công! Mật khẩu mới đã có hiệu lực. Các phiên đăng nhập khác đã được thu hồi.";

    }
}

