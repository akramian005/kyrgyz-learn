package com.example.kyrgyzlearn.dto.user;

public record UserProfileDto(
        Long id,
        String email,
        String name,
        String role,
        // подписанная ссылка на картинку, живёт час — клиент перезапрашивает профиль при открытии экрана
        String avatarUrl
) {
}
