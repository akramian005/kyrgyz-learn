package com.example.kyrgyzlearn.dto.course;

import com.example.kyrgyzlearn.entity.lesson.CourseType;

public record CourseDto(
        Long id,
        String title,
        String description,
        Integer position,
        CourseType type,
        Long ownerId, // null для OFFICIAL
        int lessonCount,
        int videoCount
) {
}
