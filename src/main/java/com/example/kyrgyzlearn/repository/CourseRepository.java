package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// В БД только пользовательские курсы; официальные читаются из файлов (см. OfficialContent)
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByOwnerId(Long ownerId, Sort sort);

    // поиск личных курсов всех пользователей по названию; пустая строка — все курсы.
    // type = USER отсекает старые OFFICIAL-записи, если они ещё остались в БД
    Page<Course> findByTypeAndTitleContainingIgnoreCase(CourseType type, String title, Pageable pageable);
}
