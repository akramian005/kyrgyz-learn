package com.example.kyrgyzlearn.config;

import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.user.Role;
import com.example.kyrgyzlearn.entity.user.User;
import com.example.kyrgyzlearn.repository.CourseRepository;
import com.example.kyrgyzlearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Тестовые пользователи с личными курсами — чтобы было что листать в «Поиске» (пагинация по 20).
// Создаются один раз при старте: если первый тестовый пользователь уже есть, ничего не делаем.
// Включается dev-data.enabled (см. application.yaml); на проде выключить.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dev-data.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    public static final String PASSWORD = "password123";

    private record TestUser(String email, String name, List<String> courses) {
    }

    private static final List<TestUser> USERS = List.of(
            new TestUser("aibek@test.kg", "Айбек", List.of("Глаголы движения", "Числа от 1 до 100", "Дни недели", "Приветствия и прощания")),
            new TestUser("aizada@test.kg", "Айзада", List.of("Семья и родственники", "Цвета", "Глаголы прошедшего времени", "В магазине")),
            new TestUser("bakyt@test.kg", "Бакыт", List.of("Еда и напитки", "Части тела", "Погода и времена года", "Вопросительные слова")),
            new TestUser("gulnara@test.kg", "Гульнара", List.of("Профессии", "Глаголы настоящего времени", "Животные", "Город и транспорт")),
            new TestUser("daniyar@test.kg", "Данияр", List.of("Кыргызские пословицы", "Одежда", "Падежи: родительный", "Эпос «Манас»: основы")),
            new TestUser("elnura@test.kg", "Элнура", List.of("Время и часы", "Дом и квартира", "Прилагательные", "У врача")),
            new TestUser("nurlan@test.kg", "Нурлан", List.of("Спорт и игры", "Падежи: дательный", "Глаголы желания", "Путешествие по Иссык-Кулю")),
            // без имени — в поиске автор покажется как «пользователь #id»
            new TestUser("noname@test.kg", null, List.of("Алфавит и произношение", "Местоимения", "Отрицание", "Разговор по телефону"))
    );

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(USERS.get(0).email()).isPresent()) {
            return;
        }

        String passwordHash = passwordEncoder.encode(PASSWORD);
        int courseCount = 0;
        for (TestUser testUser : USERS) {
            User user = userRepository.save(User.builder()
                    .email(testUser.email())
                    .name(testUser.name())
                    .password(passwordHash)
                    .role(Role.USER)
                    .build());

            for (String title : testUser.courses()) {
                Course course = new Course();
                course.setTitle(title);
                course.setDescription("Тестовый курс для проверки поиска и пагинации");
                course.setPosition(0);
                course.setType(CourseType.USER);
                course.setOwner(user);
                courseRepository.save(course);
                courseCount++;
            }
        }
        log.info("Dev data: created {} test users and {} courses (password: {})", USERS.size(), courseCount, PASSWORD);
    }
}
