package com.example.kyrgyzlearn.dto.lesson;

public record LessonSummaryDto(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer position,
        int itemCount
) {
}
