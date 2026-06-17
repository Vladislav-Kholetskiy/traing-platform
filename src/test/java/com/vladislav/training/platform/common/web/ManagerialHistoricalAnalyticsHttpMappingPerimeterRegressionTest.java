package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ManagerialHistoricalAnalyticsHttpMappingPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ManagerialHistoricalAnalyticsHttpMappingPerimeterRegressionTest {

    private static final Path EXPECTED_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"
    );

    private static final List<String> FORBIDDEN_GENERIC_CONTROLLER_NAMES = List.of(
        "AnalyticsAnalyticsController.java",
        "CommonAnalyticsController.java",
        "ProgressController.java",
        "AnalyticsProgressController.java",
        "UserProgressController.java",
        "UnifiedAnalyticsController.java"
    );

    @Test
    void managerialHistoricalAnalyticsMustHaveDedicatedControllerWithExactName() throws IOException {
        List<String> controllerFileNames = productionControllerSources().stream()
            .map(path -> path.getFileName().toString())
            .toList();

        assertThat(controllerFileNames)
            .doesNotContainAnyElementsOf(FORBIDDEN_GENERIC_CONTROLLER_NAMES);
        assertThat(EXPECTED_CONTROLLER)
            
            .exists();
    }

    @Test
    void managerialHistoricalAnalyticsControllerMustExposeCanonicalRoutesAndSingleAnalyticsDependency() throws IOException {
        assertThat(EXPECTED_CONTROLLER)
            
            .exists();

        String source = Files.readString(EXPECTED_CONTROLLER);

        assertThat(source)
            .contains("ManagerialHistoricalAnalyticsQueryService")
            .contains("/api/v1/managerial/historical-analytics")
            .contains("/user-topic")
            .contains("/department-topic")
            .doesNotContain("ExpertQuestionAnalyticsQueryService")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentAdministrativeActionService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("/api/v1/managerial/current-supervision")
            .doesNotContain("/api/v1/expert/question-analytics")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("CapabilityAdmissionPolicy");
    }

    @Test
    void managerialHistoricalAnalyticsRoutesArePublishedExactlyOnceByDedicatedController() throws Exception {
        String source = read(EXPECTED_CONTROLLER);

        assertThat(source)
            .contains("@RequestMapping(\"/api/v1/managerial/historical-analytics\")")
            .contains("@GetMapping(\"/user-topic\")")
            .contains("@GetMapping(\"/department-topic\")");

        List<Path> routePublishers = productionControllerSources().stream()
            .filter(path -> read(path).contains("/api/v1/managerial/historical-analytics"))
            .toList();
        assertThat(routePublishers)
            
            .hasSize(1);
        assertThat(routePublishers.get(0).getFileName().toString()).isEqualTo("ManagerialHistoricalAnalyticsController.java");

        assertThat(routePublishers.stream().filter(path -> read(path).contains("/user-topic")).toList())
            
            .hasSize(1);
        assertThat(routePublishers.stream().filter(path -> read(path).contains("/department-topic")).toList())
            
            .hasSize(1);
    }

    private List<Path> productionControllerSources() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/vladislav/training/platform"))) {
            return paths
                .filter(path -> path.toString().endsWith("Controller.java"))
                .toList();
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
