package com.erp.backend.security;

import com.erp.backend.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String username;
    private String fullName;
    private String email;

    @JsonIgnore
    private String password;

    private boolean isAccountNonLocked;

    // Danh sách quyền (Roles) của User theo chuẩn Spring Security
    private Collection<? extends GrantedAuthority> authorities;

    // Hàm chuyển đổi từ Entity User sang UserDetailsImpl
    public static UserDetailsImpl build(User user) {
        // Chuyển 7 Role thành các GrantedAuthority (ROLE_ADMIN, ROLE_SALES_REP...)
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        // Kiểm tra xem tài khoản có đang bị khoá tạm 15 phút (S1-01) hoặc bị Admin khoá
        // không
        boolean isLocked = "LOCKED".equalsIgnoreCase(user.getStatus()) ||
                (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now()));

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPassword(),
                !isLocked, // isAccountNonLocked = true nếu không bị khoá
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isAccountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
