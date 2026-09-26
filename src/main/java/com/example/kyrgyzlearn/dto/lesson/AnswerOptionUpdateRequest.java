package com.example.kyrgyzlearn.dto.lesson;

import com.example.kyrgyzlearn.dto.common.Nullable;

public record AnswerOptionUpdateRequest(
        Nullable<String> text,
        Nullable<Boolean> correct,
        Nullable<Integer> correctPosition
) {
}
