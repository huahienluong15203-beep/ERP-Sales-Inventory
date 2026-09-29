package com.erp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private List<String> roles; // Trả về danh sách vai trò để Frontend hiển thị menu đúng quyền (S1-06)
    @Builder.Default
    private boolean mustChangePassword = false;
}
