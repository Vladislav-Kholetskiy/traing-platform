package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.analytics.infrastructure.persistence.JdbcAnalyticsDepartmentTopicAggregateWriter;
import com.vladislav.training.platform.analytics.infrastructure.persistence.JdbcAnalyticsQuestionAggregateWriter;
import com.vladislav.training.platform.analytics.infrastructure.persistence.JdbcAnalyticsUserTopicAggregateWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Конфигурация {@code AnalyticsResultRebuildRuntimeConfiguration}.
 */
@Configuration
class AnalyticsResultRebuildRuntimeConfiguration {

    @Bean
    AnalyticsQuestionAggregateResultSourceReader analyticsQuestionAggregateResultSourceReader(JdbcTemplate jdbcTemplate) {
        return new AnalyticsQuestionAggregateResultSourceReaderImpl(jdbcTemplate);
    }

    @Bean
    AnalyticsTopicKeyStrategy analyticsTopicKeyStrategy() {
        return new SnapshotBackedAnalyticsTopicKeyStrategy();
    }

    @Bean
    AnalyticsUserTopicAggregateWriter analyticsUserTopicAggregateWriter(JdbcTemplate jdbcTemplate) {
        return new JdbcAnalyticsUserTopicAggregateWriter(jdbcTemplate);
    }

    @Bean
    AnalyticsDepartmentTopicAggregateWriter analyticsDepartmentTopicAggregateWriter(JdbcTemplate jdbcTemplate) {
        return new JdbcAnalyticsDepartmentTopicAggregateWriter(jdbcTemplate);
    }

    @Bean
    AnalyticsQuestionAggregateWriter analyticsQuestionAggregateWriter(JdbcTemplate jdbcTemplate) {
        return new JdbcAnalyticsQuestionAggregateWriter(jdbcTemplate);
    }

    @Bean
    AnalyticsCampaignAggregateSourceReader analyticsCampaignAggregateSourceReader(JdbcTemplate jdbcTemplate) {
        return new AnalyticsCampaignAggregateSourceReaderImpl(jdbcTemplate);
    }

    @Bean
    AnalyticsResultRebuildService analyticsResultRebuildService(
        AnalyticsQuestionAggregateResultSourceReader sourceReader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter userTopicAggregateWriter,
        AnalyticsDepartmentTopicAggregateWriter departmentTopicAggregateWriter,
        AnalyticsQuestionAggregateWriter questionAggregateWriter
    ) {
        return new AnalyticsResultRebuildServiceImpl(
            sourceReader,
            topicKeyStrategy,
            userTopicAggregateWriter,
            departmentTopicAggregateWriter,
            questionAggregateWriter
        );
    }
}
