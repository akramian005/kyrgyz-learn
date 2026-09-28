package com.example.kyrgyzlearn.content;

import com.example.kyrgyzlearn.content.OfficialCourseFile.LessonFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.MessageFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.OptionFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.QuestionFile;
import com.example.kyrgyzlearn.content.OfficialCourseFile.VideoFile;
import com.example.kyrgyzlearn.entity.lesson.AnswerOption;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseItem;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonItem;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

// Официальные курсы: читаются из JSON-файлов папки official-content.dir при старте и живут в памяти, в БД их нет.
// Отдаются теми же сущностями Course/Lesson/..., что и курсы из БД, чтобы сервисы и мапперы
// работали с ними одинаково. Админ редактирует их через API: сервисы меняют объекты в памяти
// и вызывают save(course) — курс целиком перезаписывается в свой файл (в git видно, что поменялось).
// Медиа лежат файлами в official-content.media-dir (раздаёт OfficialMediaConfig).
// Ошибка в JSON или отсутствующий файл медиа при старте — приложение не стартует.
@Slf4j
@Component
public class OfficialContent {

    // одинаковый порядок с CourseItemService: по позиции, при равной — сначала урок, потом по id
    private static final Comparator<CourseItem> ITEM_ORDER = Comparator
            .comparing(CourseItem::getPosition, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(item -> item instanceof Lesson ? 0 : 1)
            .thenComparing(CourseItem::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final Path dir;
    private final Path mediaRoot;
    // тот же вид, что у файлов, написанных руками: "id": -1 и каждый элемент массива с новой строки —
    // чтобы после правки в приложении git показывал только реально изменённые строки
    private static final DefaultPrettyPrinter PRINTER = new DefaultPrettyPrinter(
            Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER))
            .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // опечатка в поле — ошибка, а не молча null
            .build();

    // курсы в порядке position; сами объекты меняют сервисы, список — только add/remove
    private final List<Course> courses = new ArrayList<>();
    // файл каждого курса: в него пишет save()
    private final Map<Long, Path> files = new HashMap<>();
    // пути к медиа из всех файлов — проверяем разом после загрузки, чтобы показать все пропавшие файлы
    private final List<String> mediaPaths = new ArrayList<>();

    // id вопросов, сообщений и вариантов не пишутся в файлах — раздаём по порядку при загрузке и создании
    private long nextItemId = -1;
    private long nextOptionId = -1;

    public OfficialContent(
            @Value("${official-content.dir}") String dir,
            @Value("${official-content.media-dir}") String mediaDir) {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
        this.mediaRoot = Path.of(mediaDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void load() throws IOException {
        List<Path> jsonFiles;
        try (Stream<Path> list = Files.list(dir)) {
            // сортируем по имени, чтобы авто-id не зависели от порядка, в котором их вернула файловая система
            jsonFiles = list.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }

        for (Path file : jsonFiles) {
            try (InputStream in = Files.newInputStream(file)) {
                Course course = buildCourse(mapper.readValue(in, OfficialCourseFile.class));
                courses.add(course);
                files.put(course.getId(), file);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Invalid official course file " + file.getFileName() + ": " + e.getMessage(), e);
            }
        }

        sortCourses();
        requireMediaFilesExist();
        log.info("Loaded {} official courses from {}, media from {}", courses.size(), dir, mediaRoot);
    }

    // ---------- Чтение ----------

    // папка с медиа официальных курсов
    public Path getMediaRoot() {
        return mediaRoot;
    }

    public synchronized List<Course> getCourses() {
        return List.copyOf(courses);
    }

    public synchronized Optional<Course> findCourse(Long id) {
        return courses.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public synchronized Optional<Lesson> findLesson(Long id) {
        return lessons().filter(l -> l.getId().equals(id)).findFirst();
    }

    public synchronized Optional<CourseVideo> findVideo(Long id) {
        return courses.stream().flatMap(c -> c.getVideos().stream()).filter(v -> v.getId().equals(id)).findFirst();
    }

    public synchronized Optional<Question> findQuestion(Long id) {
        return items().filter(i -> i instanceof Question && i.getId().equals(id)).map(Question.class::cast).findFirst();
    }

    public synchronized Optional<LessonMessage> findMessage(Long id) {
        return items().filter(i -> i instanceof LessonMessage && i.getId().equals(id)).map(LessonMessage.class::cast).findFirst();
    }

    public synchronized Optional<AnswerOption> findOption(Long id) {
        return items().filter(Question.class::isInstance)
                .flatMap(q -> ((Question) q).getOptions().stream())
                .filter(o -> o.getId().equals(id))
                .findFirst();
    }

    // ---------- Запись (редактирование админом) ----------

    // новый курс: свой файл course-<id>.json и своя папка медиа course-<id>/
    public synchronized void add(Course course) {
        course.setId(Math.min(minId(courses.stream().map(Course::getId)), 0) - 1);
        course.setType(CourseType.OFFICIAL);
        courses.add(course);
        files.put(course.getId(), dir.resolve("course" + course.getId() + ".json"));
        save(course);
    }

    public synchronized void remove(Course course) {
        courses.remove(course);
        Path file = files.remove(course.getId());
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete official course file " + file, e);
        }
    }

    // раздаёт id новым урокам, видео, вопросам, сообщениям и вариантам и перезаписывает файл курса
    public synchronized void save(Course course) {
        assignIds(course);
        sortCourses(); // position курса мог поменяться

        Path file = files.get(course.getId());
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            // пишем во временный файл и подменяем — при сбое старый файл останется целым
            Files.writeString(tmp, mapper.writer().with(PRINTER).writeValueAsString(toFile(course)) + "\n");
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write official course file " + file, e);
        }
    }

    // ---------- Медиа ----------

    // файл кладётся в папку курса (для kyrgyz.json — official-media/kyrgyz/); возвращает путь относительно media-dir
    public String storeMedia(Course course, String extension, InputStream content) {
        String stem = files.get(course.getId()).getFileName().toString().replaceFirst("\\.json$", "");
        String path = stem + "/" + UUID.randomUUID() + extension;
        Path target = resolveMedia(path);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store official media " + path, e);
        }
        return path;
    }

    public void deleteMedia(String path) {
        try {
            Files.deleteIfExists(resolveMedia(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete official media " + path, e);
        }
    }

    // ---------- Сборка сущностей из файла ----------

    private Course buildCourse(OfficialCourseFile file) {
        Course course = new Course();
        course.setId(requireNewId(file.id(), "course", courses.stream().map(Course::getId)));
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
        return course;
    }

    private Lesson buildLesson(LessonFile file, Course course, int position) {
        Lesson lesson = new Lesson();
        lesson.setId(requireNewId(file.id(), "lesson", Stream.concat(lessons(), course.getLessons().stream()).map(Lesson::getId)));
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
        return lesson;
    }

    private CourseVideo buildVideo(VideoFile file, Course course, int position) {
        CourseVideo video = new CourseVideo();
        video.setId(requireNewId(file.id(), "video",
                Stream.concat(courses.stream(), Stream.of(course)).flatMap(c -> c.getVideos().stream()).map(CourseVideo::getId)));
        video.setTitle(requireText(file.title(), "video " + file.id() + " title"));
        video.setDescription(file.description());
        video.setVideoObjectKey(media(file.video()));
        video.setPosition(position);
        video.setCourse(course);
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

    // ---------- Сущности -> файл ----------

    private OfficialCourseFile toFile(Course course) {
        List<CourseItem> ordered = new ArrayList<>(course.getLessons());
        ordered.addAll(course.getVideos());
        ordered.sort(ITEM_ORDER);

        List<OfficialCourseFile.Item> items = ordered.stream()
                .map(item -> item instanceof Lesson lesson ? toFile(lesson) : toFile((CourseVideo) item))
                .toList();
        return new OfficialCourseFile(course.getId(), course.getTitle(), blankToNull(course.getDescription()),
                course.getPosition(), items);
    }

    private OfficialCourseFile.Item toFile(Lesson lesson) {
        List<OfficialCourseFile.LessonItem> items = lesson.getItems().stream()
                .sorted(Comparator.comparing(LessonItem::getPosition, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> switch (item) {
                    case Question q -> (OfficialCourseFile.LessonItem) toFile(q);
                    case LessonMessage m -> new MessageFile(m.getText(), m.getImageObjectKey(), m.getAudioObjectKey());
                    default -> throw new IllegalStateException("Unknown lesson item: " + item.getClass());
                })
                .toList();
        return new LessonFile(lesson.getId(), lesson.getTitle(), blankToNull(lesson.getDescription()), items);
    }

    private OfficialCourseFile.Item toFile(CourseVideo video) {
        return new VideoFile(video.getId(), video.getTitle(), blankToNull(video.getDescription()), video.getVideoObjectKey());
    }

    private QuestionFile toFile(Question question) {
        boolean order = question.getType() == QuestionType.ORDER;
        List<OptionFile> options = question.getOptions().stream()
                .sorted(Comparator.comparing(AnswerOption::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                // в файл — только то, что важно для типа вопроса
                .map(o -> new OptionFile(o.getText(),
                        order ? null : o.isCorrect(),
                        order ? o.getCorrectPosition() : null))
                .toList();
        return new QuestionFile(question.getText(), question.getAudioObjectKey(), question.getType(), options);
    }

    // ---------- Вспомогательные ----------

    private void assignIds(Course course) {
        long lessonId = Math.min(minId(lessons().map(Lesson::getId)), 0);
        for (Lesson lesson : course.getLessons()) {
            if (lesson.getId() == null) {
                lesson.setId(--lessonId);
            }
            for (LessonItem item : lesson.getItems()) {
                if (item.getId() == null) {
                    item.setId(nextItemId--);
                }
                if (item instanceof Question q) {
                    q.getOptions().stream().filter(o -> o.getId() == null).forEach(o -> o.setId(nextOptionId--));
                }
            }
        }
        long videoId = Math.min(minId(courses.stream().flatMap(c -> c.getVideos().stream()).map(CourseVideo::getId)), 0);
        for (CourseVideo video : course.getVideos()) {
            if (video.getId() == null) {
                video.setId(--videoId);
            }
        }
    }

    private void sortCourses() {
        courses.sort(Comparator.comparing(Course::getPosition, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private Stream<Lesson> lessons() {
        return courses.stream().flatMap(c -> c.getLessons().stream());
    }

    private Stream<LessonItem> items() {
        return lessons().flatMap(l -> l.getItems().stream());
    }

    private static long minId(Stream<Long> ids) {
        return ids.filter(id -> id != null).min(Long::compare).orElse(0L);
    }

    // путь к медиа хранится в тех же полях *ObjectKey, что и ключ MinIO у личных курсов;
    // какой это из двух, MediaUrls решает по типу курса
    private String media(String path) {
        String value = blankToNull(path);
        if (value != null) {
            mediaPaths.add(value);
        }
        return value;
    }

    private Path resolveMedia(String path) {
        Path file = mediaRoot.resolve(path).normalize();
        // «../» в пути не должен выводить за пределы папки
        if (!file.startsWith(mediaRoot)) {
            throw new IllegalArgumentException("Media path outside of media dir: " + path);
        }
        return file;
    }

    private void requireMediaFilesExist() {
        List<String> missing = new ArrayList<>();
        for (String path : mediaPaths) {
            Path file = mediaRoot.resolve(path).normalize();
            if (!file.startsWith(mediaRoot) || !Files.isRegularFile(file)) {
                missing.add(path);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Official media files not found in " + mediaRoot + ": " + missing);
        }
    }

    // отрицательный id не пересекается с IDENTITY-id из БД, поэтому сервисы ищут сначала здесь, потом в БД
    private static long requireNewId(Long id, String what, Stream<Long> existing) {
        if (id == null || id >= 0) {
            throw new IllegalStateException(what + " id must be a negative number, got " + id);
        }
        if (existing.anyMatch(id::equals)) {
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
