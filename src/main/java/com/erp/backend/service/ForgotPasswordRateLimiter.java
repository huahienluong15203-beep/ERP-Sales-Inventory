package com.erp.backend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chống spam gửi email "Quên mật khẩu" theo từng địa chỉ email:
 * - Hai lần gửi liên tiếp phải cách nhau ít nhất 60 giây.
 * - Tối đa 5 lần gửi trong 1 giờ.
 *
 * Lưu trong bộ nhớ (reset khi khởi động lại server) - đủ dùng cho quy mô dự án.
 */
@Component
public class ForgotPasswordRateLimiter {

    static final Duration COOLDOWN = Duration.ofSeconds(60);
    static final Duration WINDOW = Duration.ofHours(1);
    static final int MAX_PER_WINDOW = 5;

    private final Clock clock;
    private final Map<String, Deque<Instant>> history = new ConcurrentHashMap<>();

    public ForgotPasswordRateLimiter() {
        this(Clock.systemUTC());
    }

    ForgotPasswordRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Số giây còn phải đợi trước khi được gửi tiếp. Trả về 0 nếu được gửi ngay. */
    public long secondsUntilAllowed(String email) {
        Deque<Instant> sent = history.get(key(email));
        if (sent == null || sent.isEmpty()) {
            return 0;
        }
        Instant now = clock.instant();
        synchronized (sent) {
            prune(sent, now);
            if (sent.isEmpty()) {
                return 0;
            }
            long wait = 0;
            Instant last = sent.peekLast();
            Instant cooldownEnd = last.plus(COOLDOWN);
            if (cooldownEnd.isAfter(now)) {
                wait = secondsBetween(now, cooldownEnd);
            }
            if (sent.size() >= MAX_PER_WINDOW) {
                Instant windowEnd = sent.peekFirst().plus(WINDOW);
                wait = Math.max(wait, secondsBetween(now, windowEnd));
            }
            return wait;
        }
    }

    /** Ghi nhận một lần đã gửi email cho địa chỉ này. */
    public void recordSent(String email) {
        Instant now = clock.instant();
        Deque<Instant> sent = history.computeIfAbsent(key(email), k -> new ArrayDeque<>());
        synchronized (sent) {
            prune(sent, now);
            sent.addLast(now);
        }
    }

    private void prune(Deque<Instant> sent, Instant now) {
        while (!sent.isEmpty() && !sent.peekFirst().plus(WINDOW).isAfter(now)) {
            sent.pollFirst();
        }
    }

    private long secondsBetween(Instant from, Instant to) {
        long millis = Duration.between(from, to).toMillis();
        return Math.max(1, (millis + 999) / 1000); // làm tròn lên, tối thiểu 1 giây
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
