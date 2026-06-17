package com.vladislav.training.platform.analytics.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Проверяет, что {@code ExpertQuestionAnalyticsHttpMappingPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ExpertQuestionAnalyticsHttpMappingPerimeterRegressionTest {

    private static final Path EXPECTED_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java"
    );

    private static final List<String> FORBIDDEN_GENERIC_CONTROLLER_NAMES = List.of(
        "AnalyticsAnalyticsController.java",
        "CommonAnalyticsController.java",
        "AnalyticsController.java",
        "ProgressController.java"
    );

    private static final List<String> EXPERT_ANALYTICS_ROUTE_RISK_FRAGMENTS = List.of(
        "question-analytics",
        "expert/question-analytics",
        "/analytics/progress",
        "/progress"
    );

    @Test
    void controllerMappingSurfacePublishesOnlyDedicatedExpertQuestionAnalyticsGetRoute() throws IOException {
        List<Path> controllerSources = productionControllerSources();

        assertThat(controllerSources).isNotEmpty();
        assertThat(controllerSources.stream().map(path -> path.getFileName().toString()).toList())
            .doesNotContainAnyElementsOf(FORBIDDEN_GENERIC_CONTROLLER_NAMES);
        assertThat(controllerSources).contains(EXPECTED_CONTROLLER);
        assertThat(read(EXPECTED_CONTROLLER).toLowerCase(Locale.ROOT))
            .contains("/api/v1/expert/question-analytics");
        assertThat(read(EXPECTED_CONTROLLER))
            .doesNotContain("{userId}")
            .doesNotContain("{actorUserId}")
            .doesNotContain("{expertUserId}")
            .doesNotContain("{targetUserId}")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");

        for (Path controllerSource : controllerSources) {
            if (normalized(controllerSource).equals(normalized(EXPECTED_CONTROLLER))) {
                continue;
            }
            String normalizedSource = read(controllerSource).toLowerCase(Locale.ROOT);
            for (String fragment : EXPERT_ANALYTICS_ROUTE_RISK_FRAGMENTS) {
                assertThat(normalizedSource)
                    
                    .doesNotContain(fragment);
            }
        }
    }

    @Test
    void controllerExposesSingleGetMappingWithoutPathVariables() {
        assertThat(ExpertQuestionAnalyticsController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/expert/question-analytics");

        assertThat(Arrays.stream(ExpertQuestionAnalyticsController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(this::getMappingValue)
            .toList())
            .containsExactly("");
    }

    @Test
    void routeBelongsToExactDedicatedControllerAndDoesNotMixAnalyticsOrContentAuthoringDependencies() throws IOException {
        String source = read(EXPECTED_CONTROLLER);

        assertThat(EXPECTED_CONTROLLER.getFileName().toString())
            .isEqualTo("ExpertQuestionAnalyticsController.java");
        assertThat(source)
            .contains("ExpertQuestionAnalyticsQueryService")
            .contains("@RequestMapping(\"/api/v1/expert/question-analytics\")")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("ContentLifecycleController")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("/api/v1/managerial/current-supervision")
            .doesNotContain("/api/v1/managerial/historical-analytics")
            .doesNotContain("/api/v1/content")
            .doesNotContain("/api/v1/course")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping");

        List<Path> routePublishers = productionControllerSources().stream()
            .filter(path -> read(path).contains("/api/v1/expert/question-analytics"))
            .toList();
        assertThat(routePublishers).hasSize(1);
        assertThat(routePublishers.get(0).getFileName().toString())
            .isEqualTo("ExpertQuestionAnalyticsController.java");
    }

    private List<Path> productionControllerSources() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/vladislav/training/platform"))) {
            return paths
                .filter(path -> path.toString().endsWith("Controller.java"))
                .filter(path -> normalized(path).contains("/controller/"))
                .toList();
        }
    }

    private String getMappingValue(Method method) {
        return method.getAnnotation(GetMapping.class).value().length == 0
            ? ""
            : method.getAnnotation(GetMapping.class).value()[0];
    }

    private String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
