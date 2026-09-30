package com.erp.backend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chống dò email ở màn "Quên mật khẩu", tính theo từng máy (địa chỉ IP):
 * nhập email CHƯA ĐĂNG KÝ sai 5 lần -> khoá tạm 5 phút.
 * Nhập đúng email (gửi mail thành công) thì đếm lại từ đầu.
 *
 * Lưu trong bộ nhớ (reset khi khởi động lại server) - đủ dùng cho quy mô dự án.
 */
@Component
public class ForgotPasswordFailureLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final Clock clock;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    private static final class State {
        int failures;
        Instant lockedUntil;
    }

    public ForgotPasswordFailureLimiter() {
        this(Clock.systemUTC());
    }

    ForgotPasswordFailureLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Số giây còn bị khoá. Trả về 0 nếu không bị khoá. */
    public long secondsLocked(String clientKey) {
        State state = states.get(key(clientKey));
        if (state == null) {
            return 0;
        }
        synchronized (state) {
            Instant now = clock.instant();
            if (state.lockedUntil == null) {
                return 0;
            }
            if (!state.lockedUntil.isAfter(now)) {
                // Hết thời gian khoá -> cho nhập lại từ đầu
                state.lockedUntil = null;
                state.failures = 0;
                return 0;
            }
            long millis = Duration.between(now, state.lockedUntil).toMillis();
            return Math.max(1, (millis + 999) / 1000);
        }
    }

    /**
     * Ghi nhận một lần nhập email chưa đăng ký.
     * @return số lần thử còn lại (0 nghĩa là vừa bị khoá).
     */
    public int recordFailure(String clientKey) {
        State state = states.computeIfAbsent(key(clientKey), k -> new State());
        synchronized (state) {
            state.failures++;
            if (state.failures >= MAX_FAILURES) {
                state.lockedUntil = clock.instant().plus(LOCK_DURATION);
                return 0;
            }
            return MAX_FAILURES - state.failures;
        }
    }

    /** Nhập đúng email -> xoá bộ đếm sai. */
    public void reset(String clientKey) {
        states.remove(key(clientKey));
    }

    private String key(String clientKey) {
        return clientKey == null ? "unknown" : clientKey;
    }
}
