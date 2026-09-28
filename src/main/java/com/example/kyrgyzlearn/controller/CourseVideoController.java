package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.common.Nullable;
import com.example.kyrgyzlearn.dto.video.CourseVideoCreateRequest;
import com.example.kyrgyzlearn.dto.video.CourseVideoDto;
import com.example.kyrgyzlearn.dto.video.CourseVideoUpdateRequest;
import com.example.kyrgyzlearn.service.CourseVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

// Видео курса. Общий список уроков и видео — GET /api/courses/{id}/items в CourseController
@RestController
@RequestMapping("/api/courses/{courseId}/videos")
@RequiredArgsConstructor
public class CourseVideoController {

    private final CourseVideoService courseVideoService;

    @GetMapping("/{videoId}")
    public ResponseEntity<CourseVideoDto> getVideo(
            @PathVariable Long courseId,
            @PathVariable Long videoId,
            Authentication authentication) {
        return ResponseEntity.ok(courseVideoService.getVideo(courseId, videoId, authentication));
    }

    @PostMapping
    public ResponseEntity<CourseVideoDto> createVideo(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseVideoCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseVideoService.createVideo(courseId, request, authentication));
    }

    @PatchMapping("/{videoId}")
    public ResponseEntity<CourseVideoDto> updateVideo(
            @PathVariable Long courseId,
            @PathVariable Long videoId,
            @RequestBody JsonNode body,
            Authentication authentication) {
        CourseVideoUpdateRequest request = new CourseVideoUpdateRequest(
                extractString(body, "title"),
                extractString(body, "description")
        );
        return ResponseEntity.ok(courseVideoService.updateVideo(courseId, videoId, request, authentication));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable Long courseId,
            @PathVariable Long videoId,
            Authentication authentication) {
        courseVideoService.deleteVideo(courseId, videoId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{videoId}/file")
    public ResponseEntity<String> uploadVideoFile(
            @PathVariable Long courseId,
            @PathVariable Long videoId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseVideoService.uploadVideoFile(courseId, videoId, file, authentication));
    }

    @DeleteMapping("/{videoId}/file")
    public ResponseEntity<Void> deleteVideoFile(
            @PathVariable Long courseId,
            @PathVariable Long videoId,
            Authentication authentication) {
        courseVideoService.deleteVideoFile(courseId, videoId, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Разбор PATCH-тела ----------

    private Nullable<String> extractString(JsonNode body, String field) {
        if (!body.has(field)) return Nullable.undefined();
        JsonNode node = body.get(field);
        return Nullable.of(node.isNull() ? null : node.asString());
    }
}
