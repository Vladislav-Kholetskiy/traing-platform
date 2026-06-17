package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultCompileTimeSurface} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultCompileTimeSurfaceRegressionTest {

    @Test
    void blockedSelfHistoryContourKeepsOnlyMinimalCompileTimeSurfacePublic() {
        assertThat(read("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryService.java"))
            .contains("public interface SelfHistoricalResultQueryService")
            .contains("record SelfHistoricalResultQuery(")
            .contains("record SelfHistoricalResultReadModel(");

        assertThat(read("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"))
            .contains("class SelfHistoricalResultQueryServiceImpl")
            .doesNotContain("public class SelfHistoricalResultQueryServiceImpl");

        assertThat(read("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"))
            .contains("class JpaSelfHistoricalResultReader")
            .doesNotContain("public class JpaSelfHistoricalResultReader");

        assertThat(read("src/main/java/com/vladislav/training/platform/result/query/internal/SelfHistoricalResultReader.java"))
            .contains("public interface SelfHistoricalResultReader")
            .contains("record SelfHistoricalResultReadCriteria(")
            .contains("record SelfHistoricalResultReadRow(");
    }

    private String read(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + relativePath, exception);
        }
    }
}

