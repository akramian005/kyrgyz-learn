package com.example.kyrgyzlearn.mapper;

import com.example.kyrgyzlearn.config.OfficialMediaConfig;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// Ссылка для клиента на файл урока или видео курса:
// у официальных курсов — постоянная ссылка на файл на бэкенде (OfficialMediaConfig),
// у личных — подписанная ссылка на MinIO (живёт час).
@Component
@RequiredArgsConstructor
public class MediaUrls {

    private final FileStorageService fileStorageService;

    public String url(String key, Course course) {
        if (key == null) {
            return null;
        }
        if (course.getType() == CourseType.OFFICIAL) {
            // адрес берём из текущего запроса: телефон ходит на IP компьютера, браузер — на localhost
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(OfficialMediaConfig.URL_PREFIX)
                    .pathSegment(key.split("/"))
                    .build()
                    .encode()
                    .toUriString();
        }
        return fileStorageService.getPresignedUrl(key);
    }
}
