package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AnalyticsPublicReadForbiddenDependency} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AnalyticsPublicReadForbiddenDependencyRegressionTest {

    @Test
    void analyticsPublicReadControllersDoNotDependOnCommandServices() throws IOException {
        String managerial = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"
        ));
        String expert = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java"
        ));

        assertThat(managerial).doesNotContain("AssignmentCommandService");
        assertThat(expert).doesNotContain("AssignmentCommandService");
    }
}
