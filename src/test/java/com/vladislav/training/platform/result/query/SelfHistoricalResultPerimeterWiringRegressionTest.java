package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultPerimeterWiring} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultPerimeterWiringRegressionTest {

    @Test
    void onlyDedicatedSelfHistoryControllerMayInjectOrCallSelfHistoricalResultQueryContract() throws IOException {
        List<Path> controllerSources = productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform")
            .stream()
            .filter(path -> normalized(path).contains("/controller/"))
            .toList();

        assertThat(controllerSources).isNotEmpty();
        List<Path> selfHistoryControllerSources = productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/result/query")
            .stream()
            .filter(path -> normalized(path).endsWith("/SelfHistoricalResultController.java"))
            .toList();

        assertThat(selfHistoryControllerSources).hasSize(1);
        assertThat(read(selfHistoryControllerSources.get(0)))
            .contains("SelfHistoricalResultQueryService")
            .contains("findSelfHistoricalResults(")
            .contains("InteractiveActorResolver")
            .doesNotContain("SelfHistoricalResultReader")
            .doesNotContain("JpaSelfHistoricalResultReader")
            .doesNotContain("SpringDataResultJpaRepository")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("AccessPolicyQueryContextResolver");

        assertThat(controllerSources.stream().map(this::read).toList())
            .noneMatch(source -> source.contains("SelfHistoricalResultQueryService"))
            .noneMatch(source -> source.contains("SelfHistoricalResultReader"));
    }

    @Test
    void orchestrationFacingServicesDoNotExposeInternalSelfHistorySeam() throws IOException {
        List<Path> orchestrationSources = Stream.of(
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/testing/service"),
            productionJavaSourcesUnder("src/main/java/com/vladislav/training/platform/result/service")
        )
            .flatMap(List::stream)
            .toList();

        assertThat(orchestrationSources).isNotEmpty();
        assertThat(orchestrationSources.stream().map(this::read).toList())
            .noneMatch(source -> source.contains("SelfHistoricalResultReader"))
            .noneMatch(source -> source.contains("SelfHistoricalResultQueryService"))
            .noneMatch(source -> source.contains("findSelfHistoricalResults("));
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

