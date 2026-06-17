package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ManagerialCurrentSupervisionPerimeterWiring} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ManagerialCurrentSupervisionPerimeterWiringRegressionTest {

    @Test
    void onlyDedicatedControllerMayInjectOrCallManagerialCurrentSupervisionQueryContract() throws IOException {
        List<Path> controllerSources = productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform")
            .stream()
            .filter(path -> normalized(path).contains("/controller/"))
            .toList();

        assertThat(controllerSources).isNotEmpty();
        List<Path> supervisionControllerSources = productionJavaSourcesUnder(
            "src/main/java/com/vladislav/training/platform/assignment/controller"
        ).stream()
            .filter(path -> normalized(path).endsWith("/ManagerialCurrentSupervisionController.java"))
            .toList();

        assertThat(supervisionControllerSources).hasSize(1);
        assertThat(read(supervisionControllerSources.get(0)))
            .contains("ManagerialCurrentSupervisionQueryService")
            .contains("findCurrentSupervision(")
            .contains("InteractiveActorResolver")
            .contains("UtcClock")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ExpertQuestionAnalyticsQueryService")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("ManagerialReadScopeProjectionService")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("AccessPolicyQueryContext")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy");

        assertThat(controllerSources.stream()
            .filter(path -> !normalized(path).endsWith("/ManagerialCurrentSupervisionController.java"))
            .map(this::read)
            .toList())
            .noneMatch(source -> source.contains("ManagerialCurrentSupervisionQueryService"))
            .noneMatch(source -> source.contains("findCurrentSupervision("))
            .noneMatch(source -> source.contains("ManagerialCurrentSupervisionReadRepository"))
            .noneMatch(source -> source.contains("/api/v1/managerial/current-supervision"));
    }

    @Test
    void orchestrationFacingServicesDoNotExposeManagerialCurrentSupervisionControllerDependencies() throws IOException {
        List<Path> orchestrationSources = Stream.of(
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/assignment/service"),
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/result/service"),
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/testing/service")
        )
            .flatMap(List::stream)
            .toList();

        assertThat(orchestrationSources).isNotEmpty();
        assertThat(orchestrationSources.stream().map(this::read).toList())
            .noneMatch(source -> source.contains("ManagerialCurrentSupervisionController"))
            .noneMatch(source -> source.contains("ManagerialCurrentSupervisionResponse"));
    }

    private List<Path> productionJavaSourcesUnder(String directory) throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of(directory))) {
            return paths
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }
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
