package com.erp.backend.dto.user;

import com.erp.backend.entity.RoleName;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/** S1-08: Dữ liệu Admin gửi lên khi tạo tài khoản mới (kèm gán vai trò/kho/địa bàn của S1-09). */
@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(min = 3, max = 50, message = "Tên tài khoản từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Tên tài khoản chỉ gồm chữ không dấu, số, dấu chấm và gạch dưới")
    private String username;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^$|^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "Số điện thoại Việt Nam không hợp lệ")
    private String phone;

    @NotEmpty(message = "Phải chọn ít nhất một vai trò")
    private Set<RoleName> roles = new HashSet<>();

    private Set<Long> warehouseIds = new HashSet<>();

    private Set<Long> regionIds = new HashSet<>();
}
