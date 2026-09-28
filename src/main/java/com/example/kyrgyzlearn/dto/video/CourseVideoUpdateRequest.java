package com.example.kyrgyzlearn.dto.video;

import com.example.kyrgyzlearn.dto.common.Nullable;

public record CourseVideoUpdateRequest(
        Nullable<String> title,
        Nullable<String> description
) {
}
