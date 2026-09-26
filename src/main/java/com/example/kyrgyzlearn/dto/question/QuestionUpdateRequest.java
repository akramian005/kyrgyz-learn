package com.example.kyrgyzlearn.dto.question;

import com.example.kyrgyzlearn.dto.common.Nullable;
import com.example.kyrgyzlearn.entity.lesson.QuestionType;

public record QuestionUpdateRequest(
        Nullable<String> text,
        Nullable<QuestionType> type
) {
}
