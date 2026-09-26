package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.common.Nullable;
import com.example.kyrgyzlearn.dto.lesson.*;
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
}
