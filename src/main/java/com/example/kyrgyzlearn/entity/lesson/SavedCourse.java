package com.example.kyrgyzlearn.entity.lesson;

import com.example.kyrgyzlearn.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Чужой (или свой) личный курс, который пользователь сохранил из поиска, чтобы проходить его из профиля.
// Официальные курсы не сохраняются: их нет в БД, и они и так всегда на главной.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SavedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Instant savedAt;
}
