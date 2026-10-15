package com.example.kyrgyzlearn.entity.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Видео в курсе (не в уроке): стоит в общем списке курса между уроками
@Entity
@Getter
@Setter
@NoArgsConstructor
public class CourseVideo implements CourseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // Markdown — может быть длинным, поэтому TEXT, а не varchar(255)
    @Column(columnDefinition = "TEXT")
    private String description;

    // ключ файла в MinIO; null — видео создано, но файл ещё не загружен
    private String videoObjectKey;

    // порядок внутри курса — общий с уроками
    @Column(nullable = false)
    private Integer position;

    // юнит видео (CourseUnit.id), как у Lesson
    private Long unitId;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
