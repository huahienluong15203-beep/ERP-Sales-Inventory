package com.erp.backend.config;

import com.erp.backend.entity.Role;
import com.erp.backend.entity.RoleName;
import com.erp.backend.entity.User;
import com.erp.backend.repository.RoleRepository;
import com.erp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Khởi tạo 7 Vai trò nghiệp vụ nếu trong DB chưa có
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder()
                        .name(roleName)
                        .description("Vai trò " + roleName.name())
                        .build());
            }
        }

        // 2. Tạo sẵn tài khoản ADMIN mẫu: admin / admin123
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy Role Admin"));

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);

            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123")) // Mật khẩu băm BCrypt
                    .fullName("Quản Trị Viên Hệ Thống")
                    .email("admin@erp.com")
                    .phone("0987654321")
                    .status("ACTIVE")
                    .failedLoginAttempts(0)
                    .roles(roles)
                    .build();

            userRepository.save(admin);
            System.out.println(">>> ĐÃ KHỞI TẠO TÀI KHOẢN MẪU: admin / admin123 (ROLE_ADMIN) <<<");
        }

        // 3. Tạo sẵn tài khoản Quản lý kinh doanh mẫu: manager / manager123 (Phục vụ
        // test xem giá vốn S1-05)
        if (!userRepository.existsByUsername("sales_manager")) {
            Role managerRole = roleRepository.findByName(RoleName.ROLE_SALES_MANAGER)
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy Role Sales Manager"));

            Set<Role> roles = new HashSet<>();
            roles.add(managerRole);

            User manager = User.builder()
                    .username("sales_manager")
                    .password(passwordEncoder.encode("manager123"))
                    .fullName("Quản Lý Kinh Doanh")
                    .email("manager@erp.com")
                    .phone("0912345678")
                    .status("ACTIVE")
                    .failedLoginAttempts(0)
                    .roles(roles)
                    .build();

            userRepository.save(manager);
            System.out.println(">>> ĐÃ KHỞI TẠO TÀI KHOẢN MẪU: sales_manager / manager123 (ROLE_SALES_MANAGER) <<<");
        }
    }
}
