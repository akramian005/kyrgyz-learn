package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // Видно: все OFFICIAL-курсы + свои USER-курсы (userId может быть null для анонима —
    // тогда вторая часть условия просто не сработает, останутся только OFFICIAL).
    @Query("SELECT c FROM Course c WHERE c.type = 'OFFICIAL' " +
            "OR (:userId IS NOT NULL AND c.type = 'USER' AND c.owner.id = :userId)")
    Page<Course> findVisibleTo(@Param("userId") Long userId, Pageable pageable);
}
