package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultSummaryDtoSeparation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultSummaryDtoSeparationRegressionTest {

    @Test
    void selfHistorySummaryDtoExistsAsLocalRepresentationLayerWithoutRuntimeOrPersistenceWiring() {
        String dtoSource = read("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultSummaryDto.java");

        assertThat(dtoSource)
            .contains("record SelfHistoricalResultSummaryDto(")
            .contains("Long resultId")
            .contains("Instant recordedAt")
            .doesNotContain("public record SelfHistoricalResultSummaryDto")
            .doesNotContain("@Service")
            .doesNotContain("@Component")
            .doesNotContain("@Repository")
            .doesNotContain("SelfHistoricalResultReader")
            .doesNotContain("SelfHistoricalResultReadRow")
            .doesNotContain("JpaSelfHistoricalResultReader");
    }

    private String read(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + relativePath, exception);
        }
    }
}

