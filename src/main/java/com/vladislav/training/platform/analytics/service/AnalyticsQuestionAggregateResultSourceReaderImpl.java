package com.vladislav.training.platform.analytics.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

public class AnalyticsQuestionAggregateResultSourceReaderImpl implements AnalyticsQuestionAggregateResultSourceReader {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsQuestionAggregateResultSourceReaderImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public List<AnalyticsQuestionAggregateResultSourceRow> readQuestionAggregateRows(
        Instant periodStartInclusive,
        Instant periodEndExclusive
    ) {
        Objects.requireNonNull(periodStartInclusive, "periodStartInclusive must not be null");
        Objects.requireNonNull(periodEndExclusive, "periodEndExclusive must not be null");
        if (!periodStartInclusive.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("periodStartInclusive must be before periodEndExclusive");
        }

        return jdbcTemplate.query(
            """
                select
                    r.id as result_id,
                    r.user_id_snapshot as user_key,
                    r.organizational_unit_id_snapshot as org_unit_key,
                    r.organizational_path_snapshot as org_path_value,
                    r.attempt_mode as attempt_mode_value,
                    r.score_percent as score_percent_value,
                    r.passed as passed_value,
                    r.snapshot_final_topic_control_flag as final_topic_control_value,
                    rqs.question_original_id as question_key,
                    rqs.topic_id_snapshot as topic_key,
                    rqs.is_correct as correct_flag,
                    rqs.earned_score as earned_score_value,
                    rqs.max_score as max_score_value,
                    r.completed_at as completed_at_value
                from result r
                join result_question_snapshot rqs on rqs.result_id = r.id
                where r.completed_at >= ?
                  and r.completed_at < ?
                order by r.completed_at asc, r.id asc, rqs.display_order asc, rqs.id asc
                """,
            resultSet -> {
                List<AnalyticsQuestionAggregateResultSourceRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Long questionOriginalId = readNullableLong(resultSet, "question_key");
                    if (questionOriginalId == null) {
                        continue;
                    }
                    rows.add(
                        new AnalyticsQuestionAggregateResultSourceRow(
                            resultSet.getLong("result_id"),
                            resultSet.getLong("user_key"),
                            resultSet.getLong("org_unit_key"),
                            resultSet.getString("org_path_value"),
                            resultSet.getString("attempt_mode_value"),
                            resultSet.getBigDecimal("score_percent_value"),
                            resultSet.getBoolean("passed_value"),
                            resultSet.getBoolean("final_topic_control_value"),
                            questionOriginalId,
                            readNullableLong(resultSet, "topic_key"),
                            resultSet.getBoolean("correct_flag"),
                            resultSet.getBigDecimal("earned_score_value"),
                            resultSet.getBigDecimal("max_score_value"),
                            resultSet.getTimestamp("completed_at_value").toInstant()
                        )
                    );
                }
                return rows;
            },
            Timestamp.from(periodStartInclusive),
            Timestamp.from(periodEndExclusive)
        );
    }

    @Override
    public Optional<AnalyticsResultSourceWindow> findAvailableResultSourceWindow() {
        return jdbcTemplate.query(
            """
                select
                    min(r.completed_at) as min_completed_at,
                    max(r.completed_at) as max_completed_at
                from result r
                where r.completed_at is not null
                """,
            resultSet -> {
                if (!resultSet.next()) {
                    return Optional.<AnalyticsResultSourceWindow>empty();
                }

                Timestamp minCompletedAt = resultSet.getTimestamp("min_completed_at");
                Timestamp maxCompletedAt = resultSet.getTimestamp("max_completed_at");
                if (minCompletedAt == null || maxCompletedAt == null) {
                    return Optional.<AnalyticsResultSourceWindow>empty();
                }

                Instant periodStartInclusive = minCompletedAt.toInstant()
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate()
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
                LocalDate lastBucketDate = maxCompletedAt.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
                Instant periodEndExclusive = lastBucketDate
                    .plusDays(1)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);

                return Optional.of(new AnalyticsResultSourceWindow(periodStartInclusive, periodEndExclusive));
            }
        );
    }

    private static Long readNullableLong(java.sql.ResultSet resultSet, String columnLabel) throws java.sql.SQLException {
        long value = resultSet.getLong(columnLabel);
        if (resultSet.wasNull()) {
            return null;
        }
        return value;
    }
}
