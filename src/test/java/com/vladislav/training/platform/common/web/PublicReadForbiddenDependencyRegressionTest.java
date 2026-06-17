package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code PublicReadForbiddenDependency} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class PublicReadForbiddenDependencyRegressionTest {

    private static final List<Path> WAVE_FIVE_PUBLIC_READ_CONTROLLER_FILES = List.of(
        Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultController.java"),
        Path.of("src/main/java/com/vladislav/training/platform/assignment/controller/ManagerialCurrentSupervisionController.java"),
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/ManagerialHistoricalAnalyticsController.java"
        ),
        Path.of("src/main/java/com/vladislav/training/platform/analytics/controller/ExpertQuestionAnalyticsController.java")
    );

    private static final List<Path> WAVE_FIVE_PUBLIC_READ_QUERY_SERVICE_FILES = List.of(
        Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"),
        Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryServiceImpl.java"
        ),
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImpl.java"
        ),
        Path.of("src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImpl.java")
    );

    private static final List<Path> WAVE_FIVE_PUBLIC_READ_REPOSITORY_ADAPTER_FILES = List.of(
        Path.of("src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"),
        Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/"
                + "JpaManagerialCurrentSupervisionReadRepositoryAdapter.java"
        ),
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"
        ),
        Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                + "JpaExpertQuestionAnalyticsReadRepositoryAdapter.java"
        )
    );

    private static final List<String> FORBIDDEN_DEPENDENCY_TOKENS = List.of(
        "CapabilityAdmissionPolicy",
        "AssignmentCommandService",
        "AssignmentAdministrativeActionService",
        "AssignmentStatusRecalculationService",
        "ResultRecordingService",
        "AnalyticsRefreshService",
        "AnalyticsRebuildService",
        "AssignedAttemptSubmissionService",
        "AssignedAttemptSubmitTerminalService",
        "AssignedAttemptExpiryTerminalService",
        "SelfAttemptSubmitTerminalService",
        "SelfAttemptAbandonTerminalService",
        "SelfCurrentAttemptReadService",
        "AssignedCurrentAttemptReadService",
        "ActiveAttemptOwnerLocalReadService",
        "AnswerMutationService",
        "QuestionCommandService",
        "QuestionLifecycleService",
        "ContentLifecycleService",
        "ContentLifecycleController"
    );

    private static final List<String> FORBIDDEN_MUTATION_CALL_TOKENS = List.of(
        ".save(",
        ".delete(",
        "recordResult(",
        "submit(",
        "abandon(",
        "refresh(",
        "rebuild(",
        "recalculate(",
        "recover(",
        "triggerRecovery(",
        "reconcile("
    );

    private static final List<String> FORBIDDEN_REQUEST_PARAM_NAMES = List.of(
        "@RequestParam String refresh",
        "@RequestParam String rebuild",
        "@RequestParam String force",
        "@RequestParam String recalculate",
        "@RequestParam String recover",
        "@RequestParam String recovery",
        "@RequestParam(required = false) String refresh",
        "@RequestParam(required = false) String rebuild",
        "@RequestParam(required = false) String force",
        "@RequestParam(required = false) String recalculate",
        "@RequestParam(required = false) String recover",
        "@RequestParam(required = false) String recovery"
    );

    private static final List<String> FORBIDDEN_ROUTE_SEGMENT_TOKENS = List.of(
        "\"/refresh\"",
        "\"/rebuild\"",
        "\"/recalculate\"",
        "\"/recover\""
    );

    @Test
    void analyticsPublicReadControllersAvoidForbiddenOwnerWriteDependenciesAndMutationPaths() throws IOException {
        List<Path> existingFiles = existingFiles(WAVE_FIVE_PUBLIC_READ_CONTROLLER_FILES);

        assertThat(existingFiles).hasSize(WAVE_FIVE_PUBLIC_READ_CONTROLLER_FILES.size());
        assertFilesAvoidForbiddenTokens(existingFiles);
    }

    @Test
    void analyticsPublicReadQueryServicesAvoidForbiddenOwnerWriteDependenciesAndMutationPaths() throws IOException {
        List<Path> existingFiles = existingFiles(WAVE_FIVE_PUBLIC_READ_QUERY_SERVICE_FILES);

        assertThat(existingFiles).hasSize(WAVE_FIVE_PUBLIC_READ_QUERY_SERVICE_FILES.size());
        assertFilesAvoidForbiddenTokens(existingFiles);
    }

    @Test
    void analyticsReadRepositoryAdaptersAvoidWriteRefreshRebuildAndRecoveryPaths() throws IOException {
        List<Path> existingFiles = existingFiles(WAVE_FIVE_PUBLIC_READ_REPOSITORY_ADAPTER_FILES);

        assertThat(existingFiles).hasSize(WAVE_FIVE_PUBLIC_READ_REPOSITORY_ADAPTER_FILES.size());
        assertFilesAvoidForbiddenTokens(existingFiles);
    }

    @Test
    void analyticsPublicReadControllersDoNotPublishRefreshRebuildRecoverOrRecalculateRoutes() throws IOException {
        List<Path> existingFiles = existingFiles(WAVE_FIVE_PUBLIC_READ_CONTROLLER_FILES);

        assertThat(existingFiles).hasSize(WAVE_FIVE_PUBLIC_READ_CONTROLLER_FILES.size());

        for (Path file : existingFiles) {
            String source = Files.readString(file);

            for (String forbiddenRouteSegment : FORBIDDEN_ROUTE_SEGMENT_TOKENS) {
                assertThat(source)
                    
                    .doesNotContain(forbiddenRouteSegment);
            }
        }
    }

    private void assertFilesAvoidForbiddenTokens(List<Path> existingFiles) throws IOException {
        for (Path file : existingFiles) {
            String source = Files.readString(file);

            for (String forbiddenDependency : FORBIDDEN_DEPENDENCY_TOKENS) {
                assertThat(source)
                    
                    .doesNotContain(forbiddenDependency);
            }

            for (String forbiddenMutationCall : FORBIDDEN_MUTATION_CALL_TOKENS) {
                assertThat(source)
                    
                    .doesNotContain(forbiddenMutationCall);
            }

            for (String forbiddenRequestParam : FORBIDDEN_REQUEST_PARAM_NAMES) {
                assertThat(source)
                    
                    .doesNotContain(forbiddenRequestParam);
            }
        }
    }

    @Test
    void analyticsPublicReadBoundaryAllowsPassiveFreshnessMetadataWithoutRefreshInvocation() throws IOException {
        List<Path> existingFiles = existingFiles(List.of(
            Path.of(
                "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                    + "JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"
            ),
            Path.of("src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImpl.java")
        ));

        List<String> sources = existingFiles.stream()
            .map(this::read)
            .toList();

        assertThat(String.join("\n", sources))
            .contains("refreshedAt")
            .contains("calculatedAt")
            .doesNotContain("refresh(")
            .doesNotContain(".refresh(")
            .doesNotContain("rebuild(")
            .doesNotContain("recalculate(")
            .doesNotContain("recover(")
            .doesNotContain("triggerRecovery(")
            .doesNotContain("reconcile(");
    }

    @Test
    void analyticsPublicReadSourcesDoNotUseSplitStringFreshnessWorkaround() throws IOException {
        List<Path> existingFiles = existingFiles(List.of(
            Path.of(
                "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/"
                    + "JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"
            ),
            Path.of("src/test/java/com/vladislav/training/platform/common/web/PublicReadForbiddenDependencyRegressionTest.java")
        ));

        for (Path file : existingFiles) {
            String source = Files.readString(file);

            assertThat(source)
                
                .doesNotContain("property(\"re\", \"freshedAt\")")
                .doesNotContain("\"re\", \"freshedAt\"");
        }
    }

    private List<Path> existingFiles(List<Path> files) {
        return files.stream()
            .filter(Files::exists)
            .toList();
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}

