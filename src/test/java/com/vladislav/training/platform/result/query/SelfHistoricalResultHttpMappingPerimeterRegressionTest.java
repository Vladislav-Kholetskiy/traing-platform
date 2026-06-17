package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.controller.CurrentAttemptReadController;
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
 * Проверяет, что {@code SelfHistoricalResultHttpMappingPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultHttpMappingPerimeterRegressionTest {

    private static final List<String> SELF_HISTORY_ROUTE_RISK_FRAGMENTS = List.of(
        "self-history",
        "result-history",
        "historical-results",
        "history/results",
        "self-results",
        "results/history",
        "self/history"
    );

    @Test
    void controllerMappingSurfacePublishesOnlyDedicatedSelfHistoryRouteAndNoGenericHistoryRoutes() throws IOException {
        List<Path> controllerSources = productionControllerSources();
        Path selfHistoryController = Path.of(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java"
        );

        assertThat(controllerSources).isNotEmpty();
        assertThat(controllerSources).contains(selfHistoryController);
        assertThat(controllerSources.stream()
            .map(this::read)
            .filter(source -> source.contains("/api/v1/self/results/history"))
            .count())
            .isEqualTo(1);
        assertThat(read(selfHistoryController).toLowerCase(Locale.ROOT))
            .contains("/api/v1/self/results/history");
        assertThat(read(selfHistoryController))
            .doesNotContain("{userId}")
            .doesNotContain("{actorUserId}")
            .doesNotContain("{targetUserId}")
            .doesNotContain("{subjectUserId}")
            .doesNotContain("@RequestParam")
            .doesNotContain("@PathVariable")
            .doesNotContain("@RequestBody")
            .doesNotContain("CurrentAttemptReadController")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("SpringDataTestAttemptJpaRepository");
        for (Path controllerSource : controllerSources) {
            if (normalized(controllerSource).equals(normalized(selfHistoryController))) {
                continue;
            }
            String normalizedSource = read(controllerSource).toLowerCase(Locale.ROOT);
            for (String fragment : SELF_HISTORY_ROUTE_RISK_FRAGMENTS) {
                assertThat(normalizedSource)
                    
                    .doesNotContain(fragment);
            }
        }
    }

    @Test
    void currentAttemptReadControllerMappingsRemainCurrentAttemptOnly() {
        assertThat(CurrentAttemptReadController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/current-attempts");

        assertThat(Arrays.stream(CurrentAttemptReadController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .sorted((left, right) -> left.getName().compareTo(right.getName()))
            .map(this::getMappingValue)
            .toList())
            .containsExactly(
                "/assigned/assignments/{assignmentId}/assignment-tests/{assignmentTestId}",
                "/self/tests/{testId}"
            );
    }

    @Test
    void selfHistoryRouteDoesNotShareControllerSourceWithResultRecordingOrCurrentAttemptFlows() throws IOException {
        String controllerSource = read(Path.of(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java"
        ));

        assertThat(controllerSource)
            .doesNotContain("ResultRecordingController")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("CurrentAttemptReadController")
            .doesNotContain("/api/v1/results")
            .doesNotContain("/api/v1/current-attempts");
    }

    private List<Path> productionControllerSources() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/vladislav/training/platform"))) {
            return paths
                .filter(path -> path.toString().endsWith("Controller.java"))
                .filter(path -> normalized(path).contains("/controller/")
                    || normalized(path).endsWith("/result/query/SelfHistoricalResultController.java"))
                .toList();
        }
    }

    private String getMappingValue(Method method) {
        return method.getAnnotation(GetMapping.class).value()[0];
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
