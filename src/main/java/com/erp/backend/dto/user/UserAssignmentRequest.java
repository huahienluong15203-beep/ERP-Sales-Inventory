package com.erp.backend.dto.user;

import com.erp.backend.entity.RoleName;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/** S1-09: Gán vai trò, kho và địa bàn cho một người dùng (ghi đè toàn bộ danh sách cũ). */
@Getter
@Setter
public class UserAssignmentRequest {

    @NotEmpty(message = "Phải chọn ít nhất một vai trò")
    private Set<RoleName> roles = new HashSet<>();

    private Set<Long> warehouseIds = new HashSet<>();

    private Set<Long> regionIds = new HashSet<>();
}
