package com.example.kyrgyzlearn.dto.lesson;

public record LessonMessageDto(
        Long id,
        Integer position,
        String text,
        String imageUrl,
        String audioUrl
) implements LessonItemDto {
    @Override
    public String kind() {
        return "MESSAGE";
    }
}
