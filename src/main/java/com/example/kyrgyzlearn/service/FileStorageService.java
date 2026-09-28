package com.example.kyrgyzlearn.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioClient publicMinioClient;

    public FileStorageService(
            @Qualifier("internalMinioClient") MinioClient minioClient,
            @Qualifier("publicMinioClient") MinioClient publicMinioClient) {
        this.minioClient = minioClient;
        this.publicMinioClient = publicMinioClient;
    }

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url}")
    private String publicUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    // клиенты для подписи ссылок под разные адреса (localhost, IP компьютера в сети и т.п.)
    private final Map<String, MinioClient> presignClients = new ConcurrentHashMap<>();

    // --- Методы для картинок ---
    public String uploadImage(String folder, MultipartFile file) {
        return uploadFile(folder, file, "image/", 10 * 1024 * 1024); // Лимит 10 МБ: скриншоты с телефонов бывают тяжёлыми
    }

    // --- Методы для аудио ---
    public String uploadAudio(String folder, MultipartFile file) {
        return uploadFile(folder, file, "audio/", 20 * 1024 * 1024); // Лимит 20 МБ для аудио
    }

    // --- Методы для видео ---
    public String uploadVideo(String folder, MultipartFile file) {
        return uploadFile(folder, file, "video/", 500L * 1024 * 1024); // Лимит 500 МБ; такой же стоит в spring.servlet.multipart
    }

    // --- Единый внутренний метод загрузки ---
    private String uploadFile(String folder, MultipartFile file, String allowedMimePrefix, long maxSizeBytes) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(allowedMimePrefix)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file type. Expected: " + allowedMimePrefix + "*"
            );
        }

        try {
            ensureBucketExists();

            String extension = getExtension(file.getOriginalFilename());
            String objectKey = folder + "/" + UUID.randomUUID() + extension;

            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .stream(is, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }

            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO (bucket={}, folder={})", bucket, folder, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file");
        }
    }

    public String getPresignedUrl(String objectKey) {
        try {
            return presignClientForCurrentRequest().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object {}", objectKey, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate file URL");
        }
    }

    /*
     * Адрес в ссылке входит в её подпись, поэтому подменить его потом на клиенте нельзя.
     * Если MINIO_PUBLIC_HOST не задан (localhost), подписываем ссылку на тот адрес, по которому
     * клиент пришёл к бэкенду: телефон ходит на IP компьютера (192.168.x.x:8080) и получает
     * ссылку на 192.168.x.x:9000, а браузер на этом же компьютере — на localhost:9000.
     * Если MINIO_PUBLIC_HOST задан явно (например, домен на сервере), используется он.
     */
    private MinioClient presignClientForCurrentRequest() {
        URI configured = URI.create(publicUrl);
        if (!LOCAL_HOSTS.contains(configured.getHost())) {
            return publicMinioClient;
        }

        String requestHost = currentRequestHost();
        if (requestHost == null || LOCAL_HOSTS.contains(requestHost)) {
            return publicMinioClient;
        }

        String endpoint = configured.getScheme() + "://" + requestHost + ":" + configured.getPort();
        return presignClients.computeIfAbsent(endpoint, e -> MinioClient.builder()
                .endpoint(e)
                .credentials(accessKey, secretKey)
                .region("us-east-1") // с явным регионом подпись считается без запроса к MinIO
                .build());
    }

    private String currentRequestHost() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getServerName();
        }
        return null;
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete object {}", objectKey, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file");
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            log.info("Bucket '{}' not found, creating it", bucket);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}