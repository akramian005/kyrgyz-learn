package com.example.kyrgyzlearn.dto.lesson;

import com.example.kyrgyzlearn.dto.common.Nullable;

public record LessonMessageUpdateRequest(
        Nullable<String> text
) {
}
