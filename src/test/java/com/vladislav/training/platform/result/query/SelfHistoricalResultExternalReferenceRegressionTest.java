package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultExternalReference} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultExternalReferenceRegressionTest {

    private static final Set<String> ALLOWED_SELF_HISTORY_PRODUCTION_FILES = Set.of(
        "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryService.java",
        "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java",
        "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultSummaryDto.java",
        "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java",
        "src/main/java/com/vladislav/training/platform/result/query/internal/SelfHistoricalResultReader.java",
        "src/main/java/com/vladislav/training/platform/result/query/internal/package-info.java",
        "src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"
    );

    @Test
    void selfHistoryTypesDoNotLeakIntoExternalProductionPackages() throws IOException {
        List<Path> externalProductionFiles = productionJavaFiles()
            .stream()
            .filter(path -> !ALLOWED_SELF_HISTORY_PRODUCTION_FILES.contains(normalized(path)))
            .toList();

        assertThat(externalProductionFiles).isNotEmpty();
        for (Path productionFile : externalProductionFiles) {
            String source = read(productionFile);

            assertThat(source)
                
                .doesNotContain("SelfHistoricalResultQueryService")
                .doesNotContain("SelfHistoricalResultReader")
                .doesNotContain("SelfHistoricalResultQuery")
                .doesNotContain("SelfHistoricalResultReadModel")
                .doesNotContain("findSelfHistoricalResults(");
        }
    }

    private List<Path> productionJavaFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/vladislav/training/platform"))) {
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

