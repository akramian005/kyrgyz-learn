package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonItem;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Удаляет файлы уроков из MinIO. Вынесено отдельно, чтобы им пользовались
// и LessonService, и CourseService без циклической зависимости между ними.
@Component
@RequiredArgsConstructor
public class LessonMediaCleaner {

    private final FileStorageService fileStorageService;

    public void deleteLessonMedia(Lesson lesson) {
        for (LessonItem item : lesson.getItems()) {
            deleteItemMedia(item);
        }
    }

    public void deleteItemMedia(LessonItem item) {
        if (item instanceof Question q) {
            deleteIfPresent(q.getAudioObjectKey());
        } else if (item instanceof LessonMessage m) {
            deleteIfPresent(m.getImageObjectKey());
            deleteIfPresent(m.getAudioObjectKey());
        }
    }

    private void deleteIfPresent(String objectKey) {
        if (objectKey != null) {
            fileStorageService.delete(objectKey);
        }
    }
}
