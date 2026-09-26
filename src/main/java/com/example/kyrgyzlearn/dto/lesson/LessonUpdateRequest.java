package com.example.kyrgyzlearn.dto.lesson;

import com.example.kyrgyzlearn.dto.common.Nullable;

public record LessonUpdateRequest(
        Nullable<String> title,
        Nullable<String> description,
        Nullable<Integer> position
) {
}
