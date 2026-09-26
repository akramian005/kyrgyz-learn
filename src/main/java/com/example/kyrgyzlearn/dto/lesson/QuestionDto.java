package com.example.kyrgyzlearn.dto.lesson;

import com.example.kyrgyzlearn.entity.lesson.QuestionType;

import java.util.List;

public record QuestionDto(
        Long id,
        Integer position,
        String text,
        String audioUrl,
        QuestionType type,
        List<AnswerOptionDto> options
) implements LessonItemDto {
    @Override
    public String kind() {
        return "QUESTION";
    }
}
