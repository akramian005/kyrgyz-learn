package com.example.kyrgyzlearn.dto.course;

import jakarta.validation.constraints.NotBlank;

public record CourseCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,
        Integer position
) {
}
