package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.auth.AuthResponse;
import com.example.kyrgyzlearn.dto.user.EmailChangeRequest;
import com.example.kyrgyzlearn.dto.user.PasswordChangeRequest;
import com.example.kyrgyzlearn.dto.user.UserProfileDto;
import com.example.kyrgyzlearn.dto.user.UserUpdateRequest;
import com.example.kyrgyzlearn.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// Профиль текущего пользователя (по токену). Все запросы только для вошедших — см. anyRequest() в SecurityConfig
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication));
    }

    @PatchMapping
    public ResponseEntity<UserProfileDto> updateProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfile(request, authentication));
    }

    // возвращает новый токен — старый привязан к прежней почте
    @PatchMapping("/email")
    public ResponseEntity<AuthResponse> changeEmail(
            @Valid @RequestBody EmailChangeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.changeEmail(request, authentication));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        userService.changePassword(request, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/avatar")
    public ResponseEntity<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.uploadAvatar(file, authentication));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) {
        userService.deleteAvatar(authentication);
        return ResponseEntity.noContent().build();
    }
}
