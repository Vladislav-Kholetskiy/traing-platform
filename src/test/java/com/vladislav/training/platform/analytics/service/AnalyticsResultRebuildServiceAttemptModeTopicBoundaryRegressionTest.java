package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AnalyticsResultRebuildServiceAttemptModeTopicBoundary} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AnalyticsResultRebuildServiceAttemptModeTopicBoundaryRegressionTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 standard managerial topic aggregates must not silently mix SELF results into ASSIGNED-only "
            + "user-topic and department-topic slices before an explicit self-slice contract exists.";

    @Test
    void rebuildResultAnalyticsMustExcludeSelfResultsFromStandardTopicAggregates() {
        Instant periodStartInclusive = Instant.parse("2026-05-01T00:00:00Z");
        Instant periodEndExclusive = Instant.parse("2026-05-02T00:00:00Z");

        AnalyticsQuestionAggregateResultSourceReader reader = new FakeResultSourceReader(
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
                    "SELF",
                    new BigDecimal("100.0000"),
                    true,
                    true,
                    502L,
                    true,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    Instant.parse("2026-05-01T10:05:00Z")
                )
            )
        );
        AnalyticsTopicKeyStrategy topicKeyStrategy = new SameTopicKeyStrategy();
        CapturingUserTopicAggregateWriter userTopicWriter = new CapturingUserTopicAggregateWriter();
        CapturingDepartmentTopicAggregateWriter departmentTopicWriter = new CapturingDepartmentTopicAggregateWriter();

        AnalyticsResultRebuildServiceImpl service = instantiateService(
            reader,
            topicKeyStrategy,
            userTopicWriter,
            departmentTopicWriter
        );

        service.rebuildResultAnalytics(periodStartInclusive, periodEndExclusive);

        assertThat(userTopicWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .singleElement()
            .extracting(AnalyticsUserTopicAggregateRow::attemptCount)
            .isEqualTo(1L);

        assertThat(departmentTopicWriter.capturedRows)
            .withFailMessage(CONTRACT_MESSAGE)
            .singleElement()
            .extracting(AnalyticsDepartmentTopicAggregateRow::attemptCount)
            .isEqualTo(1L);
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

    private static final class SameTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

        private final List<AnalyticsQuestionAggregateResultSourceRow> capturedRows = new ArrayList<>();

        @Override
        public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
            capturedRows.add(sourceRow);
            return new AnalyticsTopicKeyResolution(9001L, true, "supported test topic");
        }
    }

    private static final class CapturingUserTopicAggregateWriter implements AnalyticsUserTopicAggregateWriter {

        private List<AnalyticsUserTopicAggregateRow> capturedRows = List.of();

        @Override
        public void replaceUserTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsUserTopicAggregateRow> aggregateRows
        ) {
            capturedRows = List.copyOf(aggregateRows);
        }
    }

    private static final class CapturingDepartmentTopicAggregateWriter implements AnalyticsDepartmentTopicAggregateWriter {

        private List<AnalyticsDepartmentTopicAggregateRow> capturedRows = List.of();

        @Override
        public void replaceDepartmentTopicAggregates(
            Instant periodStartInclusive,
            Instant periodEndExclusive,
            List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
        ) {
            capturedRows = List.copyOf(aggregateRows);
        }
    }
}
