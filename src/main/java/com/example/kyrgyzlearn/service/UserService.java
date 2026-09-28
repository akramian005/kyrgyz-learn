package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.auth.AuthResponse;
import com.example.kyrgyzlearn.dto.user.EmailChangeRequest;
import com.example.kyrgyzlearn.dto.user.PasswordChangeRequest;
import com.example.kyrgyzlearn.dto.user.UserProfileDto;
import com.example.kyrgyzlearn.dto.user.UserUpdateRequest;
import com.example.kyrgyzlearn.entity.user.User;
import com.example.kyrgyzlearn.jwt.JwtService;
import com.example.kyrgyzlearn.repository.UserRepository;
import com.example.kyrgyzlearn.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

// Профиль текущего пользователя: имя, почта, пароль, аватар
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Authentication authentication) {
        return toProfileDto(findCurrentUser(authentication));
    }

    @Transactional
    public UserProfileDto updateProfile(UserUpdateRequest request, Authentication authentication) {
        User user = findCurrentUser(authentication);
        user.setName(request.name().trim());
        return toProfileDto(userRepository.save(user));
    }

    // email — это subject в JWT, поэтому после смены старый токен перестаёт работать: отдаём новый
    @Transactional
    public AuthResponse changeEmail(EmailChangeRequest request, Authentication authentication) {
        User user = findCurrentUser(authentication);
        requireCorrectPassword(user, request.currentPassword());

        String email = normalizeEmail(request.email());
        if (!email.equals(user.getEmail())) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + email);
            }
            user.setEmail(email);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request, Authentication authentication) {
        User user = findCurrentUser(authentication);
        requireCorrectPassword(user, request.currentPassword());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public String uploadAvatar(MultipartFile file, Authentication authentication) {
        User user = findCurrentUser(authentication);

        if (user.getAvatarObjectKey() != null) {
            fileStorageService.delete(user.getAvatarObjectKey());
        }

        String folder = "users/" + user.getId() + "/avatar";
        String objectKey = fileStorageService.uploadImage(folder, file);
        user.setAvatarObjectKey(objectKey);
        userRepository.save(user);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    @Transactional
    public void deleteAvatar(Authentication authentication) {
        User user = findCurrentUser(authentication);

        if (user.getAvatarObjectKey() != null) {
            fileStorageService.delete(user.getAvatarObjectKey());
            user.setAvatarObjectKey(null);
        }
    }

    // ---------- Вспомогательные методы ----------

    // User из токена загружен вне этой транзакции — берём свежую копию из БД
    private User findCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // 400, а не 401: на 401 фронтенд считает, что токен протух, и выкидывает из аккаунта
    private void requireCorrectPassword(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong current password");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserProfileDto toProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getAvatarObjectKey() != null ? fileStorageService.getPresignedUrl(user.getAvatarObjectKey()) : null
        );
    }
}
