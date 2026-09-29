package com.erp.backend.dto.user;

/**
 * S1-08: Kết quả tạo tài khoản.
 * activationEmailSent = false khi chưa cấu hình SMTP hoặc gửi mail lỗi -> Admin cần báo lại cho nhân viên.
 */
public record CreateUserResponse(UserResponse user, boolean activationEmailSent) {
}
