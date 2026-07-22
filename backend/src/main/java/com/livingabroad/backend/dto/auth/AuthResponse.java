package com.livingabroad.backend.dto.auth;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String refreshToken,
    UserSummary user
) {
    public record UserSummary(Long id, String name, String email) {
    }
}
