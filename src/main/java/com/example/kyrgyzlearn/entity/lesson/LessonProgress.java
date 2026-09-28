package com.example.kyrgyzlearn.entity.lesson;

import com.example.kyrgyzlearn.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Пройденный пользователем урок. lessonId и courseId — просто числа, без внешних ключей:
// у официальных курсов (отрицательные id) уроков в БД нет, они читаются из файлов (см. OfficialContent).
// Курс считается пройденным, когда пройдены все его уроки (видео не в счёт) — см. ProgressService.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
@Getter
@Setter
@NoArgsConstructor
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    // курс урока — чтобы считать пройденные курсы и чистить прогресс при удалении курса
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Instant completedAt;
}
