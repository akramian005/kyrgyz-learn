package com.example.kyrgyzlearn.dto.course;

import com.example.kyrgyzlearn.dto.common.Nullable;

public record CourseUpdateRequest(
        Nullable<String> title,
        Nullable<String> description,
        Nullable<Integer> position
) {
}
