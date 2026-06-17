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
 * Проверяет, что {@code SelfHistoricalResultAttemptBackdoor} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultAttemptBackdoorRegressionTest {

    private static final Set<String> ALLOWED_CANONICAL_RESULT_RECORDING_FILES = Set.of(
        "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java",
        "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingSnapshotFactsProvider.java",
        "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingSubordinateSnapshotMaterializer.java",
        "src/main/java/com/vladislav/training/platform/result/service/SelfCompletionOrgSnapshotFactsResolver.java",
        "src/main/java/com/vladislav/training/platform/result/service/SelfCompletionOrgSnapshotFactsReader.java",
        "src/main/java/com/vladislav/training/platform/result/domain/ResultSnapshotAssembler.java",
        "src/main/java/com/vladislav/training/platform/result/domain/ResultSnapshotFacts.java"
    );

    @Test
    void resultQueryAndPersistenceSurfacesDoNotMixImmutableResultReadsWithAttemptSideOwnerRecovery() throws IOException {
        List<Path> riskFiles = Stream.of(
            productionJavaFilesUnder("src/main/java/com/vladislav/training/platform/result/query"),
            productionJavaFilesUnder("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence")
        )
            .flatMap(List::stream)
            .filter(path -> !normalized(path).endsWith("/JpaSelfHistoricalResultReader.java"))
            .toList();

        assertThat(riskFiles).isNotEmpty();
        for (Path riskFile : riskFiles) {
            String source = read(riskFile);

            assertThat(source)
                
                .doesNotContain("TestAttemptEntity")
                .doesNotContain("TestAttemptRepository")
                .doesNotContain("SpringDataTestAttemptJpaRepository")
                .doesNotContain("JpaTestAttemptRepositoryAdapter")
                .doesNotContain("findActiveSelfAttempt(")
                .doesNotContain("findActiveAssignedAttemptForActor(")
                .doesNotContain("findAllByUserIdAndTestIdOrderByIdAsc")
                .doesNotContain("findByUserIdAndTestIdAndAttemptModeAndStatusIn")
                .doesNotContain("findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn");
        }
    }

    @Test
    void canonicalResultRecordingAttemptLinksRemainConfinedToRecordingFlowInsteadOfHistoryReadFlow() throws IOException {
        List<Path> resultProductionFiles = productionJavaFilesUnder("src/main/java/com/vladislav/training/platform/result")
            .stream()
            .filter(path -> !ALLOWED_CANONICAL_RESULT_RECORDING_FILES.contains(normalized(path)))
            .toList();

        assertThat(resultProductionFiles).isNotEmpty();
        for (Path productionFile : resultProductionFiles) {
            String source = read(productionFile);

            assertThat(source)
                
                .doesNotContain("com.vladislav.training.platform.testing.repository.TestAttemptRepository")
                .doesNotContain("SpringDataTestAttemptJpaRepository")
                .doesNotContain("JpaTestAttemptRepositoryAdapter")
                .doesNotContain("TestAttemptEntity");
        }
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

