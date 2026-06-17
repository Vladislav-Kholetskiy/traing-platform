package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImplementationStub} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImplementationStubRegressionTest {

    private static final String LEGACY_BLOCKED_SEAM_MESSAGE =
        "Immutable result-side baseline does not expose a materialized actor anchor";

    private static final List<String> FORBIDDEN_PLACEHOLDER_TOKENS = List.of(
        "todo analytics",
        "temporary",
        "placeholder",
        "stub"
    );

    private static final List<Path> WAVE_FIVE_RUNTIME_FILES = List.of(
        Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java"),
        Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"),
        Path.of("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"),
        Path.of("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/SpringDataResultJpaRepository.java"),
        Path.of("src/main/java/com/vladislav/training/platform/assignment/controller/ManagerialCurrentSupervisionController.java"),
        Path.of("src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryServiceImpl.java"),
        Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/"
                + "JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"
        ),
        Path.of("src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"),
        Path.of("src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImpl.java"),
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"
        ),
        Path.of("src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java"),
        Path.of("src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImpl.java"),
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaExpertQuestionAnalyticsReadRepositoryAdapter.java"
        )
    );

    private static final List<Path> WAVE_FIVE_TARGET_TESTS = List.of(
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultControllerTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImplTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/assignment/controller/ManagerialCurrentSupervisionControllerTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryServiceImplTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsControllerTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImplTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsControllerTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImplTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/common/web/AnalyticsPublicApiPerimeterRegressionTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/common/web/AnalyticsPublicReadForbiddenDependencyRegressionTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/common/web/AnalyticsScenarioBoundaryRegressionTest.java")
    );

    @Test
    void analyticsTargetTestsRemainEnabled() throws IOException {
        assertThat(existingFiles(WAVE_FIVE_TARGET_TESTS)).hasSize(WAVE_FIVE_TARGET_TESTS.size());

        for (Path file : WAVE_FIVE_TARGET_TESTS) {
            assertThat(Files.readString(file))
                
                .doesNotContain("@Disabled");
        }
    }

    @Test
    void selfHistoryRuntimePathDoesNotContainLegacyBlockedSeamExceptionOrIntentionalBlocker() throws IOException {
        List<Path> selfHistoryRuntimeFiles = List.of(
            Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java"),
            Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"),
            Path.of("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/SpringDataResultJpaRepository.java")
        );

        for (Path file : selfHistoryRuntimeFiles) {
            String source = Files.readString(file);

            assertThat(source)
                
                .doesNotContain(LEGACY_BLOCKED_SEAM_MESSAGE);
        }

        assertThat(Files.readString(
            Path.of("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java")
        ))
            
            .doesNotContain("IllegalStateException");
    }

    @Test
    void analyticsRuntimeAndStaticLockFilesDoNotContainPublicPathPlaceholders() throws IOException {
        List<Path> filesToCheck = new ArrayList<>(existingFiles(WAVE_FIVE_RUNTIME_FILES));
        filesToCheck.add(Path.of("src/test/java/com/vladislav/training/platform/common/web/AnalyticsPublicApiPerimeterRegressionTest.java"));
        filesToCheck.add(Path.of("src/test/java/com/vladislav/training/platform/common/web/AnalyticsPublicReadForbiddenDependencyRegressionTest.java"));
        filesToCheck.add(Path.of("src/test/java/com/vladislav/training/platform/common/web/AnalyticsScenarioBoundaryRegressionTest.java"));

        for (Path file : filesToCheck) {
            String lowerCasedSource = Files.readString(file).toLowerCase();

            for (String forbiddenToken : FORBIDDEN_PLACEHOLDER_TOKENS) {
                assertThat(lowerCasedSource)
                    
                    .doesNotContain(forbiddenToken);
            }
        }
    }

    private List<Path> existingFiles(List<Path> files) {
        return files.stream()
            .filter(Files::exists)
            .toList();
    }
}

