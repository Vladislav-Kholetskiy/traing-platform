package com.vladislav.training.platform.analytics.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateRow;
import com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
/**
 * Проверяет договорённости вокруг {@code JdbcAnalyticsUserTopicAggregateWriter}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class JdbcAnalyticsUserTopicAggregateWriterContractTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 user-topic aggregate writer implementation must stay bounded to analytics_user_topic_aggregate "
            + "exact-period materialization and must not open live source fallback, scheduler or public API surfaces.";

    private static final String WRITER_CLASS_NAME =
        "com.vladislav.training.platform.analytics.infrastructure.persistence.JdbcAnalyticsUserTopicAggregateWriter";

    private static final List<String> FORBIDDEN_MARKERS = List.of(
        "@Service",
        "@Component",
        "@Scheduled",
        "@EnableScheduling",
        "@RestController",
        "@Controller",
        "@RequestMapping",
        "@PostMapping",
        "@GetMapping",
        "QuestionEntity",
        "TopicEntity",
        "CourseEntity",
        "OrganizationalUnitEntity",
        "AssignmentEntity",
        "TestAttemptEntity",
        "questionRepository",
        "topicRepository",
        "contentRepository",
        "assignmentRepository",
        "testAttemptRepository",
        "question.topic_id",
        "getTopicId",
        "AssignmentStatus",
        "TestAttemptStatus"
    );

    @Test
    void jdbcUserTopicAggregateWriterMustStayBoundedToExactPeriodMaterializationOnly() throws Exception {
        Path writerSourcePath = writerProductionSourcePath();
        assertThat(Files.exists(writerSourcePath))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        Class<?> writerClass = loadClassOrFail(WRITER_CLASS_NAME);

        assertThat(writerClass.getPackageName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("com.vladislav.training.platform.analytics.infrastructure.persistence");
        assertThat(writerClass.isInterface())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(Modifier.isAbstract(writerClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(writerClass.isEnum())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(writerClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(AnalyticsUserTopicAggregateWriter.class.isAssignableFrom(writerClass))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();

        Constructor<?> constructor = writerClass.getDeclaredConstructor(JdbcTemplate.class);
        Object jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        assertThat(constructor.newInstance(jdbcTemplate))
            .withFailMessage(CONTRACT_MESSAGE)
            .isNotNull();
        assertThatThrownBy(() -> constructor.newInstance(new Object[] {null}))
            .withFailMessage(CONTRACT_MESSAGE)
            .hasCauseInstanceOf(NullPointerException.class);

        Method replaceMethod = writerClass.getDeclaredMethod(
            "replaceUserTopicAggregates",
            Instant.class,
            Instant.class,
            List.class
        );
        assertThat(replaceMethod.getReturnType())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(void.class);
        assertThat(replaceMethod.getGenericParameterTypes()[2].getTypeName())
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("java.util.List<" + AnalyticsUserTopicAggregateRow.class.getName() + ">");

        String writerSource = Files.readString(writerSourcePath);
        assertThat(FORBIDDEN_MARKERS.stream().noneMatch(writerSource::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(writerSource)
            .withFailMessage(CONTRACT_MESSAGE)
            .contains("JdbcTemplate")
            .doesNotContain("analytics_department_topic_aggregate")
            .doesNotContain("analytics_question_aggregate");
    }

    private static Class<?> loadClassOrFail(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        }
    }

    private static Path writerProductionSourcePath() {
        return Path.of(
            "src",
            "main",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "analytics",
            "infrastructure",
            "persistence",
            "JdbcAnalyticsUserTopicAggregateWriter.java"
        );
    }
}
