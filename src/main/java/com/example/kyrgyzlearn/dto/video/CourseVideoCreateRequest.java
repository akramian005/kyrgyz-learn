package com.example.kyrgyzlearn.dto.video;

import jakarta.validation.constraints.NotBlank;

public record CourseVideoCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        // куда вставить видео среди уроков и других видео (0 — в начало); null — в конец
        Integer position
) {
}
