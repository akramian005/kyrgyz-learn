package com.example.kyrgyzlearn.dto.lesson;

import jakarta.validation.constraints.NotNull;

// у урока нет обязательного названия — на экране курса он «Урок N» своего юнита
public record LessonCreateRequest(
        String title,

        String description,
        // место внутри юнита (0 — в начало); null — в конец юнита
        Integer position,

        @NotNull(message = "courseId is required")
        Long courseId,

        // юнит урока; null — последний юнит курса
        Long unitId
) {
}
