package com.example.kyrgyzlearn.entity.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "item_type")
@Getter
@Setter
@NoArgsConstructor
public abstract class LessonItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // порядок элемента внутри урока (вопрос или сообщение идут вперемешку)
    @Column(nullable = false)
    private Integer position;

    @ManyToOne
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
}
