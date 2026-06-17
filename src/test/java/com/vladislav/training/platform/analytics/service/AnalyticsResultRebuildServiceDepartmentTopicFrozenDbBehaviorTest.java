package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AnalyticsResultRebuildServiceDepartmentTopicFrozenDb}.
 * Сценарии описывают ожидаемую работу компонента.
 */
class AnalyticsResultRebuildServiceDepartmentTopicFrozenDbBehaviorTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 department-topic rebuild must materialize rows compatible with frozen analytics_department_topic_aggregate V100 schema and must not keep attemptModeSnapshot as a department-topic dimension.";

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
    void rebuildResultAnalyticsMustMaterializeFrozenDbShapedDepartmentTopicRows() throws Exception {
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
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
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
                    new BigDecimal("1.0000"),
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
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    Instant.parse("2026-05-01T10:10:00Z")
                )
            )
        );
        FakeTopicKeyStrategy topicKeyStrategy = new FakeTopicKeyStrategy();
        NoOpUserTopicAggregateWriter userTopicWriter = new NoOpUserTopicAggregateWriter();
        CapturingDepartmentTopicAggregateWriter departmentTopicWriter = new CapturingDepartmentTopicAggregateWriter();

        AnalyticsResultRebuildServiceImpl service = instantiateService(
            reader,
            topicKeyStrategy,
            userTopicWriter,
            departmentTopicWriter
        );

        service.rebuildResultAnalytics(periodStartInclusive, periodEndExclusive);

        assertThat(departmentTopicWriter.invocationCount)
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(1);
        assertThat(departmentTopicWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .hasSize(1);

        Object capturedRow = departmentTopicWriter.capturedRows.getFirst();
        Class<?> rowClass = capturedRow.getClass();
        RecordComponent[] components = rowClass.getRecordComponents();

        assertThat(rowClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(List.of(components).stream().map(RecordComponent::getName).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                "organizationalUnitIdSnapshot",
                "organizationalPathSnapshot",
                "topicId",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "periodStartInclusive",
                "periodEndExclusive"
            );
        assertThat(List.of(components).stream().map(component -> component.getType().getName()).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .containsExactly(
                Long.class.getName(),
                String.class.getName(),
                Long.class.getName(),
                BigDecimal.class.getName(),
                BigDecimal.class.getName(),
                long.class.getName(),
                long.class.getName(),
                Instant.class.getName(),
                Instant.class.getName()
            );
        assertThat(List.of(components).stream().map(RecordComponent::getName).toList())
            .withFailMessage(CONTRACT_MESSAGE)
            .doesNotContain("attemptModeSnapshot", "totalAnsweredQuestions", "correctAnsweredQuestions");

        assertThat(invokeAccessor(capturedRow, "organizationalUnitIdSnapshot"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(3001L);
        assertThat(invokeAccessor(capturedRow, "organizationalPathSnapshot"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo("/company/unit-a");
        assertThat(invokeAccessor(capturedRow, "topicId"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(9001L);
        assertThat(invokeAccessor(capturedRow, "averageScorePercent"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(new BigDecimal("100.0000"));
        assertThat(invokeAccessor(capturedRow, "passRatePercent"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(new BigDecimal("100.0000"));
        assertThat(invokeAccessor(capturedRow, "attemptCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(1L);
        assertThat(invokeAccessor(capturedRow, "errorCount"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(0L);
        assertThat(invokeAccessor(capturedRow, "periodStartInclusive"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodStartInclusive);
        assertThat(invokeAccessor(capturedRow, "periodEndExclusive"))
            .withFailMessage(CONTRACT_MESSAGE)
            .isEqualTo(periodEndExclusive);

        String source = Files.readString(productionSourcePath());
        assertThat(FORBIDDEN_MARKERS.stream().noneMatch(source::contains))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
    }

    private static Object invokeAccessor(Object target, String accessorName) {
        try {
            Method accessor = target.getClass().getMethod(accessorName);
            return accessor.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(CONTRACT_MESSAGE, exception);
        }
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

    private static final class NoOpUserTopicAggregateWriter implements AnalyticsUserTopicAggregateWriter {

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
        private List<AnalyticsDepartmentTopicAggregateRow> capturedRows = List.of();

        @Override
        public void replaceDepartmentTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
        ) {
            invocationCount++;
            capturedRows = List.copyOf(aggregateRows);
        }
    }
}
