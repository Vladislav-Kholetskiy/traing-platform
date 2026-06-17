package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
/**
 * Проверяет совместимость схемы вокруг {@code AnalyticsUserTopicAggregateWriter}.
 * Это защищает код от незаметных несовместимых изменений.
 */
class AnalyticsUserTopicAggregateWriterSchemaCompatibilityTest {

    @Test
    void analyticsUserTopicAggregateRowMustAlignWithFrozenAnalyticsUserTopicAggregateSchema() throws Exception {
        String ddl = Files.readString(
            Path.of("src", "main", "resources", "db", "migration", "V100__full_schema_stack.sql")
        );
        String tableBlock = extractAnalyticsUserTopicAggregateBlock(ddl);

        assertThat(tableBlock)
            .contains("user_id")
            .contains("topic_id")
            .contains("period_start")
            .contains("period_end")
            .contains("last_assigned_final_result_id")
            .contains("last_assigned_final_completed_at")
            .contains("last_assigned_final_score_percent")
            .contains("last_assigned_final_passed")
            .contains("average_score_percent")
            .contains("pass_rate_percent")
            .contains("attempt_count")
            .contains("error_count")
            .contains("unique (user_id, topic_id, period_start, period_end)");

        RecordComponent[] components = AnalyticsUserTopicAggregateRow.class.getRecordComponents();

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsUserTopicAggregateRow must use userId because frozen analytics_user_topic_aggregate stores user_id"
            )
            .contains("userId");

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsUserTopicAggregateRow must not require attemptModeSnapshot while frozen analytics_user_topic_aggregate has no attempt_mode_snapshot"
            )
            .doesNotContain("attemptModeSnapshot");

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsUserTopicAggregateRow must expose averageScorePercent because frozen analytics_user_topic_aggregate requires average_score_percent"
            )
            .contains("averageScorePercent");

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsUserTopicAggregateRow must expose passRatePercent because frozen analytics_user_topic_aggregate requires pass_rate_percent"
            )
            .contains("passRatePercent");

        assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
            .withFailMessage(
                "AnalyticsUserTopicAggregateRow must align with frozen analytics_user_topic_aggregate instead of requiring DB migration"
            )
            .containsExactly(
                "userId",
                "topicId",
                "lastAssignedFinalResultId",
                "lastAssignedFinalCompletedAt",
                "lastAssignedFinalScorePercent",
                "lastAssignedFinalPassed",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "periodStartInclusive",
                "periodEndExclusive"
            );

        assertThat(Arrays.stream(components).map(component -> component.getType().getName()).toList())
            .withFailMessage(
                "AnalyticsUserTopicAggregateRow must align with frozen analytics_user_topic_aggregate instead of requiring DB migration"
            )
            .containsExactly(
                Long.class.getName(),
                Long.class.getName(),
                Long.class.getName(),
                Instant.class.getName(),
                BigDecimal.class.getName(),
                Boolean.class.getName(),
                BigDecimal.class.getName(),
                BigDecimal.class.getName(),
                long.class.getName(),
                long.class.getName(),
                Instant.class.getName(),
                Instant.class.getName()
            );
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
}
