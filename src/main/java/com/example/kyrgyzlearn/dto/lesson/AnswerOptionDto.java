package com.example.kyrgyzlearn.dto.lesson;

public record AnswerOptionDto(
        Long id,
        String text,
        String imageUrl,
        Integer sortOrder,
        boolean correct,
        Integer correctPosition
) {
}
