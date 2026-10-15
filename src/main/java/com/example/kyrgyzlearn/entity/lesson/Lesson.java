package com.example.kyrgyzlearn.entity.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Lesson implements CourseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Markdown — может быть длинным, поэтому TEXT, а не varchar(255)
    @Column(columnDefinition = "TEXT")
    private String description;

    // порядок урока внутри курса — общий с видео (см. CourseItem)
    private Integer position;

    // юнит урока (CourseUnit.id). Просто id, без внешнего ключа: юниты и уроки удаляются вместе с курсом,
    // и порядок их удаления не важен
    private Long unitId;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // вопросы и сообщения-объяснения одним упорядоченным списком
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<LessonItem> items = new ArrayList<>();
}
