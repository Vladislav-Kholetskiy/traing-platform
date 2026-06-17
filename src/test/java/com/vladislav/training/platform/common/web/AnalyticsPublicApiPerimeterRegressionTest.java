package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AnalyticsPublicApiPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AnalyticsPublicApiPerimeterRegressionTest {

    @Test
    void analyticsControllersExposeDedicatedRoutes() throws IOException {
        String managerial = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"
        ));
        String expert = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java"
        ));

        assertThat(managerial).contains("/api/v1/managerial/historical-analytics");
        assertThat(expert).contains("/api/v1/expert/question-analytics");
    }
}
