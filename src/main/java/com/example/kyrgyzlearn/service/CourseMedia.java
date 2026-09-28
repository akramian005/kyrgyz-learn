package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.content.OfficialContent;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.mapper.MediaUrls;
import com.example.kyrgyzlearn.service.FileStorageService.Kind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

// Картинки, аудио и видео уроков и видео курса: у личных курсов — в MinIO,
// у официальных — файлами в official-content.media-dir рядом с JSON курса (см. OfficialContent).
// Сервисы работают с медиа только через него, чтобы не думать, где лежит курс.
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseMedia {

    private final FileStorageService fileStorageService;
    private final OfficialContent officialContent;
    private final MediaUrls mediaUrls;

    // folder — папка в MinIO у личных курсов; у официальных файл всегда кладётся в папку курса
    public String upload(Course course, String folder, MultipartFile file, Kind kind) {
        if (course.getType() != CourseType.OFFICIAL) {
            return fileStorageService.uploadFile(folder, file, kind);
        }

        fileStorageService.validate(file, kind);
        try (InputStream in = file.getInputStream()) {
            return officialContent.storeMedia(course, FileStorageService.extensionOf(file.getOriginalFilename()), in);
        } catch (IOException | UncheckedIOException e) {
            log.error("Failed to store official media for course {}", course.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file");
        }
    }

    public void delete(Course course, String key) {
        if (key == null) {
            return;
        }
        if (course.getType() != CourseType.OFFICIAL) {
            fileStorageService.delete(key);
            return;
        }
        try {
            officialContent.deleteMedia(key);
        } catch (UncheckedIOException e) {
            log.error("Failed to delete official media {}", key, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file");
        }
    }

    // ссылка для клиента: у официальных — постоянная на бэкенд, у личных — подписанная на MinIO
    public String url(Course course, String key) {
        return mediaUrls.url(key, course);
    }
}
