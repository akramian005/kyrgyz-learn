package com.example.kyrgyzlearn.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.public-url}")
    private String publicUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    @Qualifier("internalMinioClient")
    public MinioClient minioClient() {
        log.info("Initializing internal MinioClient with endpoint: {}", url);
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .region("us-east-1")
                .build();
    }

    @Bean
    @Qualifier("publicMinioClient")
    public MinioClient publicMinioClient() {
        log.info("Initializing public MinioClient with endpoint: {}", publicUrl);
        return MinioClient.builder()
                .endpoint(publicUrl)
                .credentials(accessKey, secretKey)
                .region("us-east-1")
                .build();
    }
}
