package com.example.kyrgyzlearn.dto.question;

import com.example.kyrgyzlearn.dto.common.Nullable;

public record AnswerOptionUpdateRequest(
        Nullable<String> text,
        Nullable<Boolean> correct,
        Nullable<Integer> correctPosition,
        Nullable<String> matchText
) {
}
