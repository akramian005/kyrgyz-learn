package com.example.kyrgyzlearn.content;

import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

// Формат файла официального курса: src/main/resources/content/official/*.json (один файл — один курс).
// Порядок уроков/видео в курсе и вопросов/сообщений в уроке — порядок в массиве items.
// id курса, урока и видео пишутся в файле явно и должны быть отрицательными (так они не пересекаются
// с id из БД) и не меняться со временем. id вопросов, сообщений и вариантов ответа раздаёт OfficialContent.
// Медиа (image, audio, video) — ключи файлов в MinIO, сами файлы заливаются в бакет отдельно.
public record OfficialCourseFile(
        Long id,
        String title,
        String description,
        Integer position,
        List<Item> items
) {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LessonFile.class, name = "LESSON"),
            @JsonSubTypes.Type(value = VideoFile.class, name = "VIDEO")
    })
    public sealed interface Item permits LessonFile, VideoFile {
    }

    public record LessonFile(Long id, String title, String description, List<LessonItem> items) implements Item {
    }

    public record VideoFile(Long id, String title, String description, String video) implements Item {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MessageFile.class, name = "MESSAGE"),
            @JsonSubTypes.Type(value = QuestionFile.class, name = "QUESTION")
    })
    public sealed interface LessonItem permits MessageFile, QuestionFile {
    }

    public record MessageFile(String text, String image, String audio) implements LessonItem {
    }

    public record QuestionFile(String text, String audio, QuestionType type, List<OptionFile> options) implements LessonItem {
    }

    // correct — для SINGLE_CHOICE / MULTIPLE_CHOICE, correctPosition — для ORDER (см. AnswerOption)
    public record OptionFile(String text, Boolean correct, Integer correctPosition) {
    }
}
