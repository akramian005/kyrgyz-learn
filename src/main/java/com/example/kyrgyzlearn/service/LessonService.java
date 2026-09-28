package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.lesson.LessonCreateRequest;
import com.example.kyrgyzlearn.dto.lesson.LessonDto;
import com.example.kyrgyzlearn.dto.lesson.LessonSummaryDto;
import com.example.kyrgyzlearn.dto.lesson.LessonUpdateRequest;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonItem;
import com.example.kyrgyzlearn.mapper.LessonMapper;
import com.example.kyrgyzlearn.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseService courseService;
    private final LessonMediaCleaner lessonMediaCleaner;
    private final LessonMapper lessonMapper;
    private final CourseItemService courseItemService;

    @Transactional(readOnly = true)
    public Page<LessonSummaryDto> getLessonsByCourse(Long courseId, Pageable pageable, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        return lessonRepository.findByCourseId(courseId, pageable).map(lessonMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public LessonDto getLessonById(Long id, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(id);
        return lessonMapper.toDto(lesson);
    }

    @Transactional
    public LessonDto createLesson(LessonCreateRequest request, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(request.courseId());
        courseService.requireCanModify(course, authentication);

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(request.title());
        lesson.setDescription(request.description());
        // порядок общий с видео курса: null — в конец, иначе сдвигаем остальные
        courseItemService.insert(course, lesson, request.position());

        Lesson saved = lessonRepository.save(lesson);
        course.getLessons().add(saved);

        return lessonMapper.toDto(saved);
    }

    @Transactional
    public LessonDto updateLesson(Long id, LessonUpdateRequest request, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(id);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        if (request.title().isPresent()) {
            String newTitle = request.title().get();
            if (newTitle == null || newTitle.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson title cannot be empty");
            }
            lesson.setTitle(newTitle);
        }
        if (request.description().isPresent()) {
            lesson.setDescription(request.description().get());
        }
        if (request.position().isPresent()) {
            lesson.setPosition(request.position().get());
        }

        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long id, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(id);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        lessonMediaCleaner.deleteLessonMedia(lesson);

        lessonRepository.delete(lesson); // каскадно удалит items и options
    }

    // ---------- Порядок элементов урока ----------

    // перемещает вопрос или сообщение на новую позицию, остальные элементы сдвигаются
    @Transactional
    public LessonDto moveItem(Long lessonId, Long itemId, int position, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        List<LessonItem> items = lesson.getItems();
        LessonItem item = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Item " + itemId + " not found in lesson " + lessonId));

        items.remove(item);
        items.add(clampIndex(position, items.size()), item);
        renumber(items);

        return lessonMapper.toDto(lesson);
    }

    // ---------- Методы для QuestionService и LessonMessageService ----------
    // публичные — их вызывают другие сервисы (через Spring-прокси)

    public Lesson findLessonOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found: " + id));
    }

    // вставляет элемент в урок на позицию (null — в конец) и перенумеровывает все элементы 0..n-1
    public void insertItem(Lesson lesson, LessonItem item, Integer position) {
        List<LessonItem> items = lesson.getItems();
        int index = position == null ? items.size() : clampIndex(position, items.size());

        item.setLesson(lesson);
        items.add(index, item);
        renumber(items);
    }

    // orphanRemoval у Lesson.items удалит элемент из БД, оставшиеся позиции идут без дыр
    public void removeItem(LessonItem item) {
        List<LessonItem> items = item.getLesson().getItems();
        items.remove(item);
        renumber(items);
    }

    // ---------- Вспомогательные методы ----------

    private void renumber(List<LessonItem> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }
    }

    private int clampIndex(int position, int size) {
        return Math.max(0, Math.min(position, size));
    }
}
