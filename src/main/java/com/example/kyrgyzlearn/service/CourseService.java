package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.course.CourseCreateRequest;
import com.example.kyrgyzlearn.dto.course.CourseDto;
import com.example.kyrgyzlearn.dto.course.CourseUpdateRequest;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.repository.CourseRepository;
import com.example.kyrgyzlearn.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final CourseRepository courseRepository;
    private final LessonMediaCleaner lessonMediaCleaner;

    @Transactional(readOnly = true)
    public Page<CourseDto> getAllCourses(Pageable pageable, Authentication authentication) {
        if (isAdmin(authentication)) {
            return courseRepository.findAll(pageable).map(this::toDto);
        }
        // не-админ видит только OFFICIAL-курсы + свои собственные USER-курсы
        Long userId = currentUserIdOrNull(authentication);
        return courseRepository.findVisibleTo(userId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public CourseDto getCourseById(Long id, Authentication authentication) {
        Course course = findCourseOrThrow(id);
        requireCanView(course, authentication);
        return toDto(course);
    }

    @Transactional
    public CourseDto createCourse(CourseCreateRequest request, Authentication authentication) {
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setPosition(request.position() != null ? request.position() : 0);

        if (isAdmin(authentication)) {
            course.setType(CourseType.OFFICIAL);
            course.setOwner(null);
        } else {
            course.setType(CourseType.USER);
            course.setOwner(getCurrentUser(authentication));
        }

        return toDto(courseRepository.save(course));
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

        return toDto(courseRepository.save(course));
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

        courseRepository.delete(course); // каскадно удалит уроки с их содержимым и видео
    }

    // ---------- Права доступа ----------
    // Оба метода публичные — их же вызывает LessonService для проверки прав
    // на урок через его course, так как правило одно и то же: "не в своём курсе — нельзя".

    public void requireCanView(Course course, Authentication authentication) {
        if (course.getType() == CourseType.OFFICIAL) {
            return; // общие курсы видны всем
        }
        if (isAdmin(authentication)) {
            return;
        }
        Long userId = currentUserIdOrNull(authentication);
        Long ownerId = course.getOwner() != null ? course.getOwner().getId() : null;
        if (!Objects.equals(userId, ownerId)) {
            // 404, а не 403 — не палим само существование чужого курса
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + course.getId());
        }
    }

    public void requireCanModify(Course course, Authentication authentication) {
        if (course.getType() == CourseType.OFFICIAL) {
            if (!isAdmin(authentication)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can modify official courses");
            }
            return;
        }

        Long userId = currentUserIdOrNull(authentication);
        Long ownerId = course.getOwner() != null ? course.getOwner().getId() : null;
        if (!Objects.equals(userId, ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own courses");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ADMIN_AUTHORITY));
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

    // ---------- Вспомогательные ----------

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + id));
    }

    private CourseDto toDto(Course course) {
        return new CourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPosition(),
                course.getType(),
                course.getOwner() != null ? course.getOwner().getId() : null,
                course.getLessons().size(),
                course.getVideos().size()
        );
    }
}
