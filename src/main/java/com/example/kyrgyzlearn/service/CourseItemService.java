package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.course.CourseItemDto;
import com.example.kyrgyzlearn.dto.course.CourseItemMoveRequest;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseItem;
import com.example.kyrgyzlearn.entity.lesson.CourseItemKind;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.mapper.CourseVideoMapper;
import com.example.kyrgyzlearn.mapper.LessonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Общий порядок уроков и видео внутри курса.
// У Lesson и CourseVideo одна нумерация position, список собирается из двух таблиц и сортируется.
@Service
@RequiredArgsConstructor
public class CourseItemService {

    // при одинаковой позиции (старые данные, ручной PATCH position у урока) — сначала урок, потом по id
    private static final Comparator<CourseItem> ORDER = Comparator
            .comparing(CourseItem::getPosition, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(item -> item instanceof Lesson ? 0 : 1)
            .thenComparing(CourseItem::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CourseService courseService;
    private final LessonMapper lessonMapper;
    private final CourseVideoMapper courseVideoMapper;

    @Transactional(readOnly = true)
    public List<CourseItemDto> getItems(Long courseId, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        return toDtos(orderedItems(course));
    }

    // перемещает урок или видео на новую позицию, остальные элементы сдвигаются; возвращает новый порядок
    @Transactional
    public List<CourseItemDto> moveItem(Long courseId, CourseItemMoveRequest request, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        courseService.requireCanModify(course, authentication);

        List<CourseItem> items = orderedItems(course);
        CourseItem item = items.stream()
                .filter(i -> matches(i, request.kind(), request.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        request.kind() + " " + request.id() + " not found in course " + courseId));

        items.remove(item);
        items.add(clampIndex(request.position(), items.size()), item);
        renumber(items);

        return toDtos(items);
    }

    // Для LessonService и CourseVideoService: ставит новый элемент (ещё не добавленный в курс)
    // на позицию position (null — в конец) и перенумеровывает все элементы курса 0..n-1
    public void insert(Course course, CourseItem newItem, Integer position) {
        List<CourseItem> items = orderedItems(course);
        int index = position == null ? items.size() : clampIndex(position, items.size());

        items.add(index, newItem);
        renumber(items);
    }

    // ---------- Вспомогательные методы ----------

    private List<CourseItem> orderedItems(Course course) {
        List<CourseItem> items = new ArrayList<>(course.getLessons());
        items.addAll(course.getVideos());
        items.sort(ORDER);
        return items;
    }

    private boolean matches(CourseItem item, CourseItemKind kind, Long id) {
        boolean kindMatches = switch (kind) {
            case LESSON -> item instanceof Lesson;
            case VIDEO -> item instanceof CourseVideo;
        };
        return kindMatches && item.getId().equals(id);
    }

    private void renumber(List<CourseItem> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }
    }

    private int clampIndex(int position, int size) {
        return Math.max(0, Math.min(position, size));
    }

    private List<CourseItemDto> toDtos(List<CourseItem> items) {
        return items.stream().map(this::toDto).toList();
    }

    private CourseItemDto toDto(CourseItem item) {
        if (item instanceof Lesson lesson) {
            return lessonMapper.toSummaryDto(lesson);
        } else if (item instanceof CourseVideo video) {
            return courseVideoMapper.toDto(video);
        }
        throw new IllegalStateException("Unknown course item type: " + item.getClass());
    }
}
