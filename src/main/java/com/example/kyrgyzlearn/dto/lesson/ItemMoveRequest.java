package com.example.kyrgyzlearn.dto.lesson;

import jakarta.validation.constraints.NotNull;

public record ItemMoveRequest(
        @NotNull(message = "position is required")
        Integer position
) {
}
