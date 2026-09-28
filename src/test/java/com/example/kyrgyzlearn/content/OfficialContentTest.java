package com.example.kyrgyzlearn.content;

import com.example.kyrgyzlearn.entity.lesson.AnswerOption;
import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import com.example.kyrgyzlearn.entity.lesson.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Загрузка и запись official-content: test-course.json из src/test/resources/content/official-test
// копируется во временную папку — OfficialContent пишет в неё изменения
class OfficialContentTest {

    private static final Path TEST_COURSE = Path.of("src/test/resources/content/official-test/test-course.json");

    @TempDir
    Path contentDir;

    @TempDir
    Path mediaDir;

    @BeforeEach
    void copyCourseFile() throws Exception {
        Files.copy(TEST_COURSE, contentDir.resolve("test-course.json"));
    }

    @Test
    void loadsCourseFromJson() throws Exception {
        createMedia();

        OfficialContent content = load();

        Course course = content.findCourse(-100L).orElseThrow();
        assertThat(course.getType()).isEqualTo(CourseType.OFFICIAL);
        assertThat(course.getPosition()).isEqualTo(5);

        Lesson lesson = content.findLesson(-100L).orElseThrow();
        assertThat(lesson.getCourse()).isSameAs(course);
        assertThat(lesson.getPosition()).isZero();
        assertThat(lesson.getItems()).hasSize(2);
        assertThat(lesson.getItems().get(0)).isInstanceOf(LessonMessage.class);
        assertThat(((LessonMessage) lesson.getItems().get(0)).getImageObjectKey()).isEqualTo("test/salam.png");

        Question question = (Question) lesson.getItems().get(1);
        assertThat(question.getPosition()).isEqualTo(1);
        assertThat(question.getId()).isNegative();
        assertThat(content.findQuestion(question.getId())).containsSame(question);
        assertThat(question.getOptions()).extracting("correct").containsExactly(true, false);

        CourseVideo video = content.findVideo(-100L).orElseThrow();
        assertThat(video.getPosition()).isEqualTo(1);
        assertThat(video.getVideoObjectKey()).isEqualTo("test/video.mp4");
    }

    @Test
    void failsWhenMediaFileIsMissing() {
        OfficialContent content = new OfficialContent(contentDir.toString(), mediaDir.toString());

        assertThatThrownBy(content::load)
                .hasMessageContaining("test/salam.png")
                .hasMessageContaining("test/video.mp4");
    }

    @Test
    void savesEditedCourseBackToItsFile() throws Exception {
        createMedia();
        OfficialContent content = load();
        Course course = content.findCourse(-100L).orElseThrow();

        // как это делают сервисы: меняют объекты в памяти и вызывают save
        course.setTitle("Новое название");
        course.getVideos().clear();

        Lesson lesson = new Lesson();
        lesson.setTitle("Второй урок");
        lesson.setPosition(1);
        lesson.setCourse(course);
        Question question = new Question();
        question.setText("Порядок слов");
        question.setType(QuestionType.ORDER);
        question.setPosition(0);
        question.setLesson(lesson);
        AnswerOption option = new AnswerOption();
        option.setText("Мен");
        option.setCorrectPosition(0);
        option.setSortOrder(0);
        option.setQuestion(question);
        question.getOptions().add(option);
        lesson.getItems().add(question);
        course.getLessons().add(lesson);

        content.save(course);

        // новые объекты получили отрицательные id, не занятые другими
        assertThat(lesson.getId()).isEqualTo(-101L);
        assertThat(question.getId()).isNegative();
        assertThat(option.getId()).isNegative();

        String json = Files.readString(contentDir.resolve("test-course.json"));
        assertThat(json).contains("Новое название").doesNotContain("null").doesNotContain("\"VIDEO\"");

        Course reloaded = load().findCourse(-100L).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Новое название");
        assertThat(reloaded.getVideos()).isEmpty();
        assertThat(reloaded.getLessons()).extracting(Lesson::getTitle).containsExactly("Урок", "Второй урок");
        Question reloadedQuestion = (Question) reloaded.getLessons().get(1).getItems().get(0);
        assertThat(reloadedQuestion.getType()).isEqualTo(QuestionType.ORDER);
        assertThat(reloadedQuestion.getOptions()).extracting("correctPosition").containsExactly(0);
    }

    @Test
    void addsAndRemovesCourseFiles() throws Exception {
        createMedia();
        OfficialContent content = load();

        Course course = new Course();
        course.setTitle("Новый курс");
        course.setPosition(0);
        content.add(course);

        assertThat(course.getId()).isEqualTo(-101L);
        assertThat(contentDir.resolve("course-101.json")).exists();
        assertThat(content.getCourses()).extracting(Course::getId).containsExactly(-101L, -100L); // по position

        String media = content.storeMedia(course, ".mp3", new ByteArrayInputStream("mp3".getBytes()));
        assertThat(media).startsWith("course-101/").endsWith(".mp3");
        assertThat(mediaDir.resolve(media)).hasContent("mp3");

        content.remove(course);
        assertThat(contentDir.resolve("course-101.json")).doesNotExist();
        assertThat(content.findCourse(-101L)).isEmpty();
    }

    private OfficialContent load() throws Exception {
        OfficialContent content = new OfficialContent(contentDir.toString(), mediaDir.toString());
        content.load();
        return content;
    }

    private void createMedia() throws Exception {
        Files.createDirectories(mediaDir.resolve("test"));
        Files.writeString(mediaDir.resolve("test/salam.png"), "png");
        Files.writeString(mediaDir.resolve("test/video.mp4"), "mp4");
    }
}
