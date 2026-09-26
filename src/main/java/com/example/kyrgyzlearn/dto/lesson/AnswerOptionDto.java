package com.example.kyrgyzlearn.dto.lesson;

public record AnswerOptionDto(
        Long id,
        String text,
        Integer sortOrder,
        boolean correct,
        Integer correctPosition
) {
}
