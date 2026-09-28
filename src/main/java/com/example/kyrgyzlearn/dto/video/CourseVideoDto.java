package com.example.kyrgyzlearn.dto.video;

import com.example.kyrgyzlearn.dto.course.CourseItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CourseVideoDto(
        Long id,
        Long courseId,
        Integer position,
        String title,
        String description,
        // подписанная ссылка на файл, живёт час; null — файл ещё не загружен
        String videoUrl,
        boolean watched // текущий пользователь уже открывал это видео
) implements CourseItemDto {
    // без @JsonProperty Jackson не видит kind(): это не компонент record и не getter
    @Override
    @JsonProperty("kind")
    public String kind() {
        return "VIDEO";
    }
}
