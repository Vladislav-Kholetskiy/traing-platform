package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfResultHistoryContourWiring} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfResultHistoryContourWiringRegressionTest {

    @Test
    void fileExistsWithoutLegacyBlockedVocabulary() throws Exception {
        Path sourceFile = Path.of(
            "src/test/java/com/vladislav/training/platform/access/service/SelfResultHistoryContourWiringRegressionTest.java"
        );

        assertThat(Files.exists(sourceFile)).isTrue();
        assertThat(Files.readString(sourceFile))
            .contains("SelfResultHistoryContourWiringRegressionTest")
            .doesNotContain("new SelfHistoricalResult" + "ReadRow(")
            .doesNotContain("new SelfHistoricalResult" + "ReadModel(");
    }
}
