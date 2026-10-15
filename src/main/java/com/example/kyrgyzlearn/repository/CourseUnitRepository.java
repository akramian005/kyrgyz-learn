package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.CourseUnit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseUnitRepository extends JpaRepository<CourseUnit, Long> {
}
