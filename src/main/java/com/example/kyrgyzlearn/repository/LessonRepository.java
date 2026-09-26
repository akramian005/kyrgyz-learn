package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Page<Lesson> findByCourseId(Long courseId, Pageable pageable);
}
