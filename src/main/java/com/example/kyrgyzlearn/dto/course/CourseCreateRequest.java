package com.example.kyrgyzlearn.dto.course;

import jakarta.validation.constraints.NotBlank;

public record CourseCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,
        Integer position,
        // true — официальный курс (только админ, хранится JSON-файлом); иначе — личный курс автора
        Boolean official
) {
}
