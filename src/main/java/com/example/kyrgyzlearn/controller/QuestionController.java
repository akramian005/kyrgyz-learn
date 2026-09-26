package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.common.Nullable;
import com.example.kyrgyzlearn.dto.question.*;
import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import com.example.kyrgyzlearn.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/lessons/{lessonId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // ---------- Вопросы ----------

    @PostMapping
    public ResponseEntity<QuestionDto> addQuestion(
            @PathVariable Long lessonId,
            @Valid @RequestBody QuestionCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.addQuestion(lessonId, request, authentication));
    }

    @PatchMapping("/{questionId}")
    public ResponseEntity<QuestionDto> updateQuestion(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @RequestBody JsonNode body,
            Authentication authentication) {
        QuestionUpdateRequest request = new QuestionUpdateRequest(
                extractString(body, "text"),
                extractQuestionType(body, "type")
        );
        return ResponseEntity.ok(questionService.updateQuestion(lessonId, questionId, request, authentication));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            Authentication authentication) {
        questionService.deleteQuestion(lessonId, questionId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{questionId}/audio")
    public ResponseEntity<String> uploadQuestionAudio(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.uploadQuestionAudio(lessonId, questionId, file, authentication));
    }

    @DeleteMapping("/{questionId}/audio")
    public ResponseEntity<Void> deleteQuestionAudio(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            Authentication authentication) {
        questionService.deleteQuestionAudio(lessonId, questionId, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Варианты ответа ----------

    @PostMapping("/{questionId}/options")
    public ResponseEntity<AnswerOptionDto> addAnswerOption(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerOptionCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.addAnswerOption(lessonId, questionId, request, authentication));
    }

    @PatchMapping("/{questionId}/options/{optionId}")
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
        return ResponseEntity.ok(questionService.updateAnswerOption(lessonId, questionId, optionId, request, authentication));
    }

    @DeleteMapping("/{questionId}/options/{optionId}")
    public ResponseEntity<Void> deleteAnswerOption(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @PathVariable Long optionId,
            Authentication authentication) {
        questionService.deleteAnswerOption(lessonId, questionId, optionId, authentication);
        return ResponseEntity.noContent().build();
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
