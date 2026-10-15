package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.content.OfficialContent;
import com.example.kyrgyzlearn.dto.question.AnswerOptionCreateRequest;
import com.example.kyrgyzlearn.dto.question.AnswerOptionDto;
import com.example.kyrgyzlearn.dto.question.AnswerOptionUpdateRequest;
import com.example.kyrgyzlearn.dto.question.QuestionCreateRequest;
import com.example.kyrgyzlearn.dto.question.QuestionDto;
import com.example.kyrgyzlearn.dto.question.QuestionUpdateRequest;
import com.example.kyrgyzlearn.entity.lesson.AnswerOption;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.Question;
import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import com.example.kyrgyzlearn.mapper.LessonMapper;
import com.example.kyrgyzlearn.repository.AnswerOptionRepository;
import com.example.kyrgyzlearn.repository.QuestionRepository;
import com.example.kyrgyzlearn.service.FileStorageService.Kind;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final LessonService lessonService;
    private final CourseService courseService;
    private final CourseMedia courseMedia;
    private final OfficialContent officialContent;
    private final LessonMediaCleaner lessonMediaCleaner;
    private final LessonMapper lessonMapper;

    // ---------- Вопросы ----------

    @Transactional
    public QuestionDto addQuestion(Long lessonId, QuestionCreateRequest request, Authentication authentication) {
        Lesson lesson = lessonService.findLessonOrThrow(lessonId);
        courseService.requireCanModify(lesson.getCourse(), authentication);

        Question question = new Question();
        question.setText(request.text());
        question.setType(request.type());

        if (request.options() != null) {
            requireMatchingLimit(request.type(), request.options().size());
            int i = 0;
            for (AnswerOptionCreateRequest optionRequest : request.options()) {
                AnswerOption option = new AnswerOption();
                option.setQuestion(question);
                option.setText(optionRequest.text());
                option.setCorrect(optionRequest.correct());
                option.setCorrectPosition(optionRequest.correctPosition());
                option.setMatchText(optionRequest.matchText());
                option.setSortOrder(i++);
                question.getOptions().add(option);
            }
        }

        lessonService.insertItem(lesson, question, request.position());
        if (courseService.saveIfOfficial(lesson.getCourse())) {
            return lessonMapper.toQuestionDto(question);
        }
        return lessonMapper.toQuestionDto(questionRepository.save(question));
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

        if (courseService.saveIfOfficial(courseOf(question))) {
            return lessonMapper.toQuestionDto(question);
        }
        return lessonMapper.toQuestionDto(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long lessonId, Long questionId, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        Course course = courseOf(question);
        lessonMediaCleaner.deleteItemMedia(question);
        lessonService.removeItem(question); // каскадно удалит options
        courseService.saveIfOfficial(course);
    }

    @Transactional
    public String uploadQuestionAudio(Long lessonId, Long questionId, MultipartFile file, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        Course course = courseOf(question);
        courseMedia.delete(course, question.getAudioObjectKey());

        String folder = "questions/" + questionId + "/audio";
        String objectKey = courseMedia.upload(course, folder, file, Kind.AUDIO);
        question.setAudioObjectKey(objectKey);
        if (!courseService.saveIfOfficial(course)) {
            questionRepository.save(question);
        }

        return courseMedia.url(course, objectKey);
    }

    @Transactional
    public void deleteQuestionAudio(Long lessonId, Long questionId, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);

        if (question.getAudioObjectKey() != null) {
            courseMedia.delete(courseOf(question), question.getAudioObjectKey());
            question.setAudioObjectKey(null);
            courseService.saveIfOfficial(courseOf(question));
        }
    }

    // ---------- Варианты ответа ----------

    @Transactional
    public AnswerOptionDto addAnswerOption(Long lessonId, Long questionId, AnswerOptionCreateRequest request, Authentication authentication) {
        Question question = findQuestionOrThrow(lessonId, questionId);
        courseService.requireCanModify(question.getLesson().getCourse(), authentication);
        requireMatchingLimit(question.getType(), question.getOptions().size() + 1);

        AnswerOption option = new AnswerOption();
        option.setQuestion(question);
        option.setText(request.text());
        option.setCorrect(request.correct());
        option.setCorrectPosition(request.correctPosition());
        option.setMatchText(request.matchText());
        option.setSortOrder(question.getOptions().size());

        if (courseOf(question).getType() == CourseType.OFFICIAL) {
            question.getOptions().add(option);
            courseService.saveIfOfficial(courseOf(question)); // id варианту раздаст OfficialContent
            return lessonMapper.toAnswerOptionDto(option);
        }
        AnswerOption saved = answerOptionRepository.save(option);
        question.getOptions().add(saved);

        return lessonMapper.toAnswerOptionDto(saved);
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
        if (request.matchText().isPresent()) {
            option.setMatchText(request.matchText().get());
        }

        if (courseService.saveIfOfficial(courseOf(option.getQuestion()))) {
            return lessonMapper.toAnswerOptionDto(option);
        }
        return lessonMapper.toAnswerOptionDto(answerOptionRepository.save(option));
    }

    @Transactional
    public void deleteAnswerOption(Long lessonId, Long questionId, Long optionId, Authentication authentication) {
        AnswerOption option = findAnswerOptionOrThrow(lessonId, questionId, optionId);
        courseService.requireCanModify(option.getQuestion().getLesson().getCourse(), authentication);

        Question question = option.getQuestion();
        if (courseOf(question).getType() == CourseType.OFFICIAL) {
            question.getOptions().remove(option);
            courseService.saveIfOfficial(courseOf(question));
        } else {
            answerOptionRepository.delete(option);
        }
    }

    // ---------- Вспомогательные методы ----------

    // в «Сопоставлении» не больше MAX_MATCHING_PAIRS пар, иначе столбцы не помещаются на экран телефона
    private static void requireMatchingLimit(QuestionType type, int optionCount) {
        if (type == QuestionType.MATCHING && optionCount > QuestionType.MAX_MATCHING_PAIRS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Matching question can have at most " + QuestionType.MAX_MATCHING_PAIRS + " pairs");
        }
    }

    private Question findQuestionOrThrow(Long lessonId, Long questionId) {
        Question question = officialContent.findQuestion(questionId)
                .or(() -> questionRepository.findById(questionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        if (!question.getLesson().getId().equals(lessonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Question " + questionId + " does not belong to lesson " + lessonId);
        }
        return question;
    }

    private AnswerOption findAnswerOptionOrThrow(Long lessonId, Long questionId, Long optionId) {
        findQuestionOrThrow(lessonId, questionId);

        AnswerOption option = officialContent.findOption(optionId)
                .or(() -> answerOptionRepository.findById(optionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found: " + optionId));

        if (!option.getQuestion().getId().equals(questionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Option " + optionId + " does not belong to question " + questionId);
        }
        return option;
    }

    private static Course courseOf(Question question) {
        return question.getLesson().getCourse();
    }
}
