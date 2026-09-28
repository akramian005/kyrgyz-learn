package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.content.OfficialContent;
import com.example.kyrgyzlearn.dto.course.CourseCreateRequest;
import com.example.kyrgyzlearn.dto.course.CourseDto;
import com.example.kyrgyzlearn.dto.course.CourseUpdateRequest;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.mapper.CourseMapper;
import com.example.kyrgyzlearn.repository.CourseRepository;
import com.example.kyrgyzlearn.repository.SavedCourseRepository;
import com.example.kyrgyzlearn.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SavedCourseRepository savedCourseRepository;
    private final OfficialContent officialContent;
    private final LessonMediaCleaner lessonMediaCleaner;
    private final CourseMapper courseMapper;
    private final ProgressService progressService;

    // Главная и «Прочее»: сначала официальные курсы (из файлов, в своём порядке), затем свои USER-курсы из БД.
    // Чужие личные курсы здесь не показываются — их находят через search.
    @Transactional(readOnly = true)
    public Page<CourseDto> getAllCourses(Pageable pageable, Authentication authentication) {
        Long userId = currentUserIdOrNull(authentication);
        Set<Long> savedIds = savedCourseIds(userId);
        ProgressService.Completed completed = progressService.completed(authentication);

        List<CourseDto> all = new ArrayList<>();
        officialContent.getCourses().forEach(course -> all.add(toDto(course, savedIds, completed)));
        if (userId != null) {
            courseRepository.findByOwnerId(userId, pageable.getSort()).forEach(course -> all.add(toDto(course, savedIds, completed)));
        }
        return Pages.of(all, pageable);
    }

    // «Поиск»: личные курсы всех пользователей по названию, постранично (официальные — на главной)
    @Transactional(readOnly = true)
    public Page<CourseDto> searchCourses(String query, Pageable pageable, Authentication authentication) {
        Set<Long> savedIds = savedCourseIds(currentUserIdOrNull(authentication));
        ProgressService.Completed completed = progressService.completed(authentication);
        String title = query != null ? query.trim() : "";
        return courseRepository.findByTypeAndTitleContainingIgnoreCase(CourseType.USER, title, pageable)
                .map(course -> toDto(course, savedIds, completed));
    }

    // любой курс открыт для просмотра всем: официальные — на главной, личные — через поиск
    @Transactional(readOnly = true)
    public CourseDto getCourseById(Long id, Authentication authentication) {
        return toDto(findCourseOrThrow(id), savedCourseIds(currentUserIdOrNull(authentication)),
                progressService.completed(authentication));
    }

    // через API создаются только личные курсы (у админа тоже) — официальные лежат в content/official
    @Transactional
    public CourseDto createCourse(CourseCreateRequest request, Authentication authentication) {
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setPosition(request.position() != null ? request.position() : 0);
        course.setType(CourseType.USER);
        course.setOwner(getCurrentUser(authentication));

        return courseMapper.toDto(courseRepository.save(course), false, ProgressService.Completed.NONE);
    }

    @Transactional
    public CourseDto updateCourse(Long id, CourseUpdateRequest request, Authentication authentication) {
        Course course = findCourseOrThrow(id);
        requireCanModify(course, authentication);

        if (request.title().isPresent()) {
            String newTitle = request.title().get();
            if (newTitle == null || newTitle.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course title cannot be empty");
            }
            course.setTitle(newTitle);
        }
        if (request.description().isPresent()) {
            course.setDescription(request.description().get());
        }
        if (request.position().isPresent()) {
            course.setPosition(request.position().get());
        }

        Course saved = courseRepository.save(course);
        return toDto(saved, savedCourseIds(currentUserIdOrNull(authentication)),
                progressService.completed(authentication));
    }

    @Transactional
    public void deleteCourse(Long id, Authentication authentication) {
        Course course = findCourseOrThrow(id);
        requireCanModify(course, authentication);

        // файлы в MinIO каскад не трогает — чистим их вручную
        for (Lesson lesson : course.getLessons()) {
            lessonMediaCleaner.deleteLessonMedia(lesson);
        }
        for (CourseVideo video : course.getVideos()) {
            lessonMediaCleaner.deleteVideoMedia(video);
        }

        savedCourseRepository.deleteByCourseId(course.getId()); // курс мог быть в сохранённых у других
        progressService.deleteForCourse(course.getId());
        courseRepository.delete(course); // каскадно удалит уроки с их содержимым и видео
    }

    // ---------- Права доступа ----------
    // Публичный — его же вызывают LessonService, QuestionService и др. для проверки прав
    // на урок через его course, так как правило одно и то же: "не в своём курсе — нельзя".

    // официальные курсы только для чтения — даже для админа: они не в БД, а в файлах
    // (заодно это гарантирует, что их объекты из OfficialContent никогда не попадут в save/delete)
    public void requireCanModify(Course course, Authentication authentication) {
        if (course.getType() == CourseType.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Official courses are read-only: they are edited in content/official on the backend");
        }

        Long userId = currentUserIdOrNull(authentication);
        Long ownerId = course.getOwner() != null ? course.getOwner().getId() : null;
        if (!Objects.equals(userId, ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own courses");
        }
    }

    private Long currentUserIdOrNull(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return null;
        }
        return principal.getId();
    }

    private com.example.kyrgyzlearn.entity.user.User getCurrentUser(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return principal.getUser();
    }

    // ---------- Поиск курса ----------

    // публичный — им же ищут курс LessonService, CourseVideoService, CourseItemService и SavedCourseService
    public Course findCourseOrThrow(Long id) {
        return officialContent.findCourse(id)
                .or(() -> courseRepository.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + id));
    }

    // ---------- Вспомогательные ----------

    private Set<Long> savedCourseIds(Long userId) {
        return userId != null ? savedCourseRepository.findCourseIdsByUserId(userId) : Set.of();
    }

    private CourseDto toDto(Course course, Set<Long> savedIds, ProgressService.Completed completed) {
        return courseMapper.toDto(course, savedIds.contains(course.getId()), completed);
    }
}
