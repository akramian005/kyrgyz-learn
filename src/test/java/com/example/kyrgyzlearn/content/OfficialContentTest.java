package com.example.kyrgyzlearn.content;

import com.example.kyrgyzlearn.entity.lesson.Course;
import com.example.kyrgyzlearn.entity.lesson.CourseType;
import com.example.kyrgyzlearn.entity.lesson.CourseVideo;
import com.example.kyrgyzlearn.entity.lesson.Lesson;
import com.example.kyrgyzlearn.entity.lesson.LessonMessage;
import com.example.kyrgyzlearn.entity.lesson.Question;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Загрузка test-course.json из src/test/resources/content/official-test
class OfficialContentTest {

    private static final String LOCATION = "classpath*:content/official-test/*.json";

    @TempDir
    Path mediaDir;

    @Test
    void loadsCourseFromJson() throws Exception {
        Files.createDirectories(mediaDir.resolve("test"));
        Files.writeString(mediaDir.resolve("test/salam.png"), "png");
        Files.writeString(mediaDir.resolve("test/video.mp4"), "mp4");

        OfficialContent content = new OfficialContent(LOCATION, mediaDir.toString());
        content.load();

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
        assertThat(question.getOptions()).extracting("correct").containsExactly(true, false);

        CourseVideo video = content.findVideo(-100L).orElseThrow();
        assertThat(video.getPosition()).isEqualTo(1);
        assertThat(video.getVideoObjectKey()).isEqualTo("test/video.mp4");
    }

    @Test
    void failsWhenMediaFileIsMissing() {
        OfficialContent content = new OfficialContent(LOCATION, mediaDir.toString());

        assertThatThrownBy(content::load)
                .hasMessageContaining("test/salam.png")
                .hasMessageContaining("test/video.mp4");
    }
}
