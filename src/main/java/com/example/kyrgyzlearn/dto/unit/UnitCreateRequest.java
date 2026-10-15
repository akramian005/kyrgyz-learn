package com.example.kyrgyzlearn.dto.unit;

import jakarta.validation.constraints.NotBlank;

public record UnitCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        // куда вставить юнит (0 — в начало); null — в конец
        Integer position
) {
}
