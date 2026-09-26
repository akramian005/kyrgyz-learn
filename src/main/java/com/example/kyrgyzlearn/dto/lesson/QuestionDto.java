package com.example.kyrgyzlearn.dto.lesson;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    // без @JsonProperty Jackson не видит kind(): это не компонент record и не getter
    @Override
    @JsonProperty("kind")
    public String kind() {
        return "QUESTION";
    }
}
