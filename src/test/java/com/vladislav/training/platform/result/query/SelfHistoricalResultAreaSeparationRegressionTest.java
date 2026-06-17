package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultAreaSeparation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultAreaSeparationRegressionTest {

    @Test
    void selfHistoryContourDoesNotDriftIntoAttemptOrTestingOwnershipRecovery() {
        List<Path> selfHistorySources = List.of(
            Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryService.java"),
            Path.of("src/main/java/com/vladislav/training/platform/result/query/internal/SelfHistoricalResultReader.java"),
            Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java")
        );

        for (Path path : selfHistorySources) {
            String source = read(path);

            assertThat(source)
                .doesNotContain("TestAttemptEntity")
                .doesNotContain("CurrentAttemptReadController")
                .doesNotContain("ActiveAttemptOwnerLocalReadService")
                .doesNotContain("SelfCurrentAttemptReadFoundationStateReadService")
                .doesNotContain("AssignedCurrentAttemptReadFoundationStateReadService")
                .doesNotContain("TestAttemptRepository")
                .doesNotContain("JpaTestAttemptRepositoryAdapter")
                .doesNotContain("SpringDataTestAttemptJpaRepository")
                .doesNotContain("com.vladislav.training.platform.testing.");
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

