package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.service.AnalyticsDepartmentTopicAggregateRow;
import com.vladislav.training.platform.analytics.service.AnalyticsDepartmentTopicAggregateWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcAnalyticsDepartmentTopicAggregateWriter implements AnalyticsDepartmentTopicAggregateWriter {

    private static final String DELETE_SQL =
        """
            delete from analytics_department_topic_aggregate
            where period_start = ?
              and period_end = ?
            """;

    private static final String INSERT_SQL =
        """
            insert into analytics_department_topic_aggregate (
                organizational_unit_id_snapshot,
                organizational_path_snapshot,
                topic_id,
                period_start,
                period_end,
                average_score_percent,
                pass_rate_percent,
                attempt_count,
                error_count,
                calculated_at,
                refreshed_at,
                reconciled_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAnalyticsDepartmentTopicAggregateWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public void replaceDepartmentTopicAggregates(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
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
                AnalyticsDepartmentTopicAggregateRow row = aggregateRows.get(i);
                ps.setLong(1, row.organizationalUnitIdSnapshot());
                ps.setString(2, row.organizationalPathSnapshot());
                ps.setLong(3, row.topicId());
                ps.setTimestamp(4, periodStart);
                ps.setTimestamp(5, periodEnd);
                ps.setBigDecimal(6, row.averageScorePercent());
                ps.setBigDecimal(7, row.passRatePercent());
                ps.setLong(8, row.attemptCount());
                ps.setLong(9, row.errorCount());
                ps.setTimestamp(10, calculatedAt);
                ps.setTimestamp(11, refreshedAt);
                ps.setTimestamp(12, null);
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
        List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
    ) {
        Objects.requireNonNull(periodStartInclusive, "periodStartInclusive must not be null");
        Objects.requireNonNull(periodEndExclusive, "periodEndExclusive must not be null");
        Objects.requireNonNull(aggregateRows, "aggregateRows must not be null");
        if (!periodStartInclusive.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("periodStartInclusive must be before periodEndExclusive");
        }
        for (AnalyticsDepartmentTopicAggregateRow row : aggregateRows) {
            Objects.requireNonNull(row, "aggregateRows must not contain null rows");
            if (!periodStartInclusive.equals(row.periodStartInclusive())
                || !periodEndExclusive.equals(row.periodEndExclusive())) {
                throw new IllegalArgumentException("aggregate row period bounds must match method period bounds");
            }
        }
    }
}
