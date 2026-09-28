package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.course.CourseDto;
import com.example.kyrgyzlearn.service.SavedCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Сохранённые курсы текущего пользователя (доступ — только авторизованным, см. anyRequest в SecurityConfig)
@RestController
@RequestMapping("/api/saved-courses")
@RequiredArgsConstructor
public class SavedCourseController {

    private final SavedCourseService savedCourseService;

    @GetMapping
    public ResponseEntity<List<CourseDto>> getSavedCourses(Authentication authentication) {
        return ResponseEntity.ok(savedCourseService.getSavedCourses(authentication));
    }

    // PUT, а не POST: сохранить уже сохранённый курс — не ошибка
    @PutMapping("/{courseId}")
    public ResponseEntity<Void> saveCourse(@PathVariable Long courseId, Authentication authentication) {
        savedCourseService.saveCourse(courseId, authentication);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> unsaveCourse(@PathVariable Long courseId, Authentication authentication) {
        savedCourseService.unsaveCourse(courseId, authentication);
        return ResponseEntity.noContent().build();
    }
}
