package com.example.kyrgyzlearn.dto.message;

import jakarta.validation.constraints.NotBlank;

public record LessonMessageCreateRequest(
        @NotBlank(message = "Message text is required")
        String text,
        // куда вставить сообщение внутри урока (0 — в начало); null — в конец
        Integer position
) {
}
