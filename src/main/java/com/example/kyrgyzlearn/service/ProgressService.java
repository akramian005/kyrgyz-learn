package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.content.OfficialContent;
import com.example.kyrgyzlearn.dto.progress.ProgressDto;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonProgress;
import com.example.kyrgyzlearn.entity.lesson.VideoProgress;
import com.example.kyrgyzlearn.repository.CourseRepository;
import com.example.kyrgyzlearn.repository.LessonProgressRepository;
import com.example.kyrgyzlearn.repository.UserRepository;
import com.example.kyrgyzlearn.repository.VideoProgressRepository;
import com.example.kyrgyzlearn.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// Прогресс пользователя: пройденные уроки и просмотренные видео.
// Курс пройден, когда пройдены все его уроки и просмотрены все видео (см. Completed).
// Зависит только от репозиториев и OfficialContent, а не от других сервисов, —
// им пользуются CourseService, LessonService и др., и так не будет циклической зависимости.
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final OfficialContent officialContent;

    // Что прошёл пользователь — чтобы проставить отметки в списках, не делая запрос на каждый курс
    public record Completed(Set<Long> lessonIds, Set<Long> videoIds) {

        public static final Completed NONE = new Completed(Set.of(), Set.of());

        // урок без шагов и видео без файла открыть нельзя — в прогрессе курса они не считаются,
        // иначе такой курс никогда не стал бы пройденным
        public static int totalIn(Course course) {
            return (int) (course.getLessons().stream().filter(ProgressService::countable).count()
                    + course.getVideos().stream().filter(ProgressService::countable).count());
        }

        public int doneIn(Course course) {
            return (int) (course.getLessons().stream().filter(l -> countable(l) && lessonIds.contains(l.getId())).count()
                    + course.getVideos().stream().filter(v -> countable(v) && videoIds.contains(v.getId())).count());
        }

        public boolean courseCompleted(Course course) {
            int total = totalIn(course);
            return total > 0 && doneIn(course) == total;
        }
    }

    // у анонима ничего не пройдено
    public Completed completed(Authentication authentication) {
        Long userId = currentUserIdOrNull(authentication);
        if (userId == null) {
            return Completed.NONE;
        }
        return new Completed(
                lessonProgressRepository.findLessonIdsByUserId(userId),
                videoProgressRepository.findVideoIdsByUserId(userId)
        );
    }

    // повторное прохождение ничего не меняет — урок уже отмечен
    @Transactional
    public void markCompleted(Lesson lesson, Authentication authentication) {
        Long userId = requireUserId(authentication);
        if (lessonProgressRepository.existsByUserIdAndLessonId(userId, lesson.getId())) {
            return;
        }

        LessonProgress progress = new LessonProgress();
        progress.setUser(userRepository.getReferenceById(userId));
        progress.setLessonId(lesson.getId());
        progress.setCourseId(lesson.getCourse().getId());
        progress.setCompletedAt(Instant.now());
        lessonProgressRepository.save(progress);
    }

    // видео открыли — считаем просмотренным; повторное открытие ничего не меняет
    @Transactional
    public void markWatched(CourseVideo video, Authentication authentication) {
        Long userId = requireUserId(authentication);
        if (videoProgressRepository.existsByUserIdAndVideoId(userId, video.getId())) {
            return;
        }

        VideoProgress progress = new VideoProgress();
        progress.setUser(userRepository.getReferenceById(userId));
        progress.setVideoId(video.getId());
        progress.setCourseId(video.getCourse().getId());
        progress.setWatchedAt(Instant.now());
        videoProgressRepository.save(progress);
    }

    // считаем по текущему составу курсов: удалённые курсы и уроки (например, убранные из файлов
    // официального курса) не учитываются, а новый урок или видео в курсе снимает с курса отметку «пройден»
    @Transactional(readOnly = true)
    public ProgressDto getProgress(Authentication authentication) {
        Long userId = requireUserId(authentication);
        Completed completed = completed(authentication);

        Set<Long> courseIds = new HashSet<>(lessonProgressRepository.findCourseIdsByUserId(userId));
        courseIds.addAll(videoProgressRepository.findCourseIdsByUserId(userId));

        int lessons = 0;
        int courses = 0;
        for (Long courseId : courseIds) {
            Optional<Course> course = officialContent.findCourse(courseId).or(() -> courseRepository.findById(courseId));
            if (course.isEmpty()) {
                continue;
            }
            lessons += (int) course.get().getLessons().stream()
                    .filter(lesson -> completed.lessonIds().contains(lesson.getId()))
                    .count();
            if (completed.courseCompleted(course.get())) {
                courses++;
            }
        }
        return new ProgressDto(lessons, courses);
    }

    // уроки, видео и курсы из БД удаляются вместе со своим прогрессом
    public void deleteForLesson(Long lessonId) {
        lessonProgressRepository.deleteByLessonId(lessonId);
    }

    public void deleteForVideo(Long videoId) {
        videoProgressRepository.deleteByVideoId(videoId);
    }

    public void deleteForCourse(Long courseId) {
        lessonProgressRepository.deleteByCourseId(courseId);
        videoProgressRepository.deleteByCourseId(courseId);
    }

    private static boolean countable(Lesson lesson) {
        return !lesson.getItems().isEmpty();
    }

    private static boolean countable(CourseVideo video) {
        return video.getVideoObjectKey() != null;
    }

    private Long requireUserId(Authentication authentication) {
        Long userId = currentUserIdOrNull(authentication);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userId;
    }

    private Long currentUserIdOrNull(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return null;
        }
        return principal.getId();
    }
}
