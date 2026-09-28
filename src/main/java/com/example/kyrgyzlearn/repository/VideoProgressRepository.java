package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.VideoProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface VideoProgressRepository extends JpaRepository<VideoProgress, Long> {

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    @Query("SELECT p.videoId FROM VideoProgress p WHERE p.user.id = :userId")
    Set<Long> findVideoIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p.courseId FROM VideoProgress p WHERE p.user.id = :userId")
    Set<Long> findCourseIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM VideoProgress p WHERE p.videoId = :videoId")
    void deleteByVideoId(@Param("videoId") Long videoId);

    @Modifying
    @Query("DELETE FROM VideoProgress p WHERE p.courseId = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
