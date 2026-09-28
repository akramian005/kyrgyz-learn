package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.content.OfficialContent;
import com.example.kyrgyzlearn.dto.message.LessonMessageCreateRequest;
import com.example.kyrgyzlearn.dto.message.LessonMessageDto;
import com.example.kyrgyzlearn.dto.message.LessonMessageUpdateRequest;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.mapper.LessonMapper;
import com.example.kyrgyzlearn.repository.LessonMessageRepository;
import com.example.kyrgyzlearn.service.FileStorageService.Kind;
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
    private final CourseMedia courseMedia;
    private final OfficialContent officialContent;
    private final LessonMediaCleaner lessonMediaCleaner;
    private final LessonMapper lessonMapper;

    @Transactional
    public LessonMessageDto addMessage(Long lessonId, LessonMessageCreateRequest request, Authentication authentication) {
        Lesson lesson = lessonService.findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        LessonMessage message = new LessonMessage();
        message.setText(request.text());

        lessonService.insertItem(lesson, message, request.position());
        if (courseService.saveIfOfficial(lesson.getCourse())) {
            return lessonMapper.toMessageDto(message);
        }
        return lessonMapper.toMessageDto(lessonMessageRepository.save(message));
    }

    @Transactional
    public LessonMessageDto updateMessage(Long lessonId, Long messageId, LessonMessageUpdateRequest request, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (request.text().isPresent()) {
            message.setText(request.text().get());
        }

        if (courseService.saveIfOfficial(courseOf(message))) {
            return lessonMapper.toMessageDto(message);
        }
        return lessonMapper.toMessageDto(lessonMessageRepository.save(message));
    }

    @Transactional
    public void deleteMessage(Long lessonId, Long messageId, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        Course course = courseOf(message);
        lessonMediaCleaner.deleteItemMedia(message);
        lessonService.removeItem(message);
        courseService.saveIfOfficial(course);
    }

    @Transactional
    public String uploadMessageImage(Long lessonId, Long messageId, MultipartFile file, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        Course course = courseOf(message);
        courseMedia.delete(course, message.getImageObjectKey());

        String folder = "messages/" + messageId + "/images";
        String objectKey = courseMedia.upload(course, folder, file, Kind.IMAGE);
        message.setImageObjectKey(objectKey);
        if (!courseService.saveIfOfficial(course)) {
            lessonMessageRepository.save(message);
        }

        return courseMedia.url(course, objectKey);
    }

    @Transactional
    public String uploadMessageAudio(Long lessonId, Long messageId, MultipartFile file, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        Course course = courseOf(message);
        courseMedia.delete(course, message.getAudioObjectKey());

        String folder = "messages/" + messageId + "/audio";
        String objectKey = courseMedia.upload(course, folder, file, Kind.AUDIO);
        message.setAudioObjectKey(objectKey);
        if (!courseService.saveIfOfficial(course)) {
            lessonMessageRepository.save(message);
        }

        return courseMedia.url(course, objectKey);
    }

    @Transactional
    public void deleteMessageImage(Long lessonId, Long messageId, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (message.getImageObjectKey() != null) {
            courseMedia.delete(courseOf(message), message.getImageObjectKey());
            message.setImageObjectKey(null);
            courseService.saveIfOfficial(courseOf(message));
        }
    }

    @Transactional
    public void deleteMessageAudio(Long lessonId, Long messageId, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (message.getAudioObjectKey() != null) {
            courseMedia.delete(courseOf(message), message.getAudioObjectKey());
            message.setAudioObjectKey(null);
            courseService.saveIfOfficial(courseOf(message));
        }
    }

    // ---------- Вспомогательные методы ----------

    private LessonMessage findMessageOrThrow(Long lessonId, Long messageId) {
        LessonMessage message = officialContent.findMessage(messageId)
                .or(() -> lessonMessageRepository.findById(messageId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId));

        if (!message.getLesson().getId().equals(lessonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Message " + messageId + " does not belong to lesson " + lessonId);
        }
        return message;
    }

    private static Course courseOf(LessonMessage message) {
        return message.getLesson().getCourse();
    }
}
