package com.vladislav.training.platform.assignment.controller;

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
 * Проверяет, что {@code ManagerialCurrentSupervisionHttpMappingPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ManagerialCurrentSupervisionHttpMappingPerimeterRegressionTest {

    private static final Path CURRENT_SUPERVISION_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/assignment/controller/ManagerialCurrentSupervisionController.java"
    );
    private static final List<String> CURRENT_SUPERVISION_ROUTE_RISK_FRAGMENTS = List.of(
        "current-supervision",
        "managerial/current-supervision",
        "manager/current-supervision"
    );

    @Test
    void controllerMappingSurfacePublishesOnlyDedicatedManagerialCurrentSupervisionGetRoute() throws IOException {
        List<Path> controllerSources = productionControllerSources();

        assertThat(controllerSources).isNotEmpty();
        assertThat(controllerSources).contains(CURRENT_SUPERVISION_CONTROLLER);
        assertThat(read(CURRENT_SUPERVISION_CONTROLLER).toLowerCase(Locale.ROOT))
            .contains("/api/v1/managerial/current-supervision");
        assertThat(read(CURRENT_SUPERVISION_CONTROLLER))
            .doesNotContain("{userId}")
            .doesNotContain("{actorUserId}")
            .doesNotContain("{managerUserId}")
            .doesNotContain("{targetUserId}")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");

        for (Path controllerSource : controllerSources) {
            if (normalized(controllerSource).equals(normalized(CURRENT_SUPERVISION_CONTROLLER))) {
                continue;
            }
            String normalizedSource = read(controllerSource).toLowerCase(Locale.ROOT);
            for (String fragment : CURRENT_SUPERVISION_ROUTE_RISK_FRAGMENTS) {
                assertThat(normalizedSource)
                    
                    .doesNotContain(fragment);
            }
        }
    }

    @Test
    void controllerDoesNotPublishHistoricalAnalyticsExpertAnalyticsOrScopeOverrideSurface() {
        String source = read(CURRENT_SUPERVISION_CONTROLLER);

        assertThat(source)
            .doesNotContain("/api/v1/managerial/historical-analytics")
            .doesNotContain("ExpertQuestionAnalyticsQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("@RequestParam(\"actorUserId\")")
            .doesNotContain("@RequestParam(\"managerUserId\")")
            .doesNotContain("@RequestParam(\"targetUserId\")")
            .doesNotContain("@RequestParam(\"scope\")")
            .doesNotContain("@RequestParam(\"scopeOverride\")")
            .doesNotContain("@RequestParam(\"organizationalUnitIds\")")
            .doesNotContain("@RequestParam(\"subtreePaths\")");
    }

    @Test
    void controllerExposesSingleGetMappingWithoutSelectorParameters() {
        assertThat(ManagerialCurrentSupervisionController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/managerial/current-supervision");

        assertThat(Arrays.stream(ManagerialCurrentSupervisionController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(this::getMappingValue)
            .toList())
            .containsExactly("");
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
