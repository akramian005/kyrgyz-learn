package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.content.OfficialContent;
import com.example.kyrgyzlearn.dto.video.CourseVideoCreateRequest;
import com.example.kyrgyzlearn.dto.video.CourseVideoDto;
import com.example.kyrgyzlearn.dto.video.CourseVideoUpdateRequest;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.mapper.CourseVideoMapper;
import com.example.kyrgyzlearn.repository.CourseVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// Видео в курсе: стоят в общем списке курса между уроками
@Service
@RequiredArgsConstructor
public class CourseVideoService {

    private final CourseVideoRepository courseVideoRepository;
    private final OfficialContent officialContent;
    private final CourseService courseService;
    private final CourseItemService courseItemService;
    private final FileStorageService fileStorageService;
    private final LessonMediaCleaner lessonMediaCleaner;
    private final CourseVideoMapper courseVideoMapper;

    @Transactional(readOnly = true)
    public CourseVideoDto getVideo(Long courseId, Long videoId, Authentication authentication) {
        CourseVideo video = findVideoOrThrow(courseId, videoId);
        return courseVideoMapper.toDto(video);
    }

    // файл загружается отдельным запросом (uploadVideoFile), как картинки и аудио у сообщений
    @Transactional
    public CourseVideoDto createVideo(Long courseId, CourseVideoCreateRequest request, Authentication authentication) {
        Course course = courseService.findCourseOrThrow(courseId);
        courseService.requireCanModify(course, authentication);

        CourseVideo video = new CourseVideo();
        video.setCourse(course);
        video.setTitle(request.title());
        video.setDescription(request.description());
        courseItemService.insert(course, video, request.position());

        CourseVideo saved = courseVideoRepository.save(video);
        course.getVideos().add(saved);

        return courseVideoMapper.toDto(saved);
    }

    @Transactional
    public CourseVideoDto updateVideo(Long courseId, Long videoId, CourseVideoUpdateRequest request, Authentication authentication) {
        CourseVideo video = findVideoOrThrow(courseId, videoId);
        courseService.requireCanModify(video.getCourse(), authentication);

        if (request.title().isPresent()) {
            String newTitle = request.title().get();
            if (newTitle == null || newTitle.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video title cannot be empty");
            }
            video.setTitle(newTitle);
        }
        if (request.description().isPresent()) {
            video.setDescription(request.description().get());
        }

        return courseVideoMapper.toDto(courseVideoRepository.save(video));
    }

    @Transactional
    public void deleteVideo(Long courseId, Long videoId, Authentication authentication) {
        CourseVideo video = findVideoOrThrow(courseId, videoId);
        courseService.requireCanModify(video.getCourse(), authentication);

        lessonMediaCleaner.deleteVideoMedia(video);
        video.getCourse().getVideos().remove(video); // orphanRemoval удалит запись из БД
    }

    @Transactional
    public String uploadVideoFile(Long courseId, Long videoId, MultipartFile file, Authentication authentication) {
        CourseVideo video = findVideoOrThrow(courseId, videoId);
        courseService.requireCanModify(video.getCourse(), authentication);

        if (video.getVideoObjectKey() != null) {
            fileStorageService.delete(video.getVideoObjectKey());
        }

        String folder = "courses/" + courseId + "/videos/" + videoId;
        String objectKey = fileStorageService.uploadVideo(folder, file);
        video.setVideoObjectKey(objectKey);
        courseVideoRepository.save(video);

        return fileStorageService.getPresignedUrl(objectKey);
    }

    @Transactional
    public void deleteVideoFile(Long courseId, Long videoId, Authentication authentication) {
        CourseVideo video = findVideoOrThrow(courseId, videoId);
        courseService.requireCanModify(video.getCourse(), authentication);

        if (video.getVideoObjectKey() != null) {
            fileStorageService.delete(video.getVideoObjectKey());
            video.setVideoObjectKey(null);
        }
    }

    // ---------- Вспомогательные методы ----------

    private CourseVideo findVideoOrThrow(Long courseId, Long videoId) {
        CourseVideo video = officialContent.findVideo(videoId)
                .or(() -> courseVideoRepository.findById(videoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found: " + videoId));

        if (!video.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Video " + videoId + " does not belong to course " + courseId);
        }
        return video;
    }
}
