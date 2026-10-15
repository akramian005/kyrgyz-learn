package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.unit.UnitCreateRequest;
import com.example.kyrgyzlearn.dto.unit.UnitDto;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseItem;
import com.example.kyrgyzlearn.entity.lesson.CourseUnit;
import com.example.kyrgyzlearn.repository.CourseUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

// Юниты курса: список с уроками и видео, создание, переименование, порядок и удаление
@Service
@RequiredArgsConstructor
public class CourseUnitService {

    private final CourseService courseService;
    private final CourseItemService courseItemService;
    private final ProgressService progressService;
    private final CourseUnitRepository courseUnitRepository;

    @Transactional(readOnly = true)
    public List<UnitDto> getUnits(Long courseId, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        ProgressService.Completed completed = progressService.completed(authentication);
        Map<Long, List<CourseItem>> byUnit = courseItemService.itemsByUnit(course);
        return course.getUnits().stream()
                .map(unit -> new UnitDto(unit.getId(), course.getId(), unit.getTitle(), unit.getPosition(),
                        courseItemService.toDtos(byUnit.get(unit.getId()), completed)))
                .toList();
    }

    @Transactional
    public UnitDto createUnit(Long courseId, UnitCreateRequest request, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        courseService.requireCanModify(course, authentication);

        CourseUnit unit = new CourseUnit();
        unit.setCourse(course);
        unit.setTitle(request.title().trim());
        List<CourseUnit> units = course.getUnits();
        units.add(request.position() == null ? units.size() : clampIndex(request.position(), units.size()), unit);
        renumberUnits(course);

        // id нужен сразу: официальному курсу его раздаст OfficialContent при сохранении, личному — БД
        if (!courseService.saveIfOfficial(course)) {
            courseUnitRepository.save(unit);
        }
        // порядок юнитов изменился — сквозные позиции уроков и видео тоже
        courseItemService.renumber(courseItemService.itemsByUnit(course));
        courseService.saveIfOfficial(course);
        return new UnitDto(unit.getId(), course.getId(), unit.getTitle(), unit.getPosition(), List.of());
    }

    @Transactional
    public void renameUnit(Long courseId, Long unitId, String title, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        courseService.requireCanModify(course, authentication);

        courseItemService.requireUnit(course, unitId).setTitle(title.trim());
        courseService.saveIfOfficial(course); // личный курс сохранит JPA сам (dirty checking)
    }

    // переставляет юнит на новое место; уроки и видео переезжают вместе с ним
    @Transactional
    public List<UnitDto> moveUnit(Long courseId, Long unitId, int position, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        courseService.requireCanModify(course, authentication);

        CourseUnit unit = courseItemService.requireUnit(course, unitId);
        List<CourseUnit> units = course.getUnits();
        units.remove(unit);
        units.add(clampIndex(position, units.size()), unit);
        renumberUnits(course);
        courseItemService.renumber(courseItemService.itemsByUnit(course));
        courseService.saveIfOfficial(course);

        return getUnits(courseId, authentication);
    }

    // удалить можно только пустой юнит — уроки и видео сначала удаляют или переносят в другой юнит
    @Transactional
    public void deleteUnit(Long courseId, Long unitId, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        courseService.requireCanModify(course, authentication);

        CourseUnit unit = courseItemService.requireUnit(course, unitId);
        if (!courseItemService.itemsByUnit(course).get(unitId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit is not empty: delete or move its lessons and videos first");
        }
        course.getUnits().remove(unit);
        renumberUnits(course);
        if (!courseService.saveIfOfficial(course)) {
            courseUnitRepository.delete(unit);
        }
    }

    private void renumberUnits(Course course) {
        List<CourseUnit> units = course.getUnits();
        for (int i = 0; i < units.size(); i++) {
            units.get(i).setPosition(i);
        }
    }

    private int clampIndex(int position, int size) {
        return Math.max(0, Math.min(position, size));
    }
}
