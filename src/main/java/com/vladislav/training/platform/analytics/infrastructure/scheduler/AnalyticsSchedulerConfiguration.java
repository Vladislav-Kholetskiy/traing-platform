package com.vladislav.training.platform.analytics.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Конфигурация {@code AnalyticsSchedulerConfiguration}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "analytics.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class AnalyticsSchedulerConfiguration {
}

