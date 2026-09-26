package com.example.kyrgyzlearn.entity.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class LessonMessage extends LessonItem {

    @Column(columnDefinition = "TEXT")
    private String text;

    // ключи файлов в MinIO, оба опциональны
    private String imageObjectKey;
    private String audioObjectKey;
}
