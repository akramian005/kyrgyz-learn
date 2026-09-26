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
    private final FileStorageService fileStorageService;
    private final LessonMediaCleaner lessonMediaCleaner;

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

        lessonMediaCleaner.deleteLessonMedia(lesson);

        lessonRepository.delete(lesson); // каскадно удалит items и options
    }

    // ---------- Вопросы ----------

    @Transactional
    public QuestionDto addQuestion(Long lessonId, QuestionCreateRequest request, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        Question question = new Question();
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

        insertItem(lesson, question, request.position());
        return toQuestionDto(questionRepository.save(question));
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

        lessonMediaCleaner.deleteItemMedia(question);
        removeItem(question); // каскадно удалит options
    }

    @Transactional
    public String uploadQuestionAudio(Long lessonId, Long questionId, MultipartFile file, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        if (question.getAudioObjectKey() != null) {
            fileStorageService.delete(question.getAudioObjectKey());
        }

        String folder = "questions/" + questionId + "/audio";
        String objectKey = fileStorageService.uploadAudio(folder, file);
        question.setAudioObjectKey(objectKey);
        questionRepository.save(question);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    @Transactional
    public void deleteQuestionAudio(Long lessonId, Long questionId, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        if (question.getAudioObjectKey() != null) {
            fileStorageService.delete(question.getAudioObjectKey());
            question.setAudioObjectKey(null);
        }
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

        answerOptionRepository.delete(option);
    }

    // ---------- Сообщения-объяснения ----------

    @Transactional
    public LessonMessageDto addMessage(Long lessonId, LessonMessageCreateRequest request, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        LessonMessage message = new LessonMessage();
        message.setText(request.text());

        insertItem(lesson, message, request.position());
        return toMessageDto(lessonMessageRepository.save(message));
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

        lessonMediaCleaner.deleteItemMedia(message);
        removeItem(message);
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

    // ---------- Порядок элементов урока ----------

    // перемещает вопрос или сообщение на новую позицию, остальные элементы сдвигаются
    @Transactional
    public LessonDto moveItem(Long lessonId, Long itemId, int position, Authentication authentication) {
        Lesson lesson = findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        List<LessonItem> items = lesson.getItems();
        LessonItem item = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Item " + itemId + " not found in lesson " + lessonId));

        items.remove(item);
        items.add(clampIndex(position, items.size()), item);
        renumber(items);

        return toDto(lesson);
    }

    // ---------- Вспомогательные методы ----------

    // вставляет элемент в урок на позицию (null — в конец) и перенумеровывает все элементы 0..n-1
    private void insertItem(Lesson lesson, LessonItem item, Integer position) {
        List<LessonItem> items = lesson.getItems();
        int index = position == null ? items.size() : clampIndex(position, items.size());

        item.setLesson(lesson);
        items.add(index, item);
        renumber(items);
    }

    // orphanRemoval у Lesson.items удалит элемент из БД, оставшиеся позиции идут без дыр
    private void removeItem(LessonItem item) {
        List<LessonItem> items = item.getLesson().getItems();
        items.remove(item);
        renumber(items);
    }

    private void renumber(List<LessonItem> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }
    }

    private int clampIndex(int position, int size) {
        return Math.max(0, Math.min(position, size));
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
        findQuestionOrThrow(lessonId, questionId);

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
                option.getSortOrder(),
                option.isCorrect(),
                option.getCorrectPosition()
        );
    }
}