package com.example.kyrgyzlearn.dto.question;

import jakarta.validation.constraints.NotBlank;

public record AnswerOptionCreateRequest(
        @NotBlank(message = "Option text is required")
        String text,

        boolean correct,

        // заполняется только для вопросов типа ORDER
        Integer correctPosition
) {
}
