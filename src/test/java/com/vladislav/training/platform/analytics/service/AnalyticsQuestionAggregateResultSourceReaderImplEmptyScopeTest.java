package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
/**
 * Проверяет поведение {@code AnalyticsQuestionAggregateResultSourceReaderImplEmptyScope}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AnalyticsQuestionAggregateResultSourceReaderImplEmptyScopeTest {

    private static final String CONTRACT_MESSAGE =
        "Stage 2 Step 2 source-reader boundary lock must preserve read-only immutable result-facts safety; "
            + "scheduler/controller/public API, write paths, live content/current org/assignment-testing status and topic fallback are not allowed.";

    private static final Path IMPLEMENTATION_SOURCE_PATH = Path.of(
        "src",
        "main",
        "java",
        "com",
        "vladislav",
        "training",
        "platform",
        "analytics",
        "service",
        "AnalyticsQuestionAggregateResultSourceReaderImpl.java"
    );

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
        "Repository",
        "Entity",
        "EntityManager",
        "createNativeQuery",
        "insert into",
        "update ",
        "delete from",
        "save(",
        "saveAll(",
        "delete(",
        "QuestionEntity",
        "TopicEntity",
        "CourseEntity",
        "OrganizationalUnitEntity",
        "UserOrganizationAssignmentEntity",
        "AssignmentEntity",
        "AssignmentStatus",
        "TestAttemptEntity",
        "TestAttemptStatus",
        "questionRepository",
        "topicRepository",
        "contentRepository",
        "organizationalUnitRepository",
        "assignmentRepository",
        "testAttemptRepository",
        "topicId",
        "topicIdSnapshot",
        "topicOriginalId",
        "courseId"
    );

    @Test
    void sourceReaderShellRemainsBoundarySafeAndEmptyScopedUntilImmutableResultFactsBehaviorOpens() throws IOException {
        AnalyticsQuestionAggregateResultSourceReaderImpl reader = createReader();
        Instant validStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant validEnd = Instant.parse("2026-01-02T00:00:00Z");

        assertThatThrownBy(() -> reader.readQuestionAggregateRows(null, validEnd))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> reader.readQuestionAggregateRows(validStart, null))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> reader.readQuestionAggregateRows(validStart, validStart))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reader.readQuestionAggregateRows(validEnd, validStart))
            .withFailMessage(CONTRACT_MESSAGE)
            .isInstanceOf(IllegalArgumentException.class);

        String source = Files.readString(IMPLEMENTATION_SOURCE_PATH);

        assertThat(FORBIDDEN_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
    }

    private AnalyticsQuestionAggregateResultSourceReaderImpl createReader() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);

        try {
            Constructor<AnalyticsQuestionAggregateResultSourceReaderImpl> jdbcTemplateConstructor =
                AnalyticsQuestionAggregateResultSourceReaderImpl.class.getDeclaredConstructor(JdbcTemplate.class);
            jdbcTemplateConstructor.setAccessible(true);
            return jdbcTemplateConstructor.newInstance(jdbcTemplate);
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }

        try {
            Constructor<AnalyticsQuestionAggregateResultSourceReaderImpl> noArgsConstructor =
                AnalyticsQuestionAggregateResultSourceReaderImpl.class.getDeclaredConstructor();
            noArgsConstructor.setAccessible(true);
            return noArgsConstructor.newInstance();
        } catch (NoSuchMethodException exception) {
            fail(
                CONTRACT_MESSAGE
                    + " SCN-11 source reader must be ready for bounded read-only result-facts implementation.",
                exception
            );
            throw new IllegalStateException("Unreachable");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
    }
}
