package com.example.kyrgyzlearn.content;

import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

// Формат файла официального курса: official-content/*.json (один файл — один курс).
// Порядок уроков/видео в курсе и вопросов/сообщений в уроке — порядок в массиве items.
// id курса, урока и видео пишутся в файле явно и должны быть отрицательными (так они не пересекаются
// с id из БД) и не меняться со временем. id вопросов, сообщений и вариантов ответа раздаёт OfficialContent.
// Медиа (image, audio, video) — пути к файлам относительно official-content.media-dir.
// Файлы читаются при старте и перезаписываются, когда админ правит курс в приложении (пустые поля не пишутся).
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LessonFile(Long id, String title, String description, List<LessonItem> items) implements Item {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VideoFile(Long id, String title, String description, String video) implements Item {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MessageFile.class, name = "MESSAGE"),
            @JsonSubTypes.Type(value = QuestionFile.class, name = "QUESTION")
    })
    public sealed interface LessonItem permits MessageFile, QuestionFile {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageFile(String text, String image, String audio) implements LessonItem {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionFile(String text, String audio, QuestionType type, List<OptionFile> options) implements LessonItem {
    }

    // correct — для SINGLE_CHOICE / MULTIPLE_CHOICE, correctPosition — для ORDER (см. AnswerOption)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OptionFile(String text, Boolean correct, Integer correctPosition) {
    }
}
