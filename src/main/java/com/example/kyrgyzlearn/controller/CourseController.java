package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.common.Nullable;
import com.example.kyrgyzlearn.dto.course.CourseCreateRequest;
import com.example.kyrgyzlearn.dto.course.CourseDto;
import com.example.kyrgyzlearn.dto.course.CourseItemDto;
import com.example.kyrgyzlearn.dto.course.CourseItemMoveRequest;
import com.example.kyrgyzlearn.dto.course.CourseUpdateRequest;
import com.example.kyrgyzlearn.service.CourseItemService;
import com.example.kyrgyzlearn.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseItemService courseItemService;

    @GetMapping
    public ResponseEntity<Page<CourseDto>> getAllCourses(
            @PageableDefault(page = 0, size = 10, sort = "position") Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(courseService.getAllCourses(pageable, authentication));
    }

    // поиск личных курсов всех пользователей по названию; query пустой — все курсы, новые первыми
    @GetMapping("/search")
    public ResponseEntity<Page<CourseDto>> searchCourses(
            @RequestParam(defaultValue = "") String query,
            @PageableDefault(page = 0, size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(courseService.searchCourses(query, pageable, authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> getCourseById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(courseService.getCourseById(id, authentication));
    }

    // authenticated(), не hasRole — тип курса (OFFICIAL/USER) решает сервис по роли автора
    @PostMapping
    public ResponseEntity<CourseDto> createCourse(
            @Valid @RequestBody CourseCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request, authentication));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CourseDto> updateCourse(
            @PathVariable Long id,
            @RequestBody JsonNode body,
            Authentication authentication) {
        CourseUpdateRequest request = new CourseUpdateRequest(
                extractString(body, "title"),
                extractString(body, "description"),
                extractInteger(body, "position")
        );
        return ResponseEntity.ok(courseService.updateCourse(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id, Authentication authentication) {
        courseService.deleteCourse(id, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Картинка курса ----------

    // возвращает ссылку на загруженную картинку; старая удаляется
    @PostMapping("/{id}/image")
    public ResponseEntity<String> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.uploadImage(id, file, authentication));
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, Authentication authentication) {
        courseService.deleteImage(id, authentication);
        return ResponseEntity.noContent().build();
    }

    // ---------- Уроки и видео курса одним списком ----------

    @GetMapping("/{id}/items")
    public ResponseEntity<List<CourseItemDto>> getCourseItems(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(courseItemService.getItems(id, authentication));
    }

    // переместить урок или видео внутри курса; возвращает новый порядок
    @PatchMapping("/{id}/items/position")
    public ResponseEntity<List<CourseItemDto>> moveCourseItem(
            @PathVariable Long id,
            @Valid @RequestBody CourseItemMoveRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(courseItemService.moveItem(id, request, authentication));
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
