package com.example.kyrgyzlearn.dto.lesson;

import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionCreateRequest(
        @NotBlank(message = "Question text is required")
        String text,

        @NotNull(message = "Question type is required")
        QuestionType type,

        List<AnswerOptionCreateRequest> options
) {
}
