package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoryTestingSideAttemptBackdoor} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoryTestingSideAttemptBackdoorRegressionTest {

    private static final Set<String> ALLOWED_CANONICAL_RESULT_HANDOFF_FILES = Set.of(
        "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java",
        "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java"
    );

    @Test
    void testingAttemptRecoveryRiskFilesDoNotPullResultSideHistoryReadAnchors() throws IOException {
        List<Path> riskFiles = productionJavaFilesUnder("src/main/java/com/vladislav/training/platform/testing")
            .stream()
            .filter(path -> !ALLOWED_CANONICAL_RESULT_HANDOFF_FILES.contains(normalized(path)))
            .filter(path -> containsAttemptRecoveryMarker(read(path)))
            .toList();

        assertThat(riskFiles).isNotEmpty();
        for (Path riskFile : riskFiles) {
            String source = read(riskFile);

            assertThat(source)
                
                .doesNotContain("com.vladislav.training.platform.result.query")
                .doesNotContain("SelfHistoricalResultQueryService")
                .doesNotContain("SelfHistoricalResultReader")
                .doesNotContain("SelfHistoricalResultQuery")
                .doesNotContain("SelfHistoricalResultReadModel")
                .doesNotContain("ResultRepository")
                .doesNotContain("SpringDataResultJpaRepository")
                .doesNotContain("JpaResultRepositoryAdapter")
                .doesNotContain("ResultEntity")
                .doesNotContain("findResultByTestAttemptId(")
                .doesNotContain("findByTestAttemptId(");
        }
    }

    @Test
    void canonicalTestingToResultHandoffsStayCommandSideOnlyInsteadOfHistoryReadReconstruction() throws IOException {
        for (String allowedFile : ALLOWED_CANONICAL_RESULT_HANDOFF_FILES) {
            String source = read(Path.of(allowedFile));

            assertThat(source)
                
                .contains("ResultRecordingService")
                .contains("recordResult(")
                .doesNotContain("ResultRepository")
                .doesNotContain("SpringDataResultJpaRepository")
                .doesNotContain("JpaResultRepositoryAdapter")
                .doesNotContain("ResultEntity")
                .doesNotContain("findResultByTestAttemptId(")
                .doesNotContain("SelfHistoricalResultQueryService")
                .doesNotContain("SelfHistoricalResultReader");
        }
    }

    private boolean containsAttemptRecoveryMarker(String source) {
        return source.contains("TestAttemptRepository")
            || source.contains("TestAttemptEntity")
            || source.contains("SpringDataTestAttemptJpaRepository")
            || source.contains("JpaTestAttemptRepositoryAdapter")
            || source.contains("findActiveSelfAttempt(")
            || source.contains("findActiveAssignedAttemptForActor(")
            || source.contains("AssignedCurrentAttemptRead")
            || source.contains("SelfCurrentAttemptRead")
            || source.contains("CurrentAttempt")
            || source.contains("ActiveAttemptOwnerLocalReadService");
    }

    private List<Path> productionJavaFilesUnder(String directory) throws IOException {
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

