package com.erp.backend.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Format lỗi thống nhất: { code, message, details, timestamp }. */
public record ApiError(String code, String message, Map<String, String> details, LocalDateTime timestamp) {

    public static ApiError of(String code, String message, Map<String, String> details) {
        return new ApiError(code, message, details, LocalDateTime.now());
    }
}
