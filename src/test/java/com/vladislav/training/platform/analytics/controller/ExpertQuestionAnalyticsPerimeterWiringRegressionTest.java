package com.vladislav.training.platform.analytics.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ExpertQuestionAnalyticsPerimeterWiring} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ExpertQuestionAnalyticsPerimeterWiringRegressionTest {

    @Test
    void onlyDedicatedControllerMayInjectOrCallExpertQuestionAnalyticsQueryContract() throws IOException {
        List<Path> controllerSources = productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform")
            .stream()
            .filter(path -> normalized(path).contains("/controller/"))
            .toList();

        assertThat(controllerSources).isNotEmpty();
        List<Path> expertControllerSources = productionJavaSourcesUnder(
            "src/main/java/com/vladislav/training/platform/analytics/controller"
        ).stream()
            .filter(path -> normalized(path).endsWith("/ExpertQuestionAnalyticsController.java"))
            .toList();

        assertThat(expertControllerSources).hasSize(1);
        assertThat(read(expertControllerSources.get(0)))
            .contains("ExpertQuestionAnalyticsQueryService")
            .contains("findQuestionAnalytics(")
            .contains("InteractiveActorResolver")
            .contains("UtcClock")
            .doesNotContain("ExpertQuestionAnalyticsReadRepository")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("AccessPolicyQueryContext")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("SelfHistoricalResultQueryService");

        assertThat(controllerSources.stream()
            .filter(path -> !normalized(path).endsWith("/ExpertQuestionAnalyticsController.java"))
            .map(this::read)
            .toList())
            .noneMatch(source -> source.contains("ExpertQuestionAnalyticsQueryService"))
            .noneMatch(source -> source.contains("findQuestionAnalytics("))
            .noneMatch(source -> source.contains("ExpertQuestionAnalyticsReadRepository"));
    }

    @Test
    void orchestrationFacingServicesDoNotExposeExpertQuestionAnalyticsControllerDependencies() throws IOException {
        List<Path> orchestrationSources = Stream.of(
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/analytics/service"),
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/result/service"),
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/testing/service"),
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/content/service")
        )
            .flatMap(List::stream)
            .toList();

        assertThat(orchestrationSources).isNotEmpty();
        assertThat(orchestrationSources.stream().map(this::read).toList())
            .noneMatch(source -> source.contains("ExpertQuestionAnalyticsController"))
            .noneMatch(source -> source.contains("ExpertQuestionAnalyticsResponse"));
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
