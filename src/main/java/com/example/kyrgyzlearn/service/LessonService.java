package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.lesson.*;
import com.example.kyrgyzlearn.entity.lesson.AnswerOption;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonItem;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import com.example.kyrgyzlearn.repository.AnswerOptionRepository;
import com.example.kyrgyzlearn.repository.CourseRepository;
import com.example.kyrgyzlearn.repository.LessonMessageRepository;
import com.example.kyrgyzlearn.repository.LessonRepository;
import com.example.kyrgyzlearn.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final LessonMessageRepository lessonMessageRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    // тот же клиент, что и для картинок товаров — просто хранит файлы по ключу,
    // так что подходит и для аудио/картинок урока без изменений
    private final ImageStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Page<LessonSummaryDto> getLessonsByCourse(Long courseId, Pageable pageable, Authentication authentication) {
        Course course = findCourseOrThrow(courseId);
        courseService.requireCanView(course, authentication);
        return lessonRepository.findByCourseId(courseId, pageable).map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public LessonDto getLessonById(Long id, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(id);
        courseService.requireCanView(lesson.getCourse(), authentication);
        return toDto(lesson);
    }

    @Transactional
    public LessonDto createLesson(LessonCreateRequest request, Authentication authentication) {
        Course course = findCourseOrThrow(request.courseId());
        courseService.requireCanModify(course, authentication);

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(request.title());
        lesson.setDescription(request.description());
        lesson.setPosition(request.position() != null ? request.position() : course.getLessons().size());

        Lesson saved = lessonRepository.save(lesson);
        course.getLessons().add(saved);

        return toDto(saved);
    }

    @Transactional
    public LessonDto updateLesson(Long id, LessonUpdateRequest request, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(id);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        if (request.title().isPresent()) {
            String newTitle = request.title().get();
            if (newTitle == null || newTitle.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson title cannot be empty");
            }
            lesson.setTitle(newTitle);
        }
        if (request.description().isPresent()) {
            lesson.setDescription(request.description().get());
        }
        if (request.position().isPresent()) {
            lesson.setPosition(request.position().get());
        }

        return toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long id, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(id);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        for (LessonItem item : lesson.getItems()) {
            deleteItemMedia(item);
        }

        lessonRepository.delete(lesson); // каскадно удалит items и options
    }

    // ---------- Вопросы ----------

    @Transactional
    public QuestionDto addQuestion(Long lessonId, QuestionCreateRequest request, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        Question question = new Question();
        question.setLesson(lesson);
        question.setPosition(nextPosition(lesson));
        question.setText(request.text());
        question.setType(request.type());

        if (request.options() != null) {
            int i = 0;
            for (AnswerOptionCreateRequest optionRequest : request.options()) {
                AnswerOption option = new AnswerOption();
                option.setQuestion(question);
                option.setText(optionRequest.text());
                option.setCorrect(optionRequest.correct());
                option.setCorrectPosition(optionRequest.correctPosition());
                option.setSortOrder(i++);
                question.getOptions().add(option);
            }
        }

        Question saved = questionRepository.save(question);
        lesson.getItems().add(saved);

        return toQuestionDto(saved);
    }

    @Transactional
    public QuestionDto updateQuestion(Long lessonId, Long questionId, QuestionUpdateRequest request, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        if (request.text().isPresent()) {
            String newText = request.text().get();
            if (newText == null || newText.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question text cannot be empty");
            }
            question.setText(newText);
        }
        if (request.type().isPresent()) {
            question.setType(request.type().get());
        }

        return toQuestionDto(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long lessonId, Long questionId, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        deleteItemMedia(question);
        questionRepository.delete(question); // каскадно удалит options
    }

    @Transactional
    public String uploadQuestionAudio(Long lessonId, Long questionId, MultipartFile file, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        if (question.getAudioObjectKey() != null) {
            fileStorageService.delete(question.getAudioObjectKey());
        }

        String objectKey = fileStorageService.upload(questionId, file);
        question.setAudioObjectKey(objectKey);
        questionRepository.save(question);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    // ---------- Варианты ответа ----------

    @Transactional
    public AnswerOptionDto addAnswerOption(Long lessonId, Long questionId, AnswerOptionCreateRequest request, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        AnswerOption option = new AnswerOption();
        option.setQuestion(question);
        option.setText(request.text());
        option.setCorrect(request.correct());
        option.setCorrectPosition(request.correctPosition());
        option.setSortOrder(question.getOptions().size());

        AnswerOption saved = answerOptionRepository.save(option);
        question.getOptions().add(saved);

        return toAnswerOptionDto(saved);
    }

    @Transactional
    public AnswerOptionDto updateAnswerOption(Long lessonId, Long questionId, Long optionId, AnswerOptionUpdateRequest request, Authentication authentication) {
        AnswerOption option = findAnswerOptionOrThrow(lessonId, questionId, optionId);
        courseService.requireCanModify(option.getQuestion().getLesson().getCourse(), authentication);

        if (request.text().isPresent()) {
            option.setText(request.text().get());
        }
        if (request.correct().isPresent()) {
            option.setCorrect(Boolean.TRUE.equals(request.correct().get()));
        }
        if (request.correctPosition().isPresent()) {
            option.setCorrectPosition(request.correctPosition().get());
        }

        return toAnswerOptionDto(answerOptionRepository.save(option));
    }

    @Transactional
    public void deleteAnswerOption(Long lessonId, Long questionId, Long optionId, Authentication authentication) {
        AnswerOption option = findAnswerOptionOrThrow(lessonId, questionId, optionId);
        courseService.requireCanModify(option.getQuestion().getLesson().getCourse(), authentication);

        if (option.getImageObjectKey() != null) {
            fileStorageService.delete(option.getImageObjectKey());
        }

        answerOptionRepository.delete(option);
    }

    @Transactional
    public String uploadOptionImage(Long lessonId, Long questionId, Long optionId, MultipartFile file, Authentication authentication) {
        AnswerOption option = findAnswerOptionOrThrow(lessonId, questionId, optionId);
        courseService.requireCanModify(option.getQuestion().getLesson().getCourse(), authentication);

        if (option.getImageObjectKey() != null) {
            fileStorageService.delete(option.getImageObjectKey());
        }

        String objectKey = fileStorageService.upload(optionId, file);
        option.setImageObjectKey(objectKey);
        answerOptionRepository.save(option);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    // ---------- Сообщения-объяснения ----------

    @Transactional
    public LessonMessageDto addMessage(Long lessonId, LessonMessageCreateRequest request, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        LessonMessage message = new LessonMessage();
        message.setLesson(lesson);
        message.setPosition(nextPosition(lesson));
        message.setText(request.text());

        LessonMessage saved = lessonMessageRepository.save(message);
        lesson.getItems().add(saved);

        return toMessageDto(saved);
    }

    @Transactional
    public LessonMessageDto updateMessage(Long lessonId, Long messageId, LessonMessageUpdateRequest request, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (request.text().isPresent()) {
            message.setText(request.text().get());
        }

        return toMessageDto(lessonMessageRepository.save(message));
    }

    @Transactional
    public void deleteMessage(Long lessonId, Long messageId, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        deleteItemMedia(message);
        lessonMessageRepository.delete(message);
    }

    @Transactional
    public String uploadMessageImage(Long lessonId, Long messageId, MultipartFile file, Authentication authentication) {
        LessonMessage message = findMessageOrThrow(lessonId, messageId);
        courseService.requireCanModify(message.getLesson().getCourse(), authentication);

        if (message.getImageObjectKey() != null) {
            fileStorageService.delete(message.getImageObjectKey());
        }

        String objectKey = fileStorageService.upload(messageId, file);
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

        String objectKey = fileStorageService.upload(messageId, file);
        message.setAudioObjectKey(objectKey);
        lessonMessageRepository.save(message);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    // ---------- Вспомогательные методы ----------

    private int nextPosition(Lesson lesson) {
        return lesson.getItems().size();
    }

    private void deleteItemMedia(LessonItem item) {
        if (item instanceof Question q) {
            if (q.getAudioObjectKey() != null) {
                fileStorageService.delete(q.getAudioObjectKey());
            }
            for (AnswerOption option : q.getOptions()) {
                if (option.getImageObjectKey() != null) {
                    fileStorageService.delete(option.getImageObjectKey());
                }
            }
        } else if (item instanceof LessonMessage m) {
            if (m.getImageObjectKey() != null) {
                fileStorageService.delete(m.getImageObjectKey());
            }
            if (m.getAudioObjectKey() != null) {
                fileStorageService.delete(m.getAudioObjectKey());
            }
        }
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + id));
    }

    private Lesson findLessonOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found: " + id));
    }

    private Question findQuestionOrThrow(Long lessonId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        if (!question.getLesson().getId().equals(lessonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Question " + questionId + " does not belong to lesson " + lessonId);
        }
        return question;
    }

    private LessonMessage findMessageOrThrow(Long lessonId, Long messageId) {
        LessonMessage message = lessonMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId));

        if (!message.getLesson().getId().equals(lessonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Message " + messageId + " does not belong to lesson " + lessonId);
        }
        return message;
    }

    private AnswerOption findAnswerOptionOrThrow(Long lessonId, Long questionId, Long optionId) {
        findQuestionOrThrow(lessonId, questionId); // заодно проверит принадлежность вопроса уроку

        AnswerOption option = answerOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found: " + optionId));

        if (!option.getQuestion().getId().equals(questionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Option " + optionId + " does not belong to question " + questionId);
        }
        return option;
    }

    // ---------- Маппинг в DTO ----------

    private LessonSummaryDto toSummaryDto(Lesson lesson) {
        return new LessonSummaryDto(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getPosition(),
                lesson.getItems().size()
        );
    }

    private LessonDto toDto(Lesson lesson) {
        List<LessonItemDto> items = lesson.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new LessonDto(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getPosition(),
                items
        );
    }

    private LessonItemDto toItemDto(LessonItem item) {
        if (item instanceof Question q) {
            return toQuestionDto(q);
        } else if (item instanceof LessonMessage m) {
            return toMessageDto(m);
        }
        throw new IllegalStateException("Unknown lesson item type: " + item.getClass());
    }

    private QuestionDto toQuestionDto(Question question) {
        List<AnswerOptionDto> options = question.getOptions().stream()
                .map(this::toAnswerOptionDto)
                .toList();

        return new QuestionDto(
                question.getId(),
                question.getPosition(),
                question.getText(),
                question.getAudioObjectKey() != null ? fileStorageService.getPresignedUrl(question.getAudioObjectKey()) : null,
                question.getType(),
                options
        );
    }

    private LessonMessageDto toMessageDto(LessonMessage message) {
        return new LessonMessageDto(
                message.getId(),
                message.getPosition(),
                message.getText(),
                message.getImageObjectKey() != null ? fileStorageService.getPresignedUrl(message.getImageObjectKey()) : null,
                message.getAudioObjectKey() != null ? fileStorageService.getPresignedUrl(message.getAudioObjectKey()) : null
        );
    }

    private AnswerOptionDto toAnswerOptionDto(AnswerOption option) {
        return new AnswerOptionDto(
                option.getId(),
                option.getText(),
                option.getImageObjectKey() != null ? fileStorageService.getPresignedUrl(option.getImageObjectKey()) : null,
                option.getSortOrder(),
                option.isCorrect(),
                option.getCorrectPosition()
        );
    }
}
