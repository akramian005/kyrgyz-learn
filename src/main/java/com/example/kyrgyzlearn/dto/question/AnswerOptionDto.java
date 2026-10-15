package com.example.kyrgyzlearn.dto.question;

public record AnswerOptionDto(
        Long id,
        String text,
        Integer sortOrder,
        boolean correct,
        Integer correctPosition,
        String matchText
) {
}
