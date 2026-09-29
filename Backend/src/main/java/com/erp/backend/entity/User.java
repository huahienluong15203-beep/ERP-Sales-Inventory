package com.erp.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password; // Lưu mật khẩu đã mã hoá BCrypt

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    // Trạng thái tài khoản: ACTIVE (Hoạt động), LOCKED (Khoá)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    // Phục vụ S1-01: Đếm số lần đăng nhập sai (sai 5 lần liên tiếp)
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    // Phục vụ S1-01: Thời điểm hết hạn khoá tạm 15 phút
    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    // Phục vụ S1-05: Phân quyền theo 7 vai trò (1 user có thể có nhiều vai trò)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
