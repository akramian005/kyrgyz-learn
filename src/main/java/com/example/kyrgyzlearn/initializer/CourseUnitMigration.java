package com.example.kyrgyzlearn.initializer;

import com.example.kyrgyzlearn.content.OfficialContent;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseItem;
import com.example.kyrgyzlearn.entity.lesson.CourseUnit;
import com.example.kyrgyzlearn.repository.CourseRepository;
import com.example.kyrgyzlearn.repository.CourseUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// Курсы в БД, созданные до появления юнитов: у их уроков и видео нет unitId.
// При старте у каждого такого курса появляется «Юнит 1» (если юнитов нет), и все такие уроки и видео
// попадают в первый юнит курса. Повторный запуск ничего не меняет.
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseUnitMigration implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final CourseUnitRepository courseUnitRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migrated = 0;
        for (Course course : courseRepository.findAll()) {
            List<CourseItem> withoutUnit = new ArrayList<>();
            course.getLessons().stream().filter(l -> l.getUnitId() == null).forEach(withoutUnit::add);
            course.getVideos().stream().filter(v -> v.getUnitId() == null).forEach(withoutUnit::add);
            if (withoutUnit.isEmpty() && !course.getUnits().isEmpty()) {
                continue;
            }

            if (course.getUnits().isEmpty()) {
                CourseUnit unit = new CourseUnit();
                unit.setTitle(OfficialContent.DEFAULT_UNIT_TITLE);
                unit.setPosition(0);
                unit.setCourse(course);
                course.getUnits().add(courseUnitRepository.save(unit));
            }
            Long firstUnitId = course.getUnits().getFirst().getId();
            withoutUnit.forEach(item -> item.setUnitId(firstUnitId));
            migrated++;
        }
        if (migrated > 0) {
            log.info("Moved lessons and videos of {} courses into units", migrated);
        }
    }
}
