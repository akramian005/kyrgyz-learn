package com.example.kyrgyzlearn.dto.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LessonCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,
        Integer position,

        @NotNull(message = "courseId is required")
        Long courseId
) {
}
