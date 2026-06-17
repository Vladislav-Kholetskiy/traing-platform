package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AnalyticsResultRebuildServiceUserTopicLastAssignedFinal} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AnalyticsResultRebuildServiceUserTopicLastAssignedFinalRegressionTest {

    private static final Instant PERIOD_START_INCLUSIVE = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant PERIOD_END_EXCLUSIVE = Instant.parse("2026-05-02T00:00:00Z");
    private static final Long EXPECTED_LAST_ASSIGNED_FINAL_RESULT_ID = 103L;
    private static final Instant EXPECTED_LAST_ASSIGNED_FINAL_COMPLETED_AT = Instant.parse("2026-05-01T11:30:00Z");
    private static final BigDecimal EXPECTED_LAST_ASSIGNED_FINAL_SCORE_PERCENT = new BigDecimal("92.5000");
    private static final Boolean EXPECTED_LAST_ASSIGNED_FINAL_PASSED = Boolean.TRUE;

    @Test
    void userTopicAggregateContractMustMaterializeLastAssignedFinalSnapshotFieldsFromCanonicalAssignedFinalResult()
        throws Exception {
        String ddl = Files.readString(
            Path.of("src", "main", "resources", "db", "migration", "V100__full_schema_stack.sql")
        );
        String tableBlock = extractAnalyticsUserTopicAggregateBlock(ddl);

        assertThat(tableBlock)
            .contains("last_assigned_final_result_id")
            .contains("last_assigned_final_completed_at")
            .contains("last_assigned_final_score_percent")
            .contains("last_assigned_final_passed");

        List<String> rowComponents = Arrays.stream(AnalyticsUserTopicAggregateRow.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

        assertThat(rowComponents)
            .withFailMessage(
                "Stage 2 user-topic rebuild must materialize last_assigned_final_* fields for the latest ASSIGNED "
                    + "final-topic-control result within the same user/topic/period. Expected lastAssignedFinalResultId=%s, "
                    + "lastAssignedFinalCompletedAt=%s, lastAssignedFinalScorePercent=%s, lastAssignedFinalPassed=%s for "
                    + "period [%s, %s), but AnalyticsUserTopicAggregateRow currently exposes only %s",
                EXPECTED_LAST_ASSIGNED_FINAL_RESULT_ID,
                EXPECTED_LAST_ASSIGNED_FINAL_COMPLETED_AT,
                EXPECTED_LAST_ASSIGNED_FINAL_SCORE_PERCENT,
                EXPECTED_LAST_ASSIGNED_FINAL_PASSED,
                PERIOD_START_INCLUSIVE,
                PERIOD_END_EXCLUSIVE,
                rowComponents
            )
            .contains(
                "lastAssignedFinalResultId",
                "lastAssignedFinalCompletedAt",
                "lastAssignedFinalScorePercent",
                "lastAssignedFinalPassed"
            );
    }

    @Test
        void rebuildResultAnalyticsMaterializesLatestAssignedFinalSnapshotValues() throws Exception {
        AnalyticsQuestionAggregateResultSourceReader reader = new FakeResultSourceReader(
            List.of(
                sourceRow(
                    101L,
                    "ASSIGNED",
                    true,
                    Instant.parse("2026-05-01T10:00:00Z"),
                    new BigDecimal("80.0000"),
                    false
                ),
                sourceRow(
                    102L,
                    "ASSIGNED",
                    true,
                    EXPECTED_LAST_ASSIGNED_FINAL_COMPLETED_AT,
                    new BigDecimal("91.0000"),
                    false
                ),
                sourceRow(
                    EXPECTED_LAST_ASSIGNED_FINAL_RESULT_ID,
                    "ASSIGNED",
                    true,
                    EXPECTED_LAST_ASSIGNED_FINAL_COMPLETED_AT,
                    EXPECTED_LAST_ASSIGNED_FINAL_SCORE_PERCENT,
                    EXPECTED_LAST_ASSIGNED_FINAL_PASSED,
                    604L
                ),
                sourceRow(
                    EXPECTED_LAST_ASSIGNED_FINAL_RESULT_ID,
                    "ASSIGNED",
                    true,
                    EXPECTED_LAST_ASSIGNED_FINAL_COMPLETED_AT,
                    EXPECTED_LAST_ASSIGNED_FINAL_SCORE_PERCENT,
                    EXPECTED_LAST_ASSIGNED_FINAL_PASSED,
                    605L
                ),
                sourceRow(
                    104L,
                    "SELF",
                    true,
                    Instant.parse("2026-05-01T11:45:00Z"),
                    new BigDecimal("99.0000"),
                    true
                ),
                sourceRow(
                    105L,
                    "ASSIGNED",
                    false,
                    Instant.parse("2026-05-01T11:50:00Z"),
                    new BigDecimal("97.0000"),
                    true
                )
            )
        );
        CapturingUserTopicAggregateWriter writer = new CapturingUserTopicAggregateWriter();

        instantiateService(reader, new SameTopicKeyStrategy(), writer)
            .rebuildResultAnalytics(PERIOD_START_INCLUSIVE, PERIOD_END_EXCLUSIVE);

        assertThat(writer.capturedRows)
            .singleElement()
            .satisfies(row -> {
                assertThat(row.lastAssignedFinalResultId()).isEqualTo(EXPECTED_LAST_ASSIGNED_FINAL_RESULT_ID);
                assertThat(row.lastAssignedFinalCompletedAt()).isEqualTo(EXPECTED_LAST_ASSIGNED_FINAL_COMPLETED_AT);
                assertThat(row.lastAssignedFinalScorePercent()).isEqualByComparingTo(
                    EXPECTED_LAST_ASSIGNED_FINAL_SCORE_PERCENT
                );
                assertThat(row.lastAssignedFinalPassed()).isEqualTo(EXPECTED_LAST_ASSIGNED_FINAL_PASSED);
                assertThat(row.topicId()).isEqualTo(9001L);
                assertThat(row.attemptCount()).isEqualTo(4L);
            });
    }

    private static String extractAnalyticsUserTopicAggregateBlock(String ddl) {
        String marker = "create table analytics_user_topic_aggregate";
        int start = ddl.indexOf(marker);
        assertThat(start)
            .withFailMessage("analytics_user_topic_aggregate DDL block must exist in V100__full_schema_stack.sql")
            .isGreaterThanOrEqualTo(0);

        int nextSection = ddl.indexOf("-- 2. analytics_department_topic_aggregate", start);
        assertThat(nextSection)
            .withFailMessage(
                "analytics_user_topic_aggregate DDL block must terminate before analytics_department_topic_aggregate"
            )
            .isGreaterThan(start);

        return ddl.substring(start, nextSection);
    }

    private static AnalyticsResultRebuildServiceImpl instantiateService(
        AnalyticsQuestionAggregateResultSourceReader reader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter writer
    ) throws Exception {
        Constructor<AnalyticsResultRebuildServiceImpl> constructor = AnalyticsResultRebuildServiceImpl.class.getDeclaredConstructor(
            AnalyticsQuestionAggregateResultSourceReader.class,
            AnalyticsTopicKeyStrategy.class,
            AnalyticsUserTopicAggregateWriter.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(reader, topicKeyStrategy, writer);
    }

    private static AnalyticsQuestionAggregateResultSourceRow sourceRow(
        Long resultId,
        String attemptModeSnapshot,
        boolean finalTopicControlSnapshot,
        Instant completedAt,
        BigDecimal scorePercent,
        boolean passed
    ) {
        return sourceRow(
            resultId,
            attemptModeSnapshot,
            finalTopicControlSnapshot,
            completedAt,
            scorePercent,
            passed,
            501L + resultId
        );
    }

    private static AnalyticsQuestionAggregateResultSourceRow sourceRow(
        Long resultId,
        String attemptModeSnapshot,
        boolean finalTopicControlSnapshot,
        Instant completedAt,
        BigDecimal scorePercent,
        boolean passed,
        Long questionOriginalId
    ) {
        return new AnalyticsQuestionAggregateResultSourceRow(
            resultId,
            7001L,
            3001L,
            "/company/unit-a",
            attemptModeSnapshot,
            scorePercent,
            passed,
            finalTopicControlSnapshot,
            questionOriginalId,
            passed,
            passed ? BigDecimal.ONE : BigDecimal.ZERO,
            BigDecimal.ONE,
            completedAt
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

    private static final class SameTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

        private final List<AnalyticsQuestionAggregateResultSourceRow> capturedRows = new ArrayList<>();

        @Override
        public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
            capturedRows.add(sourceRow);
            return new AnalyticsTopicKeyResolution(9001L, true, "supported immutable topic");
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
}
