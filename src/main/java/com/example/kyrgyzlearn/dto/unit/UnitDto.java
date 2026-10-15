package com.example.kyrgyzlearn.dto.unit;

import com.example.kyrgyzlearn.dto.course.CourseItemDto;

import java.util.List;

// Юнит курса с его уроками и видео по порядку (отметки прохождения — для текущего пользователя)
public record UnitDto(
        Long id,
        Long courseId,
        String title,
        Integer position,
        List<CourseItemDto> items
) {
}
