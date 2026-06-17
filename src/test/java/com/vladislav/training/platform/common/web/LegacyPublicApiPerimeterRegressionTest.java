package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code LegacyPublicApiPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class LegacyPublicApiPerimeterRegressionTest {

    private static final List<String> FORBIDDEN_GENERIC_CONTROLLER_NAMES = List.of(
        "AnalyticsController.java",
        "AnalyticsAnalyticsController.java",
        "ManagerialAnalyticsController.java",
        "ProgressController.java",
        "AnalyticsProgressController.java",
        "UserProgressController.java",
        "CommonAnalyticsController.java",
        "UnifiedAnalyticsController.java"
    );

    private static final List<String> WAVE_FIVE_QUERY_SERVICES = List.of(
        "SelfHistoricalResultQueryService",
        "ManagerialCurrentSupervisionQueryService",
        "ManagerialHistoricalAnalyticsQueryService",
        "ExpertQuestionAnalyticsQueryService"
    );

    private static final Map<String, String> DEDICATED_CONTROLLER_TO_SERVICE = Map.of(
        "SelfHistoricalResultController.java", "SelfHistoricalResultQueryService",
        "ManagerialCurrentSupervisionController.java", "ManagerialCurrentSupervisionQueryService",
        "ManagerialHistoricalAnalyticsController.java", "ManagerialHistoricalAnalyticsQueryService",
        "ExpertQuestionAnalyticsController.java", "ExpertQuestionAnalyticsQueryService"
    );

    private static final Map<String, String> WAVE_FIVE_ROUTES = new LinkedHashMap<>();
    private static final String EXPERT_ROUTE = "/api/v1/expert/question-analytics";
    private static final String EXPERT_CONTROLLER = "ExpertQuestionAnalyticsController.java";

    static {
        WAVE_FIVE_ROUTES.put("/api/v1/self/results/history", "SelfHistoricalResultController.java");
        WAVE_FIVE_ROUTES.put("/api/v1/managerial/current-supervision", "ManagerialCurrentSupervisionController.java");
        WAVE_FIVE_ROUTES.put("/api/v1/managerial/historical-analytics", "ManagerialHistoricalAnalyticsController.java");
        WAVE_FIVE_ROUTES.put(EXPERT_ROUTE, EXPERT_CONTROLLER);
    }

    @Test
    void productionControllerLayerHasNoGenericAnalyticsMegaControllerNames() throws IOException {
        List<Path> controllerSources = productionControllerSources();

        assertThat(controllerSources).isNotEmpty();
        assertThat(controllerSources.stream().map(path -> path.getFileName().toString()).toList())
            .doesNotContainAnyElementsOf(FORBIDDEN_GENERIC_CONTROLLER_NAMES);
    }

    @Test
    void analyticsControllersDoNotMixContoursServicesOrRoutes() throws IOException {
        List<Path> controllerSources = productionControllerSources();

        for (Path controllerSource : controllerSources) {
            String source = read(controllerSource);
            String fileName = controllerSource.getFileName().toString();

            long referencedAnalyticsServices = WAVE_FIVE_QUERY_SERVICES.stream()
                .filter(source::contains)
                .count();
            assertThat(referencedAnalyticsServices)
                
                .isLessThanOrEqualTo(1L);

            long referencedAnalyticsRoutes = WAVE_FIVE_ROUTES.keySet().stream()
                .filter(source::contains)
                .count();
            assertThat(referencedAnalyticsRoutes)
                
                .isLessThanOrEqualTo(1L);
        }

        for (Map.Entry<String, String> entry : DEDICATED_CONTROLLER_TO_SERVICE.entrySet()) {
            String controllerFileName = entry.getKey();
            String expectedService = entry.getValue();

            List<Path> matchingControllers = controllerSources.stream()
                .filter(path -> path.getFileName().toString().equals(controllerFileName))
                .toList();
            if (matchingControllers.isEmpty()) {
                continue;
            }

            assertThat(matchingControllers).hasSize(1);
            String source = read(matchingControllers.get(0));
            assertThat(source)
                .contains(expectedService);

            for (String analyticsService : WAVE_FIVE_QUERY_SERVICES) {
                if (analyticsService.equals(expectedService)) {
                    continue;
                }
                assertThat(source)
                    
                    .doesNotContain(analyticsService);
            }
        }

        List<Path> managerialHistoricalControllers = controllerSources.stream()
            .filter(path -> path.getFileName().toString().equals("ManagerialHistoricalAnalyticsController.java"))
            .toList();
        assertThat(managerialHistoricalControllers)
            
            .hasSize(1);

        String managerialHistoricalSource = read(managerialHistoricalControllers.get(0));
        assertThat(managerialHistoricalSource)
            .contains("/api/v1/managerial/historical-analytics")
            .contains("/user-topic")
            .contains("/department-topic");

        List<Path> expertQuestionControllers = controllerSources.stream()
            .filter(path -> path.getFileName().toString().equals(EXPERT_CONTROLLER))
            .toList();
        assertThat(expertQuestionControllers)
            
            .hasSize(1);

        String expertQuestionSource = read(expertQuestionControllers.get(0));
        assertThat(expertQuestionSource)
            .contains(EXPERT_ROUTE)
            .contains("ExpertQuestionAnalyticsQueryService");

        for (Map.Entry<String, String> entry : WAVE_FIVE_ROUTES.entrySet()) {
            String route = entry.getKey();
            String expectedControllerFileName = entry.getValue();

            List<Path> routePublishers = controllerSources.stream()
                .filter(path -> read(path).contains(route))
                .toList();
            if (route.equals(EXPERT_ROUTE)) {
                assertThat(routePublishers)
                    
                    .isNotEmpty();
            }

            assertThat(routePublishers)
                
                .hasSize(1);
            assertThat(routePublishers.get(0).getFileName().toString()).isEqualTo(expectedControllerFileName);
        }

        assertThat(controllerSources.stream()
            .filter(path -> read(path).contains("/api/v1/managerial/historical-analytics"))
            .filter(path -> read(path).contains("/user-topic"))
            .toList())
            
            .hasSize(1);
        assertThat(controllerSources.stream()
            .filter(path -> read(path).contains("/api/v1/managerial/historical-analytics"))
            .filter(path -> read(path).contains("/department-topic"))
            .toList())
            
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
