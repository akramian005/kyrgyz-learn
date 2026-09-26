package com.example.kyrgyzlearn.dto.question;

import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionCreateRequest(
        @NotBlank(message = "Question text is required")
        String text,

        @NotNull(message = "Question type is required")
        QuestionType type,

        List<AnswerOptionCreateRequest> options,
        // куда вставить вопрос внутри урока (0 — в начало); null — в конец
        Integer position
) {
}
