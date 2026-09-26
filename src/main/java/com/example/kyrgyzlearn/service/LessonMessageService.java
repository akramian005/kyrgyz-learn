package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.message.LessonMessageCreateRequest;
import com.example.kyrgyzlearn.dto.message.LessonMessageDto;
import com.example.kyrgyzlearn.dto.message.LessonMessageUpdateRequest;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.mapper.LessonMapper;
import com.example.kyrgyzlearn.repository.LessonMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// Сообщения-объяснения внутри урока
@Service
@RequiredArgsConstructor
public class LessonMessageService {

    private final LessonMessageRepository lessonMessageRepository;
    private final LessonService lessonService;
    private final CourseService courseService;
    private final FileStorageService fileStorageService;
    private final LessonMediaCleaner lessonMediaCleaner;
    private final LessonMapper lessonMapper;

    @Transactional
    public LessonMessageDto addMessage(Long lessonId, LessonMessageCreateRequest request, Authentication authentication) {
        Lesson lesson = lessonService.findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        LessonMessage message = new LessonMessage();
        message.setText(request.text());

        lessonService.insertItem(lesson, message, request.position());
        return lessonMapper.toMessageDto(lessonMessageRepository.save(message));
    }

    @Transactional
    public LessonMessageDto updateMessage(Long lessonId, Long messageId, LessonMessageUpdateRequest request, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (request.text().isPresent()) {
            message.setText(request.text().get());
        }

        return lessonMapper.toMessageDto(lessonMessageRepository.save(message));
    }

    @Transactional
    public void deleteMessage(Long lessonId, Long messageId, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        lessonMediaCleaner.deleteItemMedia(message);
        lessonService.removeItem(message);
    }

    @Transactional
    public String uploadMessageImage(Long lessonId, Long messageId, MultipartFile file, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (message.getImageObjectKey() != null) {
            fileStorageService.delete(message.getImageObjectKey());
        }

        String folder = "messages/" + messageId + "/images";
        String objectKey = fileStorageService.uploadImage(folder, file);
        message.setImageObjectKey(objectKey);
        lessonMessageRepository.save(message);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    @Transactional
    public String uploadMessageAudio(Long lessonId, Long messageId, MultipartFile file, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (message.getAudioObjectKey() != null) {
            fileStorageService.delete(message.getAudioObjectKey());
        }

        String folder = "messages/" + messageId + "/audio";
        String objectKey = fileStorageService.uploadAudio(folder, file);
        message.setAudioObjectKey(objectKey);
        lessonMessageRepository.save(message);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    @Transactional
    public void deleteMessageImage(Long lessonId, Long messageId, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (message.getImageObjectKey() != null) {
            fileStorageService.delete(message.getImageObjectKey());
            message.setImageObjectKey(null);
        }
    }

    @Transactional
    public void deleteMessageAudio(Long lessonId, Long messageId, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (message.getAudioObjectKey() != null) {
            fileStorageService.delete(message.getAudioObjectKey());
            message.setAudioObjectKey(null);
        }
    }

    // ---------- Вспомогательные методы ----------

    private LessonMessage findMessageOrThrow(Long lessonId, Long messageId) {
        LessonMessage message = lessonMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId));

        if (!message.getLesson().getId().equals(lessonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Message " + messageId + " does not belong to lesson " + lessonId);
        }
        return message;
    }
}
