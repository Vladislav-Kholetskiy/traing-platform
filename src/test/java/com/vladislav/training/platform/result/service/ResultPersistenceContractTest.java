package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code ResultPersistence}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ResultPersistenceContractTest {

    @Test
    void activeAssignedAttemptLookupIsActorScoped() throws IOException {
        String contract = read("src/main/java/com/vladislav/training/platform/testing/repository/TestAttemptRepository.java");
        String springData = read(
            "src/main/java/com/vladislav/training/platform/testing/infrastructure/persistence/SpringDataTestAttemptJpaRepository.java"
        );

        assertThat(contract).contains("findActiveAssignedAttemptForActor(Long userId, Long assignmentTestId);");
        assertThat(contract).contains("findAndLockActiveAssignedAttemptForActor(Long userId, Long assignmentTestId);");
        assertThat(contract).doesNotContain("findActiveAssignedAttempt(Long assignmentTestId);");
        assertThat(contract).doesNotContain("findAndLockActiveAssignedAttempt(Long assignmentTestId);");

        assertThat(springData).contains("findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn(");
        assertThat(springData).contains("findByUserIdAndAssignmentTestIdAndStatusInForUpdate(");
        assertThat(springData).contains("ta.userId = :userId");
        assertThat(springData).doesNotContain("findFirstActive");
    }

    @Test
    void activeSelfAttemptLookupIsActorScoped() throws IOException {
        String contract = read("src/main/java/com/vladislav/training/platform/testing/repository/TestAttemptRepository.java");
        String springData = read(
            "src/main/java/com/vladislav/training/platform/testing/infrastructure/persistence/SpringDataTestAttemptJpaRepository.java"
        );

        assertThat(contract).contains("findActiveSelfAttempt(Long userId, Long testId);");
        assertThat(contract).contains("findAndLockActiveSelfAttempt(Long userId, Long testId);");
        assertThat(springData).contains("findByUserIdAndTestIdAndAttemptModeAndStatusIn(");
        assertThat(springData).contains("findByUserIdAndTestIdAndAttemptModeAndStatusInForUpdate(");
        assertThat(springData).contains("ta.userId = :userId");
        assertThat(springData).doesNotContain("findFirstActive");
    }

    @Test
    void submitLookupIsActorScoped() throws IOException {
        String selfSubmit = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitTerminalService.java"
        );
        String assignedSubmit = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java"
        );

        assertThat(selfSubmit).contains("findAndLockTestAttemptByIdAndUserId(testAttemptId, actorUserId)");
        assertThat(selfSubmit).doesNotContain("findAndLockTestAttemptById(testAttemptId)");

        assertThat(assignedSubmit).contains("refreshAssignedAttemptStatusCacheWithVerdict(");
        assertThat(assignedSubmit).contains("actorUserId,");
        assertThat(assignedSubmit).contains("assignmentTestId,");
        assertThat(assignedSubmit).contains("testAttemptId,");
        assertThat(assignedSubmit).doesNotContain("refreshAssignedAttemptStatusCache(testAttemptId, now)");
    }

    @Test
    void abandonLookupIsActorScoped() throws IOException {
        String selfAbandon = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonTerminalService.java"
        );

        assertThat(selfAbandon).contains("findAndLockTestAttemptByIdAndUserId(testAttemptId, actorUserId)");
        assertThat(selfAbandon).doesNotContain("findAndLockTestAttemptById(testAttemptId)");
    }

    @Test
    void resultWriteOnceByAttemptId() throws IOException {
        Table table = ResultEntity.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.uniqueConstraints())
            .extracting(UniqueConstraint::name)
            .contains("uq_result__test_att_id");
        assertThat(Arrays.stream(table.uniqueConstraints())
            .flatMap(uniqueConstraint -> Arrays.stream(uniqueConstraint.columnNames()))
            .collect(Collectors.toSet()))
            .contains("test_attempt_id");

        String repositoryContract = read("src/main/java/com/vladislav/training/platform/result/repository/ResultRepository.java");
        String serviceSource = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java");

        assertThat(repositoryContract).contains("Result findResultByTestAttemptId(Long testAttemptId);");
        assertThat(repositoryContract).doesNotContain("updateResult(");
        assertThat(repositoryContract).doesNotContain("deleteResult(");
        assertThat(serviceSource).contains("Result existingResult = resultRepository.findResultByTestAttemptId(testAttemptId);");
        assertThat(serviceSource).contains("Result canonicalResult = resultRepository.findResultByTestAttemptId(testAttemptId);");
    }

    @Test
    void resultSnapshotChildrenPersistWithRootAggregate() throws IOException {
        String serviceSource = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java");

        int saveResultIndex = serviceSource.indexOf("savedResult = resultRepository.saveResult(assembledResult);");
        int materializeIndex = serviceSource.indexOf(
            "subordinateSnapshotMaterializer.materialize(savedResult, terminalizedAttempt, snapshotFacts);"
        );
        int auditIndex = serviceSource.indexOf("recordResultAudit(savedResult, terminalizedAttempt.status());");
        int handoffIndex = serviceSource.indexOf(
            "assignmentCountedResultHandoffService.acceptValidCountedAssignmentResult(savedResult.id());"
        );

        assertThat(saveResultIndex).isGreaterThanOrEqualTo(0);
        assertThat(materializeIndex).isGreaterThan(saveResultIndex);
        assertThat(auditIndex).isGreaterThan(materializeIndex);
        assertThat(handoffIndex).isGreaterThan(auditIndex);
    }

    @Test
    void scoreScaleCompatibleWithV100() throws IOException {
        String ddl = read("src/main/resources/db/migration/V100__full_schema_stack.sql");
        String resultInvariant = read(
            "src/test/java/com/vladislav/training/platform/result/infrastructure/persistence/ResultPersistenceInvariantIntegrationTest.java"
        );

        assertThat(ddl).contains("threshold_percent numeric(7,4) not null");
        assertThat(ddl).contains("earned_score numeric(14,4) not null");
        assertThat(ddl).contains("max_score numeric(14,4) not null");
        assertThat(ddl).contains("score_percent numeric(7,4) not null");
        assertThat(ddl).contains("weight numeric(12,4) not null");
        assertThat(resultInvariant)
            .contains("\"70.0000\"")
            .contains("\"8.0000\"")
            .contains("\"10.0000\"")
            .contains("\"80.0000\"")
            .contains("\"1.0000\"");
    }

    @Test
    void enumValuesCompatibleWithV100() throws IOException {
        String ddl = read("src/main/resources/db/migration/V100__full_schema_stack.sql");

        assertThat(Arrays.stream(AttemptMode.values()).map(Enum::name).collect(Collectors.toSet()))
            .isEqualTo(Set.of("ASSIGNED", "SELF"));
        assertThat(Arrays.stream(TestAttemptStatus.values()).map(Enum::name).collect(Collectors.toSet()))
            .isEqualTo(Set.of("STARTED", "IN_PROGRESS", "COMPLETED", "EXPIRED", "ABANDONED"));
        assertThat(Arrays.stream(ResultQuestionType.values()).map(Enum::name).collect(Collectors.toSet()))
            .isEqualTo(Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "MATCHING", "ORDERING"));

        assertThat(ddl).contains("(attempt_mode = 'ASSIGNED' and assignment_test_id is not null)");
        assertThat(ddl).contains("(attempt_mode = 'SELF' and assignment_test_id is null)");
        assertThat(ddl).contains("question_type text not null");
        assertThat(ddl).contains("status text not null");
    }

    @Test
    void noMainDependencyOnTestOnlyCollaborators() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            String mainSources = files
                .filter(path -> path.toString().endsWith(".java"))
                .map(this::readUnchecked)
                .collect(Collectors.joining("\n"));

            assertThat(mainSources).doesNotContain("org.junit");
            assertThat(mainSources).doesNotContain("org.mockito");
            assertThat(mainSources).doesNotContain("@MockBean");
            assertThat(mainSources).doesNotContain("@TestConfiguration");
            assertThat(mainSources).doesNotContain("src/test/java");
        }
    }

    @Test
    void noNonActorScopedActiveAttemptRepositoryMethod() throws IOException {
        Set<String> contractMethods = Arrays.stream(TestAttemptRepository.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());
        Set<String> springDataMethods = Arrays.stream(SpringDataTestAttemptJpaRepository.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());
        String contractSource = read("src/main/java/com/vladislav/training/platform/testing/repository/TestAttemptRepository.java");
        String springDataSource = read(
            "src/main/java/com/vladislav/training/platform/testing/infrastructure/persistence/SpringDataTestAttemptJpaRepository.java"
        );

        assertThat(contractMethods).doesNotContain("findActiveAssignedAttempt", "findAndLockActiveAssignedAttempt");
        assertThat(springDataMethods).doesNotContain("findFirstActiveAssignedAttempt", "findFirstActiveSelfAttempt");
        assertThat(contractSource).doesNotContain("findFirstActive");
        assertThat(springDataSource).doesNotContain("findFirstActive");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }
}
