package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
/**
 * Проверяет, что {@code AnalyticsQuestionAggregateResultSourceReaderNullableQuestionOriginalId} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AnalyticsQuestionAggregateResultSourceReaderNullableQuestionOriginalIdRegressionTest {

    private static final Instant PERIOD_START = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-05-02T00:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-05-01T10:00:00Z");

    @Test
    void readQuestionAggregateRowsMustNotMaterializeNullQuestionOriginalIdAsSyntheticZero() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.getLong("result_id")).thenReturn(101L);
        when(resultSet.getLong("user_key")).thenReturn(7001L);
        when(resultSet.getLong("org_unit_key")).thenReturn(3001L);
        when(resultSet.getString("org_path_value")).thenReturn("/company/unit-a");
        when(resultSet.getString("attempt_mode_value")).thenReturn("ASSIGNED");
        when(resultSet.getLong("question_key")).thenReturn(0L);
        when(resultSet.wasNull()).thenReturn(true);
        when(resultSet.getBoolean("correct_flag")).thenReturn(true);
        when(resultSet.getBigDecimal("earned_score_value")).thenReturn(java.math.BigDecimal.ONE);
        when(resultSet.getBigDecimal("max_score_value")).thenReturn(java.math.BigDecimal.ONE);
        when(resultSet.getTimestamp("completed_at_value")).thenReturn(Timestamp.from(COMPLETED_AT));

        when(
            jdbcTemplate.query(
                anyString(),
                any(ResultSetExtractor.class),
                eq(Timestamp.from(PERIOD_START)),
                eq(Timestamp.from(PERIOD_END))
            )
        ).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<List<AnalyticsQuestionAggregateResultSourceRow>> extractor =
                invocation.getArgument(1, ResultSetExtractor.class);
            when(resultSet.next()).thenReturn(true, false);
            return extractor.extractData(resultSet);
        });

        AnalyticsQuestionAggregateResultSourceReaderImpl reader = new AnalyticsQuestionAggregateResultSourceReaderImpl(
            jdbcTemplate
        );

        assertThat(reader.readQuestionAggregateRows(PERIOD_START, PERIOD_END))
            
            .isEmpty();
    }
}
