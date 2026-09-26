package com.example.kyrgyzlearn.dto.message;

import com.example.kyrgyzlearn.dto.common.Nullable;

public record LessonMessageUpdateRequest(
        Nullable<String> text
) {
}
