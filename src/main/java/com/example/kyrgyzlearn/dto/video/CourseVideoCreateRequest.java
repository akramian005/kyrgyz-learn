package com.example.kyrgyzlearn.dto.video;

import jakarta.validation.constraints.NotBlank;

public record CourseVideoCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        // куда вставить видео среди уроков и других видео юнита (0 — в начало); null — в конец юнита
        Integer position,

        // юнит видео; null — последний юнит курса
        Long unitId
) {
}
