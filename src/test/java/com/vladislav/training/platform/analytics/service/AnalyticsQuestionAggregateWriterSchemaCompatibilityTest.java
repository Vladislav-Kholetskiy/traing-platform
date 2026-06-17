package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
/**
 * Проверяет совместимость схемы вокруг {@code AnalyticsQuestionAggregateWriter}.
 * Это защищает код от незаметных несовместимых изменений.
 */
class AnalyticsQuestionAggregateWriterSchemaCompatibilityTest {

    @Test
    void analyticsQuestionAggregateRowMustAlignWithFrozenAnalyticsQuestionAggregateSchema() throws Exception {
        String ddl = Files.readString(
            Path.of("src", "main", "resources", "db", "migration", "V100__full_schema_stack.sql")
        );
        String tableBlock = extractAnalyticsQuestionAggregateBlock(ddl);

        assertThat(tableBlock)
            .contains("question_id")
            .contains("period_start")
            .contains("period_end")
            .contains("attempt_count")
            .contains("correct_count")
            .contains("incorrect_count")
            .contains("average_earned_score")
            .contains("unique (question_id, period_start, period_end)");

        RecordComponent[] components = AnalyticsQuestionAggregateRow.class.getRecordComponents();

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsQuestionAggregateRow must use questionId because frozen analytics_question_aggregate stores question_id"
            )
            .contains("questionId");

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsQuestionAggregateRow must not require attemptModeSnapshot while frozen analytics_question_aggregate has no attempt_mode_snapshot"
            )
            .doesNotContain("attemptModeSnapshot");

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsQuestionAggregateRow must expose averageEarnedScore because frozen analytics_question_aggregate requires average_earned_score"
            )
            .contains("averageEarnedScore");

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsQuestionAggregateRow must align with frozen analytics_question_aggregate instead of requiring DB migration"
            )
            .containsExactly(
                "questionId",
                "attemptCount",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "periodStartInclusive",
                "periodEndExclusive"
            );

        assertThat(Arrays.stream(components).map(component -> component.getType().getName()).toList())
            .withFailMessage(
                "AnalyticsQuestionAggregateRow must align with frozen analytics_question_aggregate instead of requiring DB migration"
            )
            .containsExactly(
                Long.class.getName(),
                long.class.getName(),
                long.class.getName(),
                long.class.getName(),
                BigDecimal.class.getName(),
                Instant.class.getName(),
                Instant.class.getName()
            );
    }

    private static String extractAnalyticsQuestionAggregateBlock(String ddl) {
        String marker = "create table analytics_question_aggregate";
        int start = ddl.indexOf(marker);
        assertThat(start)
            .withFailMessage("analytics_question_aggregate DDL block must exist in V100__full_schema_stack.sql")
            .isGreaterThanOrEqualTo(0);

        int nextSection = ddl.indexOf("-- 4. analytics_campaign_aggregate", start);
        assertThat(nextSection)
            .withFailMessage("analytics_question_aggregate DDL block must terminate before analytics_campaign_aggregate")
            .isGreaterThan(start);

        return ddl.substring(start, nextSection);
    }
}
