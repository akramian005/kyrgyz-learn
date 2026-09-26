package com.example.kyrgyzlearn.dto.lesson;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LessonMessageDto(
        Long id,
        Integer position,
        String text,
        String imageUrl,
        String audioUrl
) implements LessonItemDto {
    // без @JsonProperty Jackson не видит kind(): это не компонент record и не getter
    @Override
    @JsonProperty("kind")
    public String kind() {
        return "MESSAGE";
    }
}
