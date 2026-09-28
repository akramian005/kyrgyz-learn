package com.example.kyrgyzlearn.content;

import com.example.kyrgyzlearn.content.OfficialCourseFile.LessonFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.MessageFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.OptionFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.QuestionFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.VideoFile;
import com.example.kyrgyzlearn.entity.lesson.AnswerOption;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonItem;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Официальные курсы: читаются из JSON-файлов при старте и живут только в памяти, в БД их нет.
// Отдаются теми же сущностями Course/Lesson/..., что и курсы из БД, чтобы сервисы и мапперы
// работали с ними одинаково. Эти объекты никогда не сохраняются: requireCanModify в CourseService
// запрещает любые изменения OFFICIAL-курсов. Медиа лежат файлами в official-content.media-dir
// (раздаёт OfficialMediaConfig). Ошибка в JSON или отсутствующий файл — приложение не стартует.
@Slf4j
@Component
public class OfficialContent {

    private final String location;
    private final Path mediaRoot;
    // пути к медиа из всех файлов — проверяем разом после загрузки, чтобы показать все пропавшие файлы
    private final List<String> mediaPaths = new ArrayList<>();

    private final Map<Long, Course> courses = new HashMap<>();
    private final Map<Long, Lesson> lessons = new HashMap<>();
    private final Map<Long, CourseVideo> videos = new HashMap<>();
    private List<Course> orderedCourses = List.of();

    // id вопросов, сообщений и вариантов не пишутся в файлах — раздаём по порядку загрузки
    private long nextItemId = -1;
    private long nextOptionId = -1;

    // location настраивается только ради тестов — в приложении всегда content/official
    public OfficialContent(
            @Value("${official-content.location:classpath*:content/official/*.json}") String location,
            @Value("${official-content.media-dir}") String mediaDir) {
        this.location = location;
        this.mediaRoot = Path.of(mediaDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void load() throws IOException {
        JsonMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // опечатка в поле — ошибка, а не молча null
                .build();

        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(location);
        // сортируем по имени, чтобы авто-id не зависели от порядка, в котором их вернула файловая система
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

        List<Course> loaded = new ArrayList<>();
        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                loaded.add(buildCourse(mapper.readValue(in, OfficialCourseFile.class)));
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Invalid official course file " + resource.getFilename() + ": " + e.getMessage(), e);
            }
        }

        loaded.sort(Comparator.comparing(Course::getPosition)); // стабильная сортировка: при равных — по имени файла
        orderedCourses = List.copyOf(loaded);
        requireMediaFilesExist();
        log.info("Loaded {} official courses ({} lessons, {} videos), media from {}",
                courses.size(), lessons.size(), videos.size(), mediaRoot);
    }

    // папка с медиа официальных курсов
    public Path getMediaRoot() {
        return mediaRoot;
    }

    public List<Course> getCourses() {
        return orderedCourses;
    }

    public Optional<Course> findCourse(Long id) {
        return Optional.ofNullable(courses.get(id));
    }

    public Optional<Lesson> findLesson(Long id) {
        return Optional.ofNullable(lessons.get(id));
    }

    public Optional<CourseVideo> findVideo(Long id) {
        return Optional.ofNullable(videos.get(id));
    }

    // ---------- Сборка сущностей из файла ----------

    private Course buildCourse(OfficialCourseFile file) {
        Course course = new Course();
        course.setId(requireNewId(file.id(), "course", courses));
        course.setTitle(requireText(file.title(), "course " + file.id() + " title"));
        course.setDescription(file.description());
        course.setPosition(file.position() != null ? file.position() : 0);
        course.setType(CourseType.OFFICIAL);

        List<OfficialCourseFile.Item> items = listOrEmpty(file.items());
        for (int i = 0; i < items.size(); i++) {
            switch (items.get(i)) {
                case LessonFile l -> course.getLessons().add(buildLesson(l, course, i));
                case VideoFile v -> course.getVideos().add(buildVideo(v, course, i));
            }
        }

        courses.put(course.getId(), course);
        return course;
    }

    private Lesson buildLesson(LessonFile file, Course course, int position) {
        Lesson lesson = new Lesson();
        lesson.setId(requireNewId(file.id(), "lesson", lessons));
        lesson.setTitle(requireText(file.title(), "lesson " + file.id() + " title"));
        lesson.setDescription(file.description());
        lesson.setPosition(position);
        lesson.setCourse(course);

        List<OfficialCourseFile.LessonItem> items = listOrEmpty(file.items());
        for (int i = 0; i < items.size(); i++) {
            LessonItem item = switch (items.get(i)) {
                case MessageFile m -> buildMessage(m);
                case QuestionFile q -> buildQuestion(q, "lesson " + file.id() + " question " + i);
            };
            item.setId(nextItemId--);
            item.setPosition(i);
            item.setLesson(lesson);
            lesson.getItems().add(item);
        }

        lessons.put(lesson.getId(), lesson);
        return lesson;
    }

    private CourseVideo buildVideo(VideoFile file, Course course, int position) {
        CourseVideo video = new CourseVideo();
        video.setId(requireNewId(file.id(), "video", videos));
        video.setTitle(requireText(file.title(), "video " + file.id() + " title"));
        video.setDescription(file.description());
        video.setVideoObjectKey(media(file.video()));
        video.setPosition(position);
        video.setCourse(course);

        videos.put(video.getId(), video);
        return video;
    }

    private LessonMessage buildMessage(MessageFile file) {
        LessonMessage message = new LessonMessage();
        message.setText(file.text());
        message.setImageObjectKey(media(file.image()));
        message.setAudioObjectKey(media(file.audio()));
        return message;
    }

    private Question buildQuestion(QuestionFile file, String where) {
        if (file.type() == null) {
            throw new IllegalStateException(where + ": type is required");
        }

        Question question = new Question();
        question.setText(requireText(file.text(), where + " text"));
        question.setAudioObjectKey(media(file.audio()));
        question.setType(file.type());

        List<OptionFile> options = listOrEmpty(file.options());
        for (int i = 0; i < options.size(); i++) {
            OptionFile optionFile = options.get(i);
            AnswerOption option = new AnswerOption();
            option.setId(nextOptionId--);
            option.setText(optionFile.text());
            option.setSortOrder(i);
            option.setCorrect(Boolean.TRUE.equals(optionFile.correct()));
            option.setCorrectPosition(optionFile.correctPosition());
            option.setQuestion(question);
            question.getOptions().add(option);
        }
        return question;
    }

    // ---------- Проверки ----------

    // путь к медиа хранится в тех же полях *ObjectKey, что и ключ MinIO у личных курсов;
    // какой это из двух, MediaUrls решает по типу курса
    private String media(String path) {
        String value = blankToNull(path);
        if (value != null) {
            mediaPaths.add(value);
        }
        return value;
    }

    private void requireMediaFilesExist() {
        List<String> missing = new ArrayList<>();
        for (String path : mediaPaths) {
            Path file = mediaRoot.resolve(path).normalize();
            // «../» в пути не должен выводить за пределы папки
            if (!file.startsWith(mediaRoot) || !Files.isRegularFile(file)) {
                missing.add(path);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Official media files not found in " + mediaRoot + ": " + missing);
        }
    }

    // отрицательный id не пересекается с IDENTITY-id из БД, поэтому сервисы ищут сначала здесь, потом в БД
    private static long requireNewId(Long id, String what, Map<Long, ?> existing) {
        if (id == null || id >= 0) {
            throw new IllegalStateException(what + " id must be a negative number, got " + id);
        }
        if (existing.containsKey(id)) {
            throw new IllegalStateException("duplicate " + what + " id " + id);
        }
        return id;
    }

    private static String requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(what + " is required");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static <T> List<T> listOrEmpty(List<T> list) {
        return list != null ? list : List.of();
    }
}
