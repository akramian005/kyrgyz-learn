package com.example.kyrgyzlearn.entity.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Юнит (этап) курса: группа уроков и видео, на экране курса — один кружок «змейки».
// Уроки и видео ссылаются на юнит полем unitId (см. CourseItem), порядок юнитов — position.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class CourseUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private Integer position;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
