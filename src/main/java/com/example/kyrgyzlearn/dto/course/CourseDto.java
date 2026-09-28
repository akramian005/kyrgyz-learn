package com.example.kyrgyzlearn.dto.course;

import com.example.kyrgyzlearn.entity.lesson.CourseType;

public record CourseDto(
        Long id,
        String title,
        String description,
        String imageUrl, // картинка курса; null — картинки нет
        Integer position,
        CourseType type,
        Long ownerId, // null для OFFICIAL
        String ownerName, // имя создателя; null для OFFICIAL и у пользователей без имени
        int lessonCount,
        int videoCount,
        int completedItemCount, // сколько уроков и видео курса прошёл (посмотрел) текущий пользователь
        int totalItemCount, // сколько всего уроков и видео идут в прогресс (без пустых уроков и видео без файла)
        boolean saved // курс в сохранённых у текущего пользователя
) {
}
