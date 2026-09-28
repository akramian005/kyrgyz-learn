package com.example.kyrgyzlearn.mapper;

import com.example.kyrgyzlearn.dto.video.CourseVideoDto;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseVideoMapper {

    private final MediaUrls mediaUrls;

    public CourseVideoDto toDto(CourseVideo video, boolean watched) {
        return new CourseVideoDto(
                video.getId(),
                video.getCourse().getId(),
                video.getPosition(),
                video.getTitle(),
                video.getDescription(),
                mediaUrls.url(video.getVideoObjectKey(), video.getCourse()),
                watched
        );
    }
}
