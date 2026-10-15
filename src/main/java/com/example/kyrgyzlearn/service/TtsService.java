package com.example.kyrgyzlearn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

// Озвучка кыргызского текста: текст уходит в отдельный сервис kyrgyz-learn-tts (Python, модель KaniTTS),
// готовый WAV сразу отдаётся фронту и на сервере нигде не хранится — пользователь его слушает и при желании скачивает.
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    // генерация на процессоре идёт секунды, а запросы в сервис стоят в очереди — ждём с запасом
    private static final Duration GENERATION_TIMEOUT = Duration.ofMinutes(3);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Value("${tts.url}")
    private String ttsUrl;

    // WAV-файл озвучки
    public byte[] synthesize(String text) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ttsUrl.replaceAll("/$", "") + "/tts"))
                .timeout(GENERATION_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(Map.of("text", text.strip())), StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            log.warn("TTS service {} is unavailable: {}", ttsUrl, e.toString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TTS service is unavailable");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TTS request interrupted");
        }

        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            log.warn("TTS service answered {}: {}", response.statusCode(), body);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TTS service failed to synthesize the text");
        }
        return response.body();
    }

}
