package com.erp.backend.controller;

import com.erp.backend.dto.LockUserRequest;
import com.erp.backend.dto.UserAccountResponse;
import com.erp.backend.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public List<UserAccountResponse> getUsers() {
        return userManagementService.getUsers();
    }

    @PatchMapping("/{userId}/lock")
    public UserAccountResponse lockUser(@PathVariable Long userId, @RequestBody(required = false) LockUserRequest request) {
        return userManagementService.lockUser(userId, request);
    }

    @PatchMapping("/{userId}/unlock")
    public UserAccountResponse unlockUser(@PathVariable Long userId) {
        return userManagementService.unlockUser(userId);
    }
}