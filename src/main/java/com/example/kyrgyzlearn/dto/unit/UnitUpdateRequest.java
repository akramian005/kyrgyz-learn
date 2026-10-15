package com.example.kyrgyzlearn.dto.unit;

import jakarta.validation.constraints.NotBlank;

public record UnitUpdateRequest(
        @NotBlank(message = "Title is required")
        String title
) {
}
