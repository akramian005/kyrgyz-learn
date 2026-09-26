package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.common.Nullable;
import com.example.kyrgyzlearn.dto.lesson.*;
import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import com.example.kyrgyzlearn.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    // список уроков всегда в рамках конкретного курса
    @GetMapping
    public ResponseEntity<Page<LessonSummaryDto>> getLessonsByCourse(
            @RequestParam Long courseId,
            @PageableDefault(page = 0, size = 10, sort = "position") Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lessonService.getLessonsByCourse(courseId, pageable, authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonDto> getLessonById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(lessonService.getLessonById(id, authentication));
    }

    @PostMapping
    public ResponseEntity<LessonDto> createLesson(
            @Valid @RequestBody LessonCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createLesson(request, authentication));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LessonDto> updateLesson(
            @PathVariable Long id,
            @RequestBody JsonNode body,
            Authentication authentication) {
        LessonUpdateRequest request = new LessonUpdateRequest(
                extractString(body, "title"),
                extractString(body, "description"),
                extractInteger(body, "position")
        );
        return ResponseEntity.ok(lessonService.updateLesson(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, Authentication authentication) {
        lessonService.deleteLesson(id, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Вопросы ----------

    @PostMapping("/{lessonId}/questions")
    public ResponseEntity<QuestionDto> addQuestion(
            @PathVariable Long lessonId,
            @Valid @RequestBody QuestionCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.addQuestion(lessonId, request, authentication));
    }

    @PatchMapping("/{lessonId}/questions/{questionId}")
    public ResponseEntity<QuestionDto> updateQuestion(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @RequestBody JsonNode body,
            Authentication authentication) {
        QuestionUpdateRequest request = new QuestionUpdateRequest(
                extractString(body, "text"),
                extractQuestionType(body, "type")
        );
        return ResponseEntity.ok(lessonService.updateQuestion(lessonId, questionId, request, authentication));
    }

    @DeleteMapping("/{lessonId}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            Authentication authentication) {
        lessonService.deleteQuestion(lessonId, questionId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{lessonId}/questions/{questionId}/audio")
    public ResponseEntity<String> uploadQuestionAudio(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.uploadQuestionAudio(lessonId, questionId, file, authentication));
    }

    @DeleteMapping("/{lessonId}/questions/{questionId}/audio")
    public ResponseEntity<Void> deleteQuestionAudio(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            Authentication authentication) {
        lessonService.deleteQuestionAudio(lessonId, questionId, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Варианты ответа ----------

    @PostMapping("/{lessonId}/questions/{questionId}/options")
    public ResponseEntity<AnswerOptionDto> addAnswerOption(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerOptionCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.addAnswerOption(lessonId, questionId, request, authentication));
    }

    @PatchMapping("/{lessonId}/questions/{questionId}/options/{optionId}")
    public ResponseEntity<AnswerOptionDto> updateAnswerOption(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @PathVariable Long optionId,
            @RequestBody JsonNode body,
            Authentication authentication) {
        AnswerOptionUpdateRequest request = new AnswerOptionUpdateRequest(
                extractString(body, "text"),
                extractBoolean(body, "correct"),
                extractInteger(body, "correctPosition")
        );
        return ResponseEntity.ok(lessonService.updateAnswerOption(lessonId, questionId, optionId, request, authentication));
    }

    @DeleteMapping("/{lessonId}/questions/{questionId}/options/{optionId}")
    public ResponseEntity<Void> deleteAnswerOption(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @PathVariable Long optionId,
            Authentication authentication) {
        lessonService.deleteAnswerOption(lessonId, questionId, optionId, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Сообщения-объяснения ----------

    @PostMapping("/{lessonId}/messages")
    public ResponseEntity<LessonMessageDto> addMessage(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonMessageCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.addMessage(lessonId, request, authentication));
    }

    @PatchMapping("/{lessonId}/messages/{messageId}")
    public ResponseEntity<LessonMessageDto> updateMessage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            @RequestBody JsonNode body,
            Authentication authentication) {
        LessonMessageUpdateRequest request = new LessonMessageUpdateRequest(extractString(body, "text"));
        return ResponseEntity.ok(lessonService.updateMessage(lessonId, messageId, request, authentication));
    }

    @DeleteMapping("/{lessonId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            Authentication authentication) {
        lessonService.deleteMessage(lessonId, messageId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{lessonId}/messages/{messageId}/image")
    public ResponseEntity<String> uploadMessageImage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.uploadMessageImage(lessonId, messageId, file, authentication));
    }

    @PostMapping("/{lessonId}/messages/{messageId}/audio")
    public ResponseEntity<String> uploadMessageAudio(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.uploadMessageAudio(lessonId, messageId, file, authentication));
    }

    @DeleteMapping("/{lessonId}/messages/{messageId}/image")
    public ResponseEntity<Void> deleteMessageImage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            Authentication authentication) {
        lessonService.deleteMessageImage(lessonId, messageId, authentication);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{lessonId}/messages/{messageId}/audio")
    public ResponseEntity<Void> deleteMessageAudio(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            Authentication authentication) {
        lessonService.deleteMessageAudio(lessonId, messageId, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Порядок элементов ----------

    // переместить вопрос или сообщение внутри урока; возвращает урок с новым порядком
    @PatchMapping("/{lessonId}/items/{itemId}/position")
    public ResponseEntity<LessonDto> moveItem(
            @PathVariable Long lessonId,
            @PathVariable Long itemId,
            @Valid @RequestBody ItemMoveRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(lessonService.moveItem(lessonId, itemId, request.position(), authentication));
    }

    // ---------- Разбор PATCH-тела ----------

    private Nullable<String> extractString(JsonNode body, String field) {
        if (!body.has(field)) return Nullable.undefined();
        JsonNode node = body.get(field);
        return Nullable.of(node.isNull() ? null : node.asString());
    }

    private Nullable<Integer> extractInteger(JsonNode body, String field) {
        if (!body.has(field)) return Nullable.undefined();
        JsonNode node = body.get(field);
        return Nullable.of(node.isNull() ? null : node.intValue());
    }

    private Nullable<Boolean> extractBoolean(JsonNode body, String field) {
        if (!body.has(field)) return Nullable.undefined();
        JsonNode node = body.get(field);
        return Nullable.of(node.isNull() ? null : node.booleanValue());
    }

    private Nullable<QuestionType> extractQuestionType(JsonNode body, String field) {
        if (!body.has(field)) return Nullable.undefined();
        JsonNode node = body.get(field);
        return Nullable.of(node.isNull() ? null : QuestionType.valueOf(node.asString()));
    }
}
