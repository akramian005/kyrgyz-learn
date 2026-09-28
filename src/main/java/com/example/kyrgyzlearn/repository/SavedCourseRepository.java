package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    List<SavedCourse> findByUserIdOrderBySavedAtDesc(Long userId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // id сохранённых курсов пользователя — чтобы проставить флаг saved в списке курсов одним запросом
    @Query("SELECT s.course.id FROM SavedCourse s WHERE s.user.id = :userId")
    Set<Long> findCourseIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM SavedCourse s WHERE s.user.id = :userId AND s.course.id = :courseId")
    void deleteByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // перед удалением курса — иначе внешний ключ не даст удалить
    @Modifying
    @Query("DELETE FROM SavedCourse s WHERE s.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
