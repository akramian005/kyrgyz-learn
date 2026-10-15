package com.example.kyrgyzlearn.dto.progress;

// Сводка для профиля: сколько уроков и курсов пройдено (видео не считаются) и сколько за них XP
public record ProgressDto(
        int completedLessons,
        int completedCourses,
        int xp
) {
}
