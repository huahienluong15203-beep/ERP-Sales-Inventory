package com.erp.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {

    @GetMapping("/me")
    public String getCurrentUserProfile() {
        return "User Profile Service - Ready";
    }
}
