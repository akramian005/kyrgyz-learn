package com.example.kyrgyzlearn.dto.lesson;

import java.util.List;

public record LessonDto(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer position,
        List<LessonItemDto> items
) {
}
