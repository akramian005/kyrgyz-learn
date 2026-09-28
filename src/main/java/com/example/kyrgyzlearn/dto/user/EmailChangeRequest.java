package com.example.kyrgyzlearn.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        // смену почты подтверждаем паролем, чтобы чужой человек с открытым приложением не угнал аккаунт
        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
