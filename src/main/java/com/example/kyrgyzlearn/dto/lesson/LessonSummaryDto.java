package com.example.kyrgyzlearn.dto.lesson;

import com.example.kyrgyzlearn.dto.course.CourseItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LessonSummaryDto(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer position,
        int itemCount,
        boolean completed, // урок пройден текущим пользователем
        Long unitId
) implements CourseItemDto {
    // без @JsonProperty Jackson не видит kind(): это не компонент record и не getter
    @Override
    @JsonProperty("kind")
    public String kind() {
        return "LESSON";
    }
}
