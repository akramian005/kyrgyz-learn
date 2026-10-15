package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.lesson.ItemMoveRequest;
import com.example.kyrgyzlearn.dto.unit.UnitCreateRequest;
import com.example.kyrgyzlearn.dto.unit.UnitDto;
import com.example.kyrgyzlearn.dto.unit.UnitUpdateRequest;
import com.example.kyrgyzlearn.service.CourseUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Юниты курса — каждый со своими уроками и видео по порядку
@RestController
@RequestMapping("/api/courses/{courseId}/units")
@RequiredArgsConstructor
public class CourseUnitController {

    private final CourseUnitService courseUnitService;

    @GetMapping
    public ResponseEntity<List<UnitDto>> getUnits(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(courseUnitService.getUnits(courseId, authentication));
    }

    @PostMapping
    public ResponseEntity<UnitDto> createUnit(
            @PathVariable Long courseId,
            @Valid @RequestBody UnitCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseUnitService.createUnit(courseId, request, authentication));
    }

    @PatchMapping("/{unitId}")
    public ResponseEntity<Void> renameUnit(
            @PathVariable Long courseId,
            @PathVariable Long unitId,
            @Valid @RequestBody UnitUpdateRequest request,
            Authentication authentication) {
        courseUnitService.renameUnit(courseId, unitId, request.title(), authentication);
        return ResponseEntity.noContent().build();
    }

    // переставить юнит; возвращает все юниты в новом порядке
    @PatchMapping("/{unitId}/position")
    public ResponseEntity<List<UnitDto>> moveUnit(
            @PathVariable Long courseId,
            @PathVariable Long unitId,
            @Valid @RequestBody ItemMoveRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(courseUnitService.moveUnit(courseId, unitId, request.position(), authentication));
    }

    @DeleteMapping("/{unitId}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long courseId, @PathVariable Long unitId, Authentication authentication) {
        courseUnitService.deleteUnit(courseId, unitId, authentication);
        return ResponseEntity.noContent().build();
    }
}
