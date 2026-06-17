package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.service.AnalyticsQuestionAggregateRow;
import com.vladislav.training.platform.analytics.service.AnalyticsQuestionAggregateWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcAnalyticsQuestionAggregateWriter implements AnalyticsQuestionAggregateWriter {

    private static final String DELETE_SQL =
        """
            delete from analytics_question_aggregate
            where period_start = ?
              and period_end = ?
            """;

    private static final String INSERT_SQL =
        """
            insert into analytics_question_aggregate (
                question_id,
                period_start,
                period_end,
                attempt_count,
                correct_count,
                incorrect_count,
                average_earned_score,
                calculated_at,
                refreshed_at,
                reconciled_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAnalyticsQuestionAggregateWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public void replaceQuestionAggregates(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        List<AnalyticsQuestionAggregateRow> aggregateRows
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
                AnalyticsQuestionAggregateRow row = aggregateRows.get(i);
                ps.setLong(1, row.questionId());
                ps.setTimestamp(2, periodStart);
                ps.setTimestamp(3, periodEnd);
                ps.setLong(4, row.attemptCount());
                ps.setLong(5, row.correctCount());
                ps.setLong(6, row.incorrectCount());
                ps.setBigDecimal(7, row.averageEarnedScore());
                ps.setTimestamp(8, calculatedAt);
                ps.setTimestamp(9, refreshedAt);
                ps.setTimestamp(10, null);
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
        List<AnalyticsQuestionAggregateRow> aggregateRows
    ) {
        Objects.requireNonNull(periodStartInclusive, "periodStartInclusive must not be null");
        Objects.requireNonNull(periodEndExclusive, "periodEndExclusive must not be null");
        Objects.requireNonNull(aggregateRows, "aggregateRows must not be null");
        if (!periodStartInclusive.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("periodStartInclusive must be before periodEndExclusive");
        }
        for (AnalyticsQuestionAggregateRow row : aggregateRows) {
            Objects.requireNonNull(row, "aggregateRows must not contain null rows");
            if (!periodStartInclusive.equals(row.periodStartInclusive())
                || !periodEndExclusive.equals(row.periodEndExclusive())) {
                throw new IllegalArgumentException("aggregate row period bounds must match method period bounds");
            }
        }
    }
}
