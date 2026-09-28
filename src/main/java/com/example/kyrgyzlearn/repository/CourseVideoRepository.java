package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {
}
