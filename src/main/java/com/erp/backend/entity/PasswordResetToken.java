package com.erp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    // Đảm bảo tiêu chí: Link chỉ dùng được một lần duy nhất
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean used = false;

    // Hàm tiện ích kiểm tra xem token đã hết hạn 30 phút chưa
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
