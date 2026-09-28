package com.example.kyrgyzlearn.dto.course;

// Элемент курса в общем списке: урок (LessonSummaryDto) или видео (CourseVideoDto).
// Поле "kind" говорит фронтенду, что именно пришло.
public interface CourseItemDto {
    Long id();
    Integer position();
    String kind();
}
