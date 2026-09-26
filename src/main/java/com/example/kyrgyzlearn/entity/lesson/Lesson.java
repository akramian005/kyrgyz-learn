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
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    // порядок урока внутри курса
    private Integer position;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // вопросы и сообщения-объяснения одним упорядоченным списком
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<LessonItem> items = new ArrayList<>();
}
