package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfResultHistoryAccessAreaWiring} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfResultHistoryAccessAreaWiringRegressionTest {

    private static final Set<String> ALLOWED_SELF_HISTORY_RUNTIME_FILES = Set.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessReadArea.java",
        "src/main/java/com/vladislav/training/platform/access/service/AccessSpecificationPolicy.java",
        "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java",
        "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java",
        "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultReviewQueryServiceImpl.java"
    );

    @Test
    void selfResultHistoryContourStaysInsideControlledFailClosedQueryGate() throws IOException {
        List<Path> externalProductionFiles = productionJavaFiles()
            .stream()
            .filter(path -> !ALLOWED_SELF_HISTORY_RUNTIME_FILES.contains(normalized(path)))
            .toList();

        assertThat(externalProductionFiles).isNotEmpty();
        for (Path productionFile : externalProductionFiles) {
            String source = read(productionFile);

            assertThat(source)
                
                .doesNotContain("SELF_RESULT_HISTORY")
                .doesNotContain("AccessReadArea.SELF_RESULT_HISTORY")
                .doesNotContain("canReadSelfResultHistory(")
                .doesNotContain("self_result_history");
        }

        String queryService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"
        ));

        assertThat(queryService)
            .contains("AccessReadArea.SELF_RESULT_HISTORY")
            .contains("contextResolver.resolveActorSelfScope(")
            .contains("accessSpecificationPolicy.canRead(context)")
            .contains("SelfHistoricalResultReader.findSelfHistoricalResultRows(")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("SpringDataTestAttempt")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService");

        String reviewQueryService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultReviewQueryServiceImpl.java"
        ));

        assertThat(reviewQueryService)
            .contains("AccessReadArea.SELF_RESULT_HISTORY")
            .contains("contextResolver.resolveActorSelfScope(")
            .contains("accessSpecificationPolicy.canRead(context)")
            .contains("resultRepository.findResultById(query.resultId())")
            .contains("resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId")
            .contains("resultAnswerOptionSnapshotRepository.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("SpringDataTestAttempt")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService");

        String canonicalPolicy = read(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
        ));

        assertThat(canonicalPolicy)
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("SpringDataTestAttempt")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadFoundationStateReadService")
            .doesNotContain("SelfCurrentAttemptReadFoundationStateReadService")
            .doesNotContain("findCurrentAssignedAttemptForActor(")
            .doesNotContain("findCurrentSelfAttemptForActor(")
            .doesNotContain("findActiveAssignedAttemptForActor(")
            .doesNotContain("findActiveSelfAttempt(")
            .doesNotContain("findAllByUserIdAndTestIdOrderByIdAsc")
            .doesNotContain("findByUserIdAndTestIdAndAttemptModeAndStatusIn")
            .doesNotContain("findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn")
            .doesNotContain("SCN-20")
            .doesNotContain("current entitlement");
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

