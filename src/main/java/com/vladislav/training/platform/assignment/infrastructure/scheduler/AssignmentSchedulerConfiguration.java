package com.vladislav.training.platform.assignment.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Конфигурация {@code AssignmentSchedulerConfiguration}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "assignment.scheduler.enabled", havingValue = "true")
class AssignmentSchedulerConfiguration {
}
