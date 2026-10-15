package com.example.kyrgyzlearn.controller;

import com.example.kyrgyzlearn.dto.tts.TtsRequest;
import com.example.kyrgyzlearn.service.TtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Генерация кыргызской речи (только для вошедших — см. anyRequest в SecurityConfig): тяжёлая операция,
// анонимам её не открываем. Ответ — сам WAV-файл, на сервере он не сохраняется
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    @PostMapping(produces = "audio/wav")
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody TtsRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(ttsService.synthesize(request.text()));
    }
}
