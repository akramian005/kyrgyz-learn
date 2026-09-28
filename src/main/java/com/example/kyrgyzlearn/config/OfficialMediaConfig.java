package com.example.kyrgyzlearn.config;

import com.example.kyrgyzlearn.content.OfficialContent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

// Раздаёт картинки, аудио и видео официальных курсов из official-content.media-dir по адресу /media/official/**.
// Spring сам отвечает на Range-запросы, поэтому видео можно перематывать.
@Configuration
@RequiredArgsConstructor
public class OfficialMediaConfig implements WebMvcConfigurer {

    public static final String URL_PREFIX = "/media/official";

    private final OfficialContent officialContent;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = officialContent.getMediaRoot().toUri().toString();
        registry.addResourceHandler(URL_PREFIX + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
    }
}
