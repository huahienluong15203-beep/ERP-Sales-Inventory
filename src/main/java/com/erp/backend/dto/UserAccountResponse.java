package com.erp.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserAccountResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String status,
        String lockReason,
        LocalDateTime lockUntil,
        List<String> roles,
        boolean handoverRequired) {
}