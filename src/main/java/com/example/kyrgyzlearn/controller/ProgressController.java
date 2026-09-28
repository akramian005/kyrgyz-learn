package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.progress.ProgressDto;
import com.example.kyrgyzlearn.service.CourseVideoService;
import com.example.kyrgyzlearn.service.LessonService;
import com.example.kyrgyzlearn.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Прогресс текущего пользователя (доступ — только авторизованным, см. anyRequest в SecurityConfig)
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final LessonService lessonService;
    private final CourseVideoService courseVideoService;

    @GetMapping
    public ResponseEntity<ProgressDto> getProgress(Authentication authentication) {
        return ResponseEntity.ok(progressService.getProgress(authentication));
    }

    // PUT, а не POST: повторно пройти уже пройденный урок — не ошибка
    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<Void> completeLesson(@PathVariable Long lessonId, Authentication authentication) {
        lessonService.completeLesson(lessonId, authentication);
        return ResponseEntity.noContent().build();
    }

    // видео открыли — считаем просмотренным; повторно — не ошибка
    @PutMapping("/courses/{courseId}/videos/{videoId}")
    public ResponseEntity<Void> watchVideo(@PathVariable Long courseId, @PathVariable Long videoId, Authentication authentication) {
        courseVideoService.watchVideo(courseId, videoId, authentication);
        return ResponseEntity.noContent().build();
    }
}
