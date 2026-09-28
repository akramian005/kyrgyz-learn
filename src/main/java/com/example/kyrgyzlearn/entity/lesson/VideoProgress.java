package com.example.kyrgyzlearn.entity.lesson;

import com.example.kyrgyzlearn.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Просмотренное пользователем видео курса (видео считается просмотренным, как только его открыли).
// Отдельно от LessonProgress: у уроков и видео своя нумерация id (урок -1 и видео -1 — разные вещи).
// videoId и courseId — просто числа, без внешних ключей: видео официальных курсов в БД нет.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_id"}))
@Getter
@Setter
@NoArgsConstructor
public class VideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Instant watchedAt;
}
