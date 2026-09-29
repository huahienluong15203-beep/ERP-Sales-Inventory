package com.erp.backend.dto.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * S1-08: Sửa thông tin cơ bản của tài khoản.
 * Không sửa tên tài khoản và mật khẩu ở đây. Vai trò/kho/địa bàn sửa qua API assignments (S1-09).
 * Khoá/mở khoá thuộc S1-10.
 */
@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^$|^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "Số điện thoại Việt Nam không hợp lệ")
    private String phone;
}
