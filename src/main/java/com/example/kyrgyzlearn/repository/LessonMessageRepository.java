package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonMessageRepository extends JpaRepository<LessonMessage, Long> {
}
