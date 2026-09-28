package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);

    // id пройденных уроков пользователя — чтобы проставить отметки в списках одним запросом
    @Query("SELECT p.lessonId FROM LessonProgress p WHERE p.user.id = :userId")
    Set<Long> findLessonIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p.courseId FROM LessonProgress p WHERE p.user.id = :userId")
    Set<Long> findCourseIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM LessonProgress p WHERE p.lessonId = :lessonId")
    void deleteByLessonId(@Param("lessonId") Long lessonId);

    @Modifying
    @Query("DELETE FROM LessonProgress p WHERE p.courseId = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
