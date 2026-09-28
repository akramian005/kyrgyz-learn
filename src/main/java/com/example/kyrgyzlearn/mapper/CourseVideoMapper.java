package com.example.kyrgyzlearn.mapper;

import com.example.kyrgyzlearn.dto.video.CourseVideoDto;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseVideoMapper {

    private final FileStorageService fileStorageService;

    public CourseVideoDto toDto(CourseVideo video) {
        return new CourseVideoDto(
                video.getId(),
                video.getCourse().getId(),
                video.getPosition(),
                video.getTitle(),
                video.getDescription(),
                video.getVideoObjectKey() != null ? fileStorageService.getPresignedUrl(video.getVideoObjectKey()) : null
        );
    }
}
