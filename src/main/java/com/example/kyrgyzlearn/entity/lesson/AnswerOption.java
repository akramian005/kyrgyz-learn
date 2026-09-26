package com.example.kyrgyzlearn.entity.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    // порядок отображения варианта пользователю (не путать с correctPosition)
    private Integer sortOrder;

    // используется для SINGLE_CHOICE / MULTIPLE_CHOICE
    private boolean correct;

    // используется для ORDER: позиция варианта в правильной последовательности.
    // null — вариант не входит в правильный ответ (может быть "отвлекающим")
    private Integer correctPosition;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
