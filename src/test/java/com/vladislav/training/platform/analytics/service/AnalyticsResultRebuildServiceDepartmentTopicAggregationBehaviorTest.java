package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AnalyticsResultRebuildServiceDepartmentTopicAggregation}.
 * Сценарии описывают ожидаемую работу компонента.
 */
class AnalyticsResultRebuildServiceDepartmentTopicAggregationBehaviorTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 department-topic rebuild must aggregate immutable result source rows through explicit topic-key strategy "
            + "and delegate bounded rows to AnalyticsDepartmentTopicAggregateWriter; unsupported topic rows must be skipped; "
            + "no DB writer semantics, scheduler or public API are allowed in Stage 2 Step 5.";

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
    void rebuildResultAnalyticsAggregatesDepartmentTopicRowsAndSkipsUnsupportedTopicKeys() throws Exception {
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
                    7002L,
                    3001L,
                    "/company/unit-a",
                    "SELF",
                    new BigDecimal("50.0000"),
                    false,
                    true,
                    502L,
                    false,
                    new BigDecimal("0.5000"),
                    BigDecimal.ONE,
                    Instant.parse("2026-05-01T10:05:00Z")
                ),
                new AnalyticsQuestionAggregateResultSourceRow(
                    103L,
                    7003L,
                    3001L,
                    "/company/unit-a",
                    "ASSIGNED",
                    new BigDecimal("100.0000"),
                    true,
                    false,
                    503L,
                    true,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    Instant.parse("2026-05-01T10:10:00Z")
                )
            )
        );
        FakeTopicKeyStrategy topicKeyStrategy = new FakeTopicKeyStrategy();
        CapturingUserTopicAggregateWriter userTopicWriter = new CapturingUserTopicAggregateWriter();
        CapturingDepartmentTopicAggregateWriter departmentTopicWriter = new CapturingDepartmentTopicAggregateWriter();

        AnalyticsResultRebuildServiceImpl service = instantiateService(
            reader,
            topicKeyStrategy,
            userTopicWriter,
            departmentTopicWriter
        );

        service.rebuildResultAnalytics(periodStartInclusive, periodEndExclusive);

        assertThat(reader.invocationCount)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(1);
        assertThat(reader.capturedPeriodStartInclusive)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodStartInclusive);
        assertThat(reader.capturedPeriodEndExclusive)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodEndExclusive);

        assertThat(topicKeyStrategy.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .hasSize(3);

        assertThat(departmentTopicWriter.invocationCount)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(1);
        assertThat(departmentTopicWriter.capturedPeriodStartInclusive)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodStartInclusive);
        assertThat(departmentTopicWriter.capturedPeriodEndExclusive)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodEndExclusive);
        assertThat(departmentTopicWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                new AnalyticsDepartmentTopicAggregateRow(
                    3001L,
                    "/company/unit-a",
                    9001L,
                    new BigDecimal("100.0000"),
                    new BigDecimal("100.0000"),
                    1L,
                    0L,
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
        AnalyticsDepartmentTopicAggregateWriter departmentTopicWriter
    ) {
        try {
            Constructor<AnalyticsResultRebuildServiceImpl> constructor = AnalyticsResultRebuildServiceImpl.class.getDeclaredConstructor(
                AnalyticsQuestionAggregateResultSourceReader.class,
                AnalyticsTopicKeyStrategy.class,
                AnalyticsUserTopicAggregateWriter.class,
                AnalyticsDepartmentTopicAggregateWriter.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(reader, topicKeyStrategy, userTopicWriter, departmentTopicWriter);
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
        private int invocationCount;
        private Instant capturedPeriodStartInclusive;
        private Instant capturedPeriodEndExclusive;

        private FakeResultSourceReader(List<AnalyticsQuestionAggregateResultSourceRow> rowsToReturn) {
            this.rowsToReturn = rowsToReturn;
        }

        @Override
        public List<AnalyticsQuestionAggregateResultSourceRow> readQuestionAggregateRows(
            Instant periodStartInclusive,
            Instant periodEndExclusive
        ) {
            invocationCount++;
            capturedPeriodStartInclusive = periodStartInclusive;
            capturedPeriodEndExclusive = periodEndExclusive;
            return rowsToReturn;
        }
    }

    private static final class FakeTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

        private final List<AnalyticsQuestionAggregateResultSourceRow> capturedRows = new ArrayList<>();

        @Override
        public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
            capturedRows.add(sourceRow);
            if (sourceRow.resultId().equals(103L)) {
                return new AnalyticsTopicKeyResolution(null, false, "unsupported immutable topic anchor");
            }
            return new AnalyticsTopicKeyResolution(9001L, true, "supported test topic");
        }
    }

    private static final class CapturingUserTopicAggregateWriter implements AnalyticsUserTopicAggregateWriter {

        @Override
        public void replaceUserTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsUserTopicAggregateRow> aggregateRows
        ) {
        }
    }

    private static final class CapturingDepartmentTopicAggregateWriter implements AnalyticsDepartmentTopicAggregateWriter {

        private int invocationCount;
        private Instant capturedPeriodStartInclusive;
        private Instant capturedPeriodEndExclusive;
        private List<AnalyticsDepartmentTopicAggregateRow> capturedRows = List.of();

        @Override
        public void replaceDepartmentTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
        ) {
            invocationCount++;
            capturedPeriodStartInclusive = periodStartInclusive;
            capturedPeriodEndExclusive = periodEndExclusive;
            capturedRows = List.copyOf(aggregateRows);
        }
    }
}
