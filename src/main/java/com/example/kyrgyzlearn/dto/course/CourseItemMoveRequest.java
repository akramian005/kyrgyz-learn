package com.example.kyrgyzlearn.dto.course;

import com.example.kyrgyzlearn.entity.lesson.CourseItemKind;
import jakarta.validation.constraints.NotNull;

// у уроков и видео свои id, поэтому кроме id нужен тип элемента
public record CourseItemMoveRequest(
        @NotNull(message = "kind is required")
        CourseItemKind kind,

        @NotNull(message = "id is required")
        Long id,

        @NotNull(message = "position is required")
        Integer position
) {
}
