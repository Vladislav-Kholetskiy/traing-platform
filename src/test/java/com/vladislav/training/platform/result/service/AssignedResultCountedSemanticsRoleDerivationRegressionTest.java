package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AssignedResultCountedSemanticsRoleDerivation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AssignedResultCountedSemanticsRoleDerivationRegressionTest {

    @Test
    void assignedCountedSemanticsMustNotBeDerivedFromAssignmentRoleFlagInSnapshotFactsProvider() {
        String source = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingSnapshotFactsProvider.java");

        assertThat(source)
            
            .doesNotContain("boolean countedInAssignment = assignmentTest.assignmentTestRole() == AssignmentTestRole.FINAL_TOPIC_CONTROL;")
            .doesNotContain("boolean countedInAssignment = scoringSnapshot.passed() && withinDeadline;")
            .contains("AssignmentCountedResultDecision countedDecision = assignmentCountedResultPolicy.decide(")
            .contains("snapshotFinalTopicControlFlag,")
            .contains("scoringSnapshot,")
            .contains("withinDeadline")
            .contains("boolean countedInAssignment = countedDecision.countedInAssignment();")
            .contains("countedInAssignment,")
            .contains("assignmentTest.assignmentTestRole() == AssignmentTestRole.FINAL_TOPIC_CONTROL");
    }

    private String read(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + relativePath, exception);
        }
    }
}
