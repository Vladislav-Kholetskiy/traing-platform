package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AnalyticsScenarioBoundary} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AnalyticsScenarioBoundaryRegressionTest {

    @Test
    void analyticsDedicatedQueryServicesStaySeparated() throws IOException {
        String managerial = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImpl.java"
        ));
        String expert = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImpl.java"
        ));

        assertThat(managerial).contains("ManagerialHistoricalAnalyticsReadRepository");
        assertThat(expert).contains("ExpertQuestionAnalyticsReadRepository");
    }
}
