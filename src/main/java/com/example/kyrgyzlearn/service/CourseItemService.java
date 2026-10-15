package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.course.CourseItemDto;
import com.example.kyrgyzlearn.dto.course.CourseItemMoveRequest;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseItem;
import com.example.kyrgyzlearn.entity.lesson.CourseItemKind;
import com.example.kyrgyzlearn.entity.lesson.CourseUnit;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Порядок уроков и видео внутри курса. Они разложены по юнитам (unitId), у Lesson и CourseVideo
// одна нумерация position на весь курс: сначала все элементы первого юнита, потом второго и т.д.
// Список собирается из двух таблиц и сортируется: по порядку юнита, потом по position.
@Service
@RequiredArgsConstructor
public class CourseItemService {

    // при одинаковой позиции (старые данные, ручной PATCH position у урока) — сначала урок, потом по id
    private static final Comparator<CourseItem> POSITION_ORDER = Comparator
            .comparing(CourseItem::getPosition, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(item -> item instanceof Lesson ? 0 : 1)
            .thenComparing(CourseItem::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CourseService courseService;
    private final LessonMapper lessonMapper;
    private final CourseVideoMapper courseVideoMapper;
    private final ProgressService progressService;

    @Transactional(readOnly = true)
    public List<CourseItemDto> getItems(Long courseId, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        return toDtos(orderedItems(course), progressService.completed(authentication));
    }

    // перемещает урок или видео на позицию внутри юнита (можно в другой юнит), остальные элементы сдвигаются;
    // возвращает новый порядок
    @Transactional
    public List<CourseItemDto> moveItem(Long courseId, CourseItemMoveRequest request, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        courseService.requireCanModify(course, authentication);

        Map<Long, List<CourseItem>> byUnit = itemsByUnit(course);
        CourseItem item = byUnit.values().stream()
                .flatMap(List::stream)
                .filter(i -> matches(i, request.kind(), request.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        request.kind() + " " + request.id() + " not found in course " + courseId));

        Long targetUnitId = request.unitId() != null ? requireUnit(course, request.unitId()).getId() : item.getUnitId();
        byUnit.get(item.getUnitId()).remove(item);
        item.setUnitId(targetUnitId);
        List<CourseItem> target = byUnit.get(targetUnitId);
        target.add(clampIndex(request.position(), target.size()), item);
        renumber(byUnit);
        courseService.saveIfOfficial(course); // личный курс сохранит JPA сам (dirty checking)

        return toDtos(orderedItems(course), progressService.completed(authentication));
    }

    // Для LessonService и CourseVideoService: ставит новый элемент (ещё не добавленный в курс) в юнит unitId
    // (null — в последний) на позицию position внутри юнита (null — в конец) и перенумеровывает элементы курса
    public void insert(Course course, CourseItem newItem, Long unitId, Integer position) {
        CourseUnit unit = unitId != null ? requireUnit(course, unitId) : lastUnit(course);
        Map<Long, List<CourseItem>> byUnit = itemsByUnit(course);
        List<CourseItem> items = byUnit.get(unit.getId());
        int index = position == null ? items.size() : clampIndex(position, items.size());

        newItem.setUnitId(unit.getId());
        items.add(index, newItem);
        renumber(byUnit);
    }

    // ---------- Для CourseUnitService ----------

    // уроки и видео по юнитам в порядке юнитов; элементы без юнита (или из удалённого юнита) — под ключом null в начале
    public Map<Long, List<CourseItem>> itemsByUnit(Course course) {
        Map<Long, List<CourseItem>> byUnit = new LinkedHashMap<>();
        List<CourseItem> orphans = new ArrayList<>();
        byUnit.put(null, orphans);
        for (CourseUnit unit : course.getUnits()) {
            byUnit.put(unit.getId(), new ArrayList<>());
        }

        List<CourseItem> all = new ArrayList<>(course.getLessons());
        all.addAll(course.getVideos());
        all.sort(POSITION_ORDER);
        for (CourseItem item : all) {
            byUnit.getOrDefault(item.getUnitId(), orphans).add(item);
        }
        return byUnit;
    }

    // сквозная нумерация 0..n-1 по порядку юнитов
    public void renumber(Map<Long, List<CourseItem>> byUnit) {
        int position = 0;
        for (List<CourseItem> items : byUnit.values()) {
            for (CourseItem item : items) {
                item.setPosition(position++);
            }
        }
    }

    public CourseUnit requireUnit(Course course, Long unitId) {
        return course.getUnits().stream()
                .filter(u -> u.getId() != null && u.getId().equals(unitId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unit " + unitId + " not found in course " + course.getId()));
    }

    public List<CourseItemDto> toDtos(List<CourseItem> items, ProgressService.Completed completed) {
        return items.stream().map(item -> toDto(item, completed)).toList();
    }

    // ---------- Вспомогательные методы ----------

    private List<CourseItem> orderedItems(Course course) {
        return itemsByUnit(course).values().stream().flatMap(List::stream).toList();
    }

    private CourseUnit lastUnit(Course course) {
        List<CourseUnit> units = course.getUnits();
        if (units.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course has no units: create a unit first");
        }
        return units.getLast();
    }

    private boolean matches(CourseItem item, CourseItemKind kind, Long id) {
        boolean kindMatches = switch (kind) {
            case LESSON -> item instanceof Lesson;
            case VIDEO -> item instanceof CourseVideo;
        };
        return kindMatches && item.getId().equals(id);
    }

    private int clampIndex(int position, int size) {
        return Math.max(0, Math.min(position, size));
    }

    private CourseItemDto toDto(CourseItem item, ProgressService.Completed completed) {
        if (item instanceof Lesson lesson) {
            return lessonMapper.toSummaryDto(lesson, completed.lessonIds().contains(lesson.getId()));
        } else if (item instanceof CourseVideo video) {
            return courseVideoMapper.toDto(video, completed.videoIds().contains(video.getId()));
        }
        throw new IllegalStateException("Unknown course item type: " + item.getClass());
    }
}
