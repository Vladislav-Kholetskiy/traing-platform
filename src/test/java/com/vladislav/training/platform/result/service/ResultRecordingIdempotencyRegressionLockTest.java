package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ResultRecordingIdempotencyRegressionLock}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ResultRecordingIdempotencyRegressionLockTest {

    @Test
    void recordingImplementationChecksForExistingCanonicalResultBeforeAttemptMaterialization() throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java");

        assertThat(source).contains("Result existingResult = resultRepository.findResultByTestAttemptId(testAttemptId);");
        assertThat(source).contains("if (existingResult != null) {");
        assertThat(source).contains("return existingResult.id();");
        assertThat(source.indexOf("findResultByTestAttemptId(testAttemptId)"))
            .isLessThan(source.indexOf("findTestAttemptById(testAttemptId)"));
    }

    @Test
    void duplicateConflictPathReReadsCanonicalResultInsteadOfOpeningMutableOverwritePath() throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java");

        assertThat(source).contains("catch (PersistenceConstraintViolationException exception)");
        assertThat(source).contains("Result canonicalResult = resultRepository.findResultByTestAttemptId(testAttemptId);");
        assertThat(source).contains("if (canonicalResult != null) {");
        assertThat(source).contains("return validatedIdempotentReplayResultId(canonicalResult, testAttemptId);");
        assertThat(source).contains("private Long validatedIdempotentReplayResultId(Result existingResult, Long testAttemptId)");
        assertThat(source).contains("throw exception;");
        assertThat(source).doesNotContain(
            "updateResult(",
            "refreshResult(",
            "rebuildResult(",
            "deleteResult(",
            "saveResult(canonicalResult)"
        );
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
