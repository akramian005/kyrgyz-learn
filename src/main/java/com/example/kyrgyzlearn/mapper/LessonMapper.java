package com.example.kyrgyzlearn.mapper;

import com.example.kyrgyzlearn.dto.lesson.LessonDto;
import com.example.kyrgyzlearn.dto.lesson.LessonItemDto;
import com.example.kyrgyzlearn.dto.lesson.LessonSummaryDto;
import com.example.kyrgyzlearn.dto.message.LessonMessageDto;
import com.example.kyrgyzlearn.dto.question.AnswerOptionDto;
import com.example.kyrgyzlearn.dto.question.QuestionDto;
import com.example.kyrgyzlearn.entity.lesson.AnswerOption;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonItem;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// Маппинг урока и его элементов в DTO. Вынесено отдельно, чтобы им пользовались
// LessonService, QuestionService и LessonMessageService без циклической зависимости.
@Component
@RequiredArgsConstructor
public class LessonMapper {

    private final MediaUrls mediaUrls;

    public LessonSummaryDto toSummaryDto(Lesson lesson, boolean completed) {
        return new LessonSummaryDto(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getPosition(),
                lesson.getItems().size(),
                completed,
                lesson.getUnitId()
        );
    }

    public LessonDto toDto(Lesson lesson) {
        List<LessonItemDto> items = lesson.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new LessonDto(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getPosition(),
                items
        );
    }

    public LessonItemDto toItemDto(LessonItem item) {
        if (item instanceof Question q) {
            return toQuestionDto(q);
        } else if (item instanceof LessonMessage m) {
            return toMessageDto(m);
        }
        throw new IllegalStateException("Unknown lesson item type: " + item.getClass());
    }

    public QuestionDto toQuestionDto(Question question) {
        List<AnswerOptionDto> options = question.getOptions().stream()
                .map(this::toAnswerOptionDto)
                .toList();

        return new QuestionDto(
                question.getId(),
                question.getPosition(),
                question.getText(),
                mediaUrls.url(question.getAudioObjectKey(), question.getLesson().getCourse()),
                question.getType(),
                options
        );
    }

    public LessonMessageDto toMessageDto(LessonMessage message) {
        return new LessonMessageDto(
                message.getId(),
                message.getPosition(),
                message.getText(),
                mediaUrls.url(message.getImageObjectKey(), message.getLesson().getCourse()),
                mediaUrls.url(message.getAudioObjectKey(), message.getLesson().getCourse())
        );
    }

    public AnswerOptionDto toAnswerOptionDto(AnswerOption option) {
        return new AnswerOptionDto(
                option.getId(),
                option.getText(),
                option.getSortOrder(),
                option.isCorrect(),
                option.getCorrectPosition(),
                option.getMatchText()
        );
    }
}
