package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultTestExpectation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultTestExpectationRegressionTest {

    private static final String LEGACY_BLOCKED_MESSAGE =
        "Immutable result-side baseline does not expose a materialized actor anchor";

    private static final Path QUERY_SERVICE_CONTRACT_TEST = Path.of(
        "src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImplTest.java"
    );
    private static final Path CONTROLLER_TEST = Path.of(
        "src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultControllerTest.java"
    );
    private static final Path QUERY_SERVICE_RUNTIME_WIRING_TEST = Path.of(
        "src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceRuntimeWiringTest.java"
    );
    private static final Path READ_SEAM_RUNTIME_WIRING_TEST = Path.of(
        "src/test/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReaderRuntimeWiringTest.java"
    );
    private static final Path READ_SEAM_TEST = Path.of(
        "src/test/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReaderTest.java"
    );

    private static final List<Path> ALLOWED_SUCCESS_EXPECTATION_TESTS = List.of(
        CONTROLLER_TEST,
        QUERY_SERVICE_CONTRACT_TEST,
        QUERY_SERVICE_RUNTIME_WIRING_TEST,
        READ_SEAM_RUNTIME_WIRING_TEST,
        READ_SEAM_TEST
    );

    private static final List<Path> SELF_HISTORY_TEST_FILES = List.of(
        Path.of("src/test/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextSelfScopeContractTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/access/service/AccessSpecificationPolicyContractTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicyTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/access/service/SelfResultHistoryContourWiringRegressionTest.java"),
        READ_SEAM_TEST,
        Path.of("src/test/java/com/vladislav/training/platform/result/query/ResultSelfHistoryImmutableBaselineDiscoveryTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultApiExposureRegressionTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultAttemptBackdoorRegressionTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultContourSeparationRegressionTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultExternalReferenceRegressionTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultHttpMappingPerimeterRegressionTest.java"),
        Path.of("src/test/java/com/vladislav/training/platform/result/query/SelfHistoricalResultPerimeterWiringRegressionTest.java"),
        CONTROLLER_TEST,
        QUERY_SERVICE_CONTRACT_TEST,
        QUERY_SERVICE_RUNTIME_WIRING_TEST,
        READ_SEAM_RUNTIME_WIRING_TEST,
        Path.of("src/test/java/com/vladislav/training/platform/testing/service/SelfHistoryTestingSideAttemptBackdoorRegressionTest.java")
    );

    @Test
    void runtimeCompleteSelfHistoryTestTreeRemovesLegacyBlockedExpectationsAndKeepsResultRootedSuccessPathsBounded() {
        assertThat(SELF_HISTORY_TEST_FILES).allMatch(Files::exists);

        String queryServiceContractTestSource = read(QUERY_SERVICE_CONTRACT_TEST);
        assertThat(queryServiceContractTestSource)
            
            .contains("new SelfHistoricalResultReadRow(")
            .contains("new SelfHistoricalResultReadModel(")
            .contains("allowPolicyCallsReadSeamAfterSelfHistoryContextResolutionAndMapsRows")
            .doesNotContain("findSelfHistoricalResultsPropagatesFailClosedBlockerWithoutFallbackOrMasking");

        assertThat(read(CONTROLLER_TEST))
            .contains("new SelfHistoricalResultReadModel(")
            .doesNotContain(LEGACY_BLOCKED_MESSAGE);

        assertThat(read(QUERY_SERVICE_RUNTIME_WIRING_TEST))
            .contains("new SelfHistoricalResultReadModel(")
            .doesNotContain("blocked runtime contour");

        assertThat(read(READ_SEAM_RUNTIME_WIRING_TEST))
            .contains("new SelfHistoricalResultReadRow(")
            .contains("findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc")
            .doesNotContain("seamPublishesFailClosedBlockerWithoutRepositoryBackdoorQuery")
            .doesNotContain(LEGACY_BLOCKED_MESSAGE)
            .doesNotContain("blocked runtime contour");

        for (Path testFile : SELF_HISTORY_TEST_FILES) {
            String source = read(testFile);
            assertThat(source)
                
                .doesNotContain(LEGACY_BLOCKED_MESSAGE)
                .doesNotContain("blocked runtime contour")
                .doesNotContain("findSelfHistoricalResultsPropagatesFailClosedBlockerWithoutFallbackOrMasking");

            if (ALLOWED_SUCCESS_EXPECTATION_TESTS.contains(testFile)) {
                assertThat(source)
                    
                    .containsAnyOf("new SelfHistoricalResultReadRow(", "new SelfHistoricalResultReadModel(");
                continue;
            }

            assertThat(source)
                
                .doesNotContain("new SelfHistoricalResultReadRow(")
                .doesNotContain("new SelfHistoricalResultReadModel(");
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}

