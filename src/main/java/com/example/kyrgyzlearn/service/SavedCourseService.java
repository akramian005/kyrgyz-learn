package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.course.CourseDto;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.SavedCourse;
import com.example.kyrgyzlearn.mapper.CourseMapper;
import com.example.kyrgyzlearn.repository.SavedCourseRepository;
import com.example.kyrgyzlearn.repository.UserRepository;
import com.example.kyrgyzlearn.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

// Сохранённые курсы текущего пользователя: добавляются из поиска, проходятся из профиля
@Service
@RequiredArgsConstructor
public class SavedCourseService {

    private final SavedCourseRepository savedCourseRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final CourseMapper courseMapper;

    // последние сохранённые — первыми
    @Transactional(readOnly = true)
    public List<CourseDto> getSavedCourses(Authentication authentication) {
        return savedCourseRepository.findByUserIdOrderBySavedAtDesc(currentUserId(authentication)).stream()
                .map(saved -> courseMapper.toDto(saved.getCourse(), true))
                .toList();
    }

    // повторное сохранение ничего не меняет — кнопку можно нажать дважды без ошибки
    @Transactional
    public void saveCourse(Long courseId, Authentication authentication) {
        Long userId = currentUserId(authentication);
        Course course = courseService.findCourseOrThrow(courseId);
        if (course.getType() == CourseType.OFFICIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Official courses cannot be saved: they are always on the home page");
        }
        if (savedCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return;
        }

        SavedCourse saved = new SavedCourse();
        saved.setUser(userRepository.getReferenceById(userId));
        saved.setCourse(course);
        saved.setSavedAt(Instant.now());
        savedCourseRepository.save(saved);
    }

    @Transactional
    public void unsaveCourse(Long courseId, Authentication authentication) {
        savedCourseRepository.deleteByUserIdAndCourseId(currentUserId(authentication), courseId);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return principal.getId();
    }
}
