package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.common.Nullable;
import com.example.kyrgyzlearn.dto.message.*;
import com.example.kyrgyzlearn.service.LessonMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

// Сообщения-объяснения внутри урока
@RestController
@RequestMapping("/api/lessons/{lessonId}/messages")
@RequiredArgsConstructor
public class LessonMessageController {

    private final LessonMessageService lessonMessageService;

    @PostMapping
    public ResponseEntity<LessonMessageDto> addMessage(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonMessageCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonMessageService.addMessage(lessonId, request, authentication));
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<LessonMessageDto> updateMessage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            @RequestBody JsonNode body,
            Authentication authentication) {
        LessonMessageUpdateRequest request = new LessonMessageUpdateRequest(extractString(body, "text"));
        return ResponseEntity.ok(lessonMessageService.updateMessage(lessonId, messageId, request, authentication));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            Authentication authentication) {
        lessonMessageService.deleteMessage(lessonId, messageId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/image")
    public ResponseEntity<String> uploadMessageImage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonMessageService.uploadMessageImage(lessonId, messageId, file, authentication));
    }

    @PostMapping("/{messageId}/audio")
    public ResponseEntity<String> uploadMessageAudio(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonMessageService.uploadMessageAudio(lessonId, messageId, file, authentication));
    }

    @DeleteMapping("/{messageId}/image")
    public ResponseEntity<Void> deleteMessageImage(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            Authentication authentication) {
        lessonMessageService.deleteMessageImage(lessonId, messageId, authentication);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{messageId}/audio")
    public ResponseEntity<Void> deleteMessageAudio(
            @PathVariable Long lessonId,
            @PathVariable Long messageId,
            Authentication authentication) {
        lessonMessageService.deleteMessageAudio(lessonId, messageId, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Разбор PATCH-тела ----------

    private Nullable<String> extractString(JsonNode body, String field) {
        if (!body.has(field)) return Nullable.undefined();
        JsonNode node = body.get(field);
        return Nullable.of(node.isNull() ? null : node.asString());
    }
}
