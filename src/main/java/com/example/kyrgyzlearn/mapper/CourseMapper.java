package com.example.kyrgyzlearn.mapper;

import com.example.kyrgyzlearn.dto.course.CourseDto;
import com.example.kyrgyzlearn.entity.lesson.Course;
import org.springframework.stereotype.Component;

// Вынесено отдельно, чтобы им пользовались CourseService и SavedCourseService
@Component
public class CourseMapper {

    public CourseDto toDto(Course course, boolean saved) {
        return new CourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPosition(),
                course.getType(),
                course.getOwner() != null ? course.getOwner().getId() : null,
                course.getOwner() != null ? course.getOwner().getName() : null,
                course.getLessons().size(),
                course.getVideos().size(),
                saved
        );
    }
}
