package com.erp.backend.repository;

import com.erp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email); // <-- THÊM DÒNG NÀY

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    // S1-08: Kiểm tra trùng khi tạo / sửa tài khoản
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
