package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonItem;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Удаляет файлы уроков и видео курса (MinIO или папка официальных курсов — решает CourseMedia).
// Вынесено отдельно, чтобы им пользовались LessonService, CourseVideoService и CourseService
// без циклической зависимости между ними.
@Component
@RequiredArgsConstructor
public class LessonMediaCleaner {

    private final CourseMedia courseMedia;

    public void deleteLessonMedia(Lesson lesson) {
        for (LessonItem item : lesson.getItems()) {
            deleteItemMedia(item);
        }
    }

    public void deleteItemMedia(LessonItem item) {
        var course = item.getLesson().getCourse();
        if (item instanceof Question q) {
            courseMedia.delete(course, q.getAudioObjectKey());
        } else if (item instanceof LessonMessage m) {
            courseMedia.delete(course, m.getImageObjectKey());
            courseMedia.delete(course, m.getAudioObjectKey());
        }
    }

    public void deleteVideoMedia(CourseVideo video) {
        courseMedia.delete(video.getCourse(), video.getVideoObjectKey());
    }
}
