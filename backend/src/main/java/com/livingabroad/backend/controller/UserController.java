package com.livingabroad.backend.controller;

import com.livingabroad.backend.dto.profile.UserProfileRequest;
import com.livingabroad.backend.dto.profile.UserProfileResponse;
import com.livingabroad.backend.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UserProfileResponse response = userProfileService.getMyProfile(Long.valueOf(jwt.getSubject()));
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> saveProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UserProfileRequest request
    ) {
        return ResponseEntity.ok(userProfileService.saveMyProfile(Long.valueOf(jwt.getSubject()), request));
    }
}
