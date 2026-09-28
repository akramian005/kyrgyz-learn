package com.example.kyrgyzlearn.dto.auth;

public record AuthResponse(
        String token,
        String email,
        String name,
        String role
) {
}
