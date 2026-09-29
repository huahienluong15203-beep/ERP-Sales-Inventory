package com.erp.backend.service;

import com.erp.backend.dto.LockUserRequest;
import com.erp.backend.dto.UserAccountResponse;
import com.erp.backend.entity.RoleName;
import com.erp.backend.entity.User;
import com.erp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserAccountResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserAccountResponse lockUser(Long userId, LockUserRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do khóa tài khoản.");
        }

        String reason = request.reason().trim();
        if (reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do khóa không được vượt quá 500 ký tự.");
        }

        User user = findUser(userId);
        user.setStatus("LOCKED");
        user.setLockUntil(null);
        user.setLockReason(reason);
        user.setFailedLoginAttempts(0);

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserAccountResponse unlockUser(Long userId) {
        User user = findUser(userId);
        user.setStatus("ACTIVE");
        user.setLockUntil(null);
        user.setFailedLoginAttempts(0);

        return toResponse(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
    }

    private UserAccountResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();
        boolean salesEmployee = user.getRoles().stream()
            .map(role -> role.getName())
            .anyMatch(roleName -> roleName == RoleName.ROLE_SALES_REP || roleName == RoleName.ROLE_SALES_MANAGER);
        boolean handoverRequired = salesEmployee
            && "LOCKED".equalsIgnoreCase(user.getStatus())
            && user.getLockUntil() == null;

        return new UserAccountResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getStatus(),
                user.getLockReason(),
                user.getLockUntil(),
                roles,
                handoverRequired);
    }
}