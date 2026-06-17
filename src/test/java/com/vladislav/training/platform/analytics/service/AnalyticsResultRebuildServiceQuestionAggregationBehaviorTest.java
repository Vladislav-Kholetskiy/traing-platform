package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AnalyticsResultRebuildServiceQuestionAggregation}.
 * Сценарии описывают ожидаемую работу компонента.
 */
class AnalyticsResultRebuildServiceQuestionAggregationBehaviorTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 question aggregate rebuild must aggregate immutable result question snapshot rows by frozen question_id "
            + "and delegate bounded rows to AnalyticsQuestionAggregateWriter; it must not depend "
            + "on topic-key support, live content, scheduler, public API or DB writer semantics in Stage 2 Step 6.";

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
        "JdbcTemplate",
        "EntityManager",
        "Repository",
        "insert into",
        "update ",
        "delete from",
        "save(",
        "saveAll(",
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
    void rebuildResultAnalyticsAggregatesQuestionRowsWithoutTopicKeyDependency() throws Exception {
        Instant periodStartInclusive = Instant.parse("2026-05-01T00:00:00Z");
        Instant periodEndExclusive = Instant.parse("2026-05-02T00:00:00Z");

        FakeResultSourceReader reader = new FakeResultSourceReader(
            List.of(
                new AnalyticsQuestionAggregateResultSourceRow(
                    101L,
                    7001L,
                    3001L,
                    "/company/unit-a",
                    "ASSIGNED",
                    new BigDecimal("100.0000"),
                    true,
                    false,
                    501L,
                    true,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    Instant.parse("2026-05-01T10:00:00Z")
                ),
                new AnalyticsQuestionAggregateResultSourceRow(
                    102L,
                    7001L,
                    3001L,
                    "/company/unit-a",
                    "ASSIGNED",
                    new BigDecimal("0.0000"),
                    false,
                    false,
                    501L,
                    false,
                    BigDecimal.ZERO,
                    BigDecimal.ONE,
                    Instant.parse("2026-05-01T10:05:00Z")
                ),
                new AnalyticsQuestionAggregateResultSourceRow(
                    103L,
                    7001L,
                    3001L,
                    "/company/unit-a",
                    "ASSIGNED",
                    new BigDecimal("100.0000"),
                    true,
                    false,
                    502L,
                    true,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    Instant.parse("2026-05-01T10:10:00Z")
                )
            )
        );
        FakeTopicKeyStrategy topicKeyStrategy = new FakeTopicKeyStrategy();
        NoOpUserTopicAggregateWriter userTopicWriter = new NoOpUserTopicAggregateWriter();
        NoOpDepartmentTopicAggregateWriter departmentTopicWriter = new NoOpDepartmentTopicAggregateWriter();
        CapturingQuestionAggregateWriter questionWriter = new CapturingQuestionAggregateWriter();

        AnalyticsResultRebuildServiceImpl service = instantiateService(
            reader,
            topicKeyStrategy,
            userTopicWriter,
            departmentTopicWriter,
            questionWriter
        );

        service.rebuildResultAnalytics(periodStartInclusive, periodEndExclusive);

        assertThat(questionWriter.invocationCount)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(1);
        assertThat(questionWriter.capturedPeriodStartInclusive)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodStartInclusive);
        assertThat(questionWriter.capturedPeriodEndExclusive)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodEndExclusive);
        assertThat(questionWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                new AnalyticsQuestionAggregateRow(
                    501L,
                    2L,
                    1L,
                    1L,
                    new BigDecimal("0.5000"),
                    periodStartInclusive,
                    periodEndExclusive
                ),
                new AnalyticsQuestionAggregateRow(
                    502L,
                    1L,
                    1L,
                    0L,
                    new BigDecimal("1.0000"),
                    periodStartInclusive,
                    periodEndExclusive
                )
            );

        String source = Files.readString(productionSourcePath());
        assertThat(FORBIDDEN_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
    }

    private static AnalyticsResultRebuildServiceImpl instantiateService(
        AnalyticsQuestionAggregateResultSourceReader reader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter userTopicWriter,
        AnalyticsDepartmentTopicAggregateWriter departmentTopicWriter,
        AnalyticsQuestionAggregateWriter questionWriter
    ) {
        try {
            Constructor<AnalyticsResultRebuildServiceImpl> constructor = AnalyticsResultRebuildServiceImpl.class.getDeclaredConstructor(
                AnalyticsQuestionAggregateResultSourceReader.class,
                AnalyticsTopicKeyStrategy.class,
                AnalyticsUserTopicAggregateWriter.class,
                AnalyticsDepartmentTopicAggregateWriter.class,
                AnalyticsQuestionAggregateWriter.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(reader, topicKeyStrategy, userTopicWriter, departmentTopicWriter, questionWriter);
        } catch (NoSuchMethodException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
    }

    private static Path productionSourcePath() {
        return Path.of(
            "src",
            "main",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "analytics",
            "service",
            "AnalyticsResultRebuildServiceImpl.java"
        );
    }

    private static final class FakeResultSourceReader implements AnalyticsQuestionAggregateResultSourceReader {

        private final List<AnalyticsQuestionAggregateResultSourceRow> rowsToReturn;

        private FakeResultSourceReader(List<AnalyticsQuestionAggregateResultSourceRow> rowsToReturn) {
            this.rowsToReturn = rowsToReturn;
        }

        @Override
        public List<AnalyticsQuestionAggregateResultSourceRow> readQuestionAggregateRows(
            Instant periodStartInclusive,
            Instant periodEndExclusive
        ) {
            return rowsToReturn;
        }
    }

    private static final class FakeTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

        @Override
        public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
            return new AnalyticsTopicKeyResolution(null, false, "unsupported immutable topic anchor");
        }
    }

    private static final class NoOpUserTopicAggregateWriter implements AnalyticsUserTopicAggregateWriter {

        @Override
        public void replaceUserTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsUserTopicAggregateRow> aggregateRows
        ) {
        }
    }

    private static final class NoOpDepartmentTopicAggregateWriter implements AnalyticsDepartmentTopicAggregateWriter {

        @Override
        public void replaceDepartmentTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
        ) {
        }
    }

    private static final class CapturingQuestionAggregateWriter implements AnalyticsQuestionAggregateWriter {

        private int invocationCount;
        private Instant capturedPeriodStartInclusive;
        private Instant capturedPeriodEndExclusive;
        private List<AnalyticsQuestionAggregateRow> capturedRows = List.of();

        @Override
        public void replaceQuestionAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsQuestionAggregateRow> aggregateRows
        ) {
            invocationCount++;
            capturedPeriodStartInclusive = periodStartInclusive;
            capturedPeriodEndExclusive = periodEndExclusive;
            capturedRows = List.copyOf(aggregateRows);
        }
    }
}
