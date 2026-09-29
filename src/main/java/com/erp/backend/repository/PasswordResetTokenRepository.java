package com.erp.backend.repository;

import com.erp.backend.entity.PasswordResetToken;
import com.erp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    // Xoá hoặc vô hiệu hoá các token cũ của user nếu có yêu cầu mới
    void deleteByUser(User user);
}
