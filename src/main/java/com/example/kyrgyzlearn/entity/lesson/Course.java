package com.example.kyrgyzlearn.entity.lesson;

// TODO: поправь импорт под фактическое расположение твоей сущности User
import com.example.kyrgyzlearn.entity.user.User;
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
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseType type;

    // null для OFFICIAL, обязателен для USER — тот, кто может редактировать курс
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Lesson> lessons = new ArrayList<>();
}
