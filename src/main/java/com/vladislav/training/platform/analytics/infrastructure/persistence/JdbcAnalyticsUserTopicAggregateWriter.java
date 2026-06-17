package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateRow;
import com.vladislav.training.platform.analytics.service.AnalyticsUserTopicAggregateWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcAnalyticsUserTopicAggregateWriter implements AnalyticsUserTopicAggregateWriter {

    private static final String DELETE_SQL =
        """
            delete from analytics_user_topic_aggregate
            where period_start = ?
              and period_end = ?
            """;

    private static final String INSERT_SQL =
        """
            insert into analytics_user_topic_aggregate (
                user_id,
                topic_id,
                period_start,
                period_end,
                last_assigned_final_result_id,
                last_assigned_final_completed_at,
                last_assigned_final_score_percent,
                last_assigned_final_passed,
                average_score_percent,
                pass_rate_percent,
                attempt_count,
                error_count,
                calculated_at,
                refreshed_at,
                reconciled_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAnalyticsUserTopicAggregateWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public void replaceUserTopicAggregates(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        List<AnalyticsUserTopicAggregateRow> aggregateRows
    ) {
        validateInputs(periodStartInclusive, periodEndExclusive, aggregateRows);

        Timestamp periodStart = Timestamp.from(periodStartInclusive);
        Timestamp periodEnd = Timestamp.from(periodEndExclusive);

        jdbcTemplate.update(DELETE_SQL, periodStart, periodEnd);
        if (aggregateRows.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        Timestamp calculatedAt = Timestamp.from(now);
        Timestamp refreshedAt = Timestamp.from(now);

        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                AnalyticsUserTopicAggregateRow row = aggregateRows.get(i);
                ps.setLong(1, row.userId());
                ps.setLong(2, row.topicId());
                ps.setTimestamp(3, periodStart);
                ps.setTimestamp(4, periodEnd);
                ps.setObject(5, row.lastAssignedFinalResultId());
                ps.setTimestamp(
                    6,
                    row.lastAssignedFinalCompletedAt() == null ? null : Timestamp.from(row.lastAssignedFinalCompletedAt())
                );
                ps.setBigDecimal(7, row.lastAssignedFinalScorePercent());
                ps.setObject(8, row.lastAssignedFinalPassed());
                ps.setBigDecimal(9, row.averageScorePercent());
                ps.setBigDecimal(10, row.passRatePercent());
                ps.setLong(11, row.attemptCount());
                ps.setLong(12, row.errorCount());
                ps.setTimestamp(13, calculatedAt);
                ps.setTimestamp(14, refreshedAt);
                ps.setTimestamp(15, null);
            }

            @Override
            public int getBatchSize() {
                return aggregateRows.size();
            }
        });
    }

    private void validateInputs(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        List<AnalyticsUserTopicAggregateRow> aggregateRows
    ) {
        Objects.requireNonNull(periodStartInclusive, "periodStartInclusive must not be null");
        Objects.requireNonNull(periodEndExclusive, "periodEndExclusive must not be null");
        Objects.requireNonNull(aggregateRows, "aggregateRows must not be null");
        if (!periodStartInclusive.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("periodStartInclusive must be before periodEndExclusive");
        }
        for (AnalyticsUserTopicAggregateRow row : aggregateRows) {
            Objects.requireNonNull(row, "aggregateRows must not contain null rows");
            if (!periodStartInclusive.equals(row.periodStartInclusive())
                || !periodEndExclusive.equals(row.periodEndExclusive())) {
                throw new IllegalArgumentException("aggregate row period bounds must match method period bounds");
            }
        }
    }
}
