package com.example.kyrgyzlearn.dto.tts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TtsRequest(
        @NotBlank(message = "Text is required")
        @Size(max = 300, message = "Text must be at most 300 characters")
        String text
) {
}
