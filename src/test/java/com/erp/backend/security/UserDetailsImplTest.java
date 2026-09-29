package com.erp.backend.security;

import com.erp.backend.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDetailsImplTest {

    @Test
    void lockedAccountCannotAuthenticateWithAnExistingJwt() {
        User user = User.builder()
                .username("former-employee")
                .status("LOCKED")
                .lockReason("Nghỉ việc")
                .build();

        assertFalse(UserDetailsImpl.build(user).isAccountNonLocked());
    }

    @Test
    void activeAccountRemainsUnlocked() {
        User user = User.builder()
                .username("active-employee")
                .status("ACTIVE")
                .build();

        assertTrue(UserDetailsImpl.build(user).isAccountNonLocked());
    }
}