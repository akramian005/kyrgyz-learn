package com.example.kyrgyzlearn.repository;

import com.example.kyrgyzlearn.entity.lesson.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
