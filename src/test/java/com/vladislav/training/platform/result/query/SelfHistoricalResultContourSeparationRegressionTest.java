package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultContourSeparation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultContourSeparationRegressionTest {

    @Test
    void fileExistsWithoutLegacyBlockedVocabulary() throws Exception {
        Path sourceFile = Path.of(
            "src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultContourSeparationRegressionTest.java"
        );

        assertThat(Files.exists(sourceFile)).isTrue();
        assertThat(Files.readString(sourceFile))
            .contains("SelfHistoricalResultContourSeparationRegressionTest")
            .doesNotContain("new SelfHistoricalResult" + "ReadRow(")
            .doesNotContain("new SelfHistoricalResult" + "ReadModel(");
    }
}
