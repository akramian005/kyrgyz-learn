package com.example.kyrgyzlearn.dto.course;

import com.example.kyrgyzlearn.entity.lesson.CourseType;

public record CourseDto(
        Long id,
        String title,
        String description,
        Integer position,
        CourseType type,
        Long ownerId, // null для OFFICIAL
        String ownerName, // имя создателя; null для OFFICIAL и у пользователей без имени
        int lessonCount,
        int videoCount,
        boolean saved // курс в сохранённых у текущего пользователя
) {
}
