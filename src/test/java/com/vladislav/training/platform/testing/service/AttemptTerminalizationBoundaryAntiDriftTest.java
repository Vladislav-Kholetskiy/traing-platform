package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Не даёт {@code AttemptTerminalizationBoundary} незаметно уйти от текущего замысла.
 * Тест страхует важные ограничения и исходную идею решения.
 */
class AttemptTerminalizationBoundaryAntiDriftTest {

    @Test
    void assignedSubmitEntryFlowMustNotDelegateIntoActorlessTerminalCore() throws Exception {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"))
            
            .doesNotContain("assignedAttemptSubmitTerminalService.submitAssignedAttempt(testAttemptId)");

        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmissionService.java"))
            
            .doesNotContain("assignedAttemptSubmitSequencingService.submitAssignedAttempt(testAttemptId)");
    }

    @Test
    void selfSubmitAndAbandonEntryFlowsMustNotDelegateIntoActorlessTerminalCores() throws Exception {
        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java"))
            
            .doesNotContain("selfAttemptSubmitTerminalService.submitSelfAttempt(testAttemptId)");

        assertThat(read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonSequencingService.java"))
            
            .doesNotContain("selfAttemptAbandonTerminalService.abandonSelfAttempt(testAttemptId)");
    }

    @Test
    void normalSubmitReturnsExplicitTerminalizationOutcomeWithResultRecordableTrue() throws Exception {
        String assignedTerminalSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java"
        );
        String selfTerminalSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitTerminalService.java"
        );
        String assignedSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        );
        String selfSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java"
        );
        String outcomeSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        );
        String reasonSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationReason.java"
        );
        String allSubmitSources = assignedTerminalSource + selfTerminalSource + assignedSequencingSource + selfSequencingSource;

        assertThat(allSubmitSources + outcomeSource + reasonSource)
            
            .contains("AttemptTerminalizationOutcome");
        assertThat(outcomeSource + reasonSource)
            .contains("AttemptTerminalizationReason")
            .contains("resultRecordable")
            .doesNotContain("countedHandoffEligible")
            .contains("auditEventType")
            .contains("Objects.requireNonNull(auditEventType");
        assertThat(assignedTerminalSource)
            
            .doesNotContain("public TestAttempt submitAssignedAttempt(")
            .contains("public AttemptTerminalizationOutcome submitAssignedAttempt(")
            .contains("TestAttemptStatus.COMPLETED")
            .contains("AttemptTerminalizationOutcome.assignedNormalSubmit(");
        assertThat(selfTerminalSource)
            
            .doesNotContain("public TestAttempt submitSelfAttempt(")
            .contains("public AttemptTerminalizationOutcome submitSelfAttempt(")
            .contains("TestAttemptStatus.COMPLETED")
            .contains("AttemptTerminalizationOutcome.selfNormalSubmit(");
        assertThat(assignedSequencingSource)
            
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("terminalizationOutcome.countedHandoffEligible()")
            .doesNotContain("terminalizedAttempt.status() == TestAttemptStatus.EXPIRED")
            .doesNotContain("terminalizedAttempt.status() != TestAttemptStatus.COMPLETED");
        assertThat(assignedTerminalSource)
            .contains("AttemptTerminalizationOutcome.expiredByRefresh(");
        assertThat(outcomeSource)
            .contains("AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()");
        assertThat(selfSequencingSource)
            
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("resultRecordingService.recordResult(terminalizedAttempt.id())");
    }

    @Test
    void abandonReturnsExplicitTerminalizationOutcomeWithResultRecordableFalse() throws Exception {
        String abandonTerminalSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonTerminalService.java"
        );
        String abandonSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonSequencingService.java"
        );
        String outcomeSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        );
        String reasonSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationReason.java"
        );

        assertThat(abandonTerminalSource + abandonSequencingSource + outcomeSource + reasonSource)
            
            .contains("AttemptTerminalizationOutcome");
        assertThat(abandonTerminalSource)
            
            .doesNotContain("public TestAttempt abandonSelfAttempt(")
            .contains("public AttemptTerminalizationOutcome abandonSelfAttempt(")
            .contains("AttemptTerminalizationOutcome.selfAbandon(");
        assertThat(reasonSource)
            
            .contains("SELF_ABANDON");
        assertThat(outcomeSource)
            
            .contains("selfAbandon(Long actorUserId, TestAttempt terminalizedAttempt)")
            .contains("AttemptTerminalizationReason.SELF_ABANDON")
            .contains("false")
            .contains("terminalizedAttempt.abandonedAt()")
            .contains("auditEventType")
            .contains("AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_ABANDONED.auditEventType()");
        assertThat(abandonSequencingSource)
            
            .contains("AttemptTerminalizationOutcome terminalizationOutcome =")
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("resultRecordingService.recordResult(")
            .doesNotContain("AssignmentCountedResultHandoffService");
        assertThat(abandonTerminalSource)
            
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentCountedResultHandoffService");
    }

    @Test
    void submitSequencingDrivesResultRecordingOnlyFromTerminalizationOutcomeFlags() throws Exception {
        String assignedSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        );
        String selfSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java"
        );
        String outcomeSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        );

        assertThat(assignedSequencingSource)
            
            .contains("terminalizationOutcome.resultRecordable()")
            .contains("resultRecordingService.recordResult(terminalizationOutcome.attemptId())")
            .doesNotContain("assignmentCountedResultHandoffService.acceptValidCountedAssignmentResult(")
            .doesNotContain("resultRecordingService.recordResult(terminalizedAttempt.id())")
            .doesNotContain("terminalizedAttempt.status() == TestAttemptStatus.EXPIRED")
            .doesNotContain("terminalizedAttempt.status() != TestAttemptStatus.COMPLETED")
            .doesNotContain("terminalizedAttempt.status() == TestAttemptStatus.COMPLETED")
            .doesNotContain("auditEventType()")
            .doesNotContain("AttemptTerminalCriticalAuditCatalog")
            .doesNotContain("AuditEventType");

        int nonRecordableGuardIndex = assignedSequencingSource.indexOf("if (!terminalizationOutcome.resultRecordable())");
        int expiredReturnIndex = assignedSequencingSource.indexOf(
            "return AssignedAttemptSubmitOutcome.expired(terminalizationOutcome.attemptId());"
        );
        int recordResultIndex = assignedSequencingSource.indexOf(
            "resultRecordingService.recordResult(terminalizationOutcome.attemptId())"
        );
        assertThat(nonRecordableGuardIndex).isGreaterThanOrEqualTo(0);
        assertThat(expiredReturnIndex).isGreaterThan(nonRecordableGuardIndex);
        assertThat(recordResultIndex).isGreaterThan(expiredReturnIndex);
        assertThat(assignedSequencingSource.substring(nonRecordableGuardIndex, expiredReturnIndex))
            
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("resultRecordingService.recordResult(")
            .doesNotContain("acceptValidCountedAssignmentResult(");

        assertThat(selfSequencingSource)
            
            .contains("terminalizationOutcome.resultRecordable()")
            .contains("if (!terminalizationOutcome.resultRecordable())")
            .contains("resultRecordingService.recordResult(terminalizationOutcome.attemptId())")
            .doesNotContain("AssignmentCountedResultHandoffService")
            .doesNotContain("acceptValidCountedAssignmentResult")
            .doesNotContain("resultRecordingService.recordResult(terminalizedAttempt.id())")
            .doesNotContain("terminalizedAttempt.status()")
            .doesNotContain("auditEventType()")
            .doesNotContain("AttemptTerminalCriticalAuditCatalog")
            .doesNotContain("AuditEventType");
        int selfNonRecordableGuardIndex = selfSequencingSource.indexOf("if (!terminalizationOutcome.resultRecordable())");
        int selfRecordResultIndex = selfSequencingSource.indexOf(
            "resultRecordingService.recordResult(terminalizationOutcome.attemptId())"
        );
        assertThat(selfNonRecordableGuardIndex).isGreaterThanOrEqualTo(0);
        assertThat(selfRecordResultIndex).isGreaterThan(selfNonRecordableGuardIndex);
        assertThat(selfSequencingSource.substring(selfNonRecordableGuardIndex, selfRecordResultIndex))
            
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("resultRecordingService.recordResult(");

        int assignedNormalFactoryIndex = outcomeSource.indexOf(
            "public static AttemptTerminalizationOutcome assignedNormalSubmit(Long actorUserId, TestAttempt terminalizedAttempt)"
        );
        int selfNormalFactoryIndex = outcomeSource.indexOf(
            "public static AttemptTerminalizationOutcome selfNormalSubmit(Long actorUserId, TestAttempt terminalizedAttempt)"
        );
        int expiredFactoryIndex = outcomeSource.indexOf(
            "public static AttemptTerminalizationOutcome expiredByRefresh(Long actorUserId, TestAttempt terminalizedAttempt)"
        );
        int selfAbandonFactoryIndex = outcomeSource.indexOf(
            "public static AttemptTerminalizationOutcome selfAbandon(Long actorUserId, TestAttempt terminalizedAttempt)"
        );
        assertThat(outcomeSource)
            
            .contains("boolean resultRecordable")
            .doesNotContain("boolean countedHandoffEligible")
            .contains("AuditEventType auditEventType")
            .contains("assignedNormalSubmit(")
            .contains("selfNormalSubmit(")
            .contains("expiredByRefresh(")
            .contains("selfAbandon(");
        assertThat(assignedNormalFactoryIndex).isGreaterThanOrEqualTo(0);
        assertThat(selfNormalFactoryIndex).isGreaterThan(assignedNormalFactoryIndex);
        assertThat(expiredFactoryIndex).isGreaterThan(selfNormalFactoryIndex);
        assertThat(selfAbandonFactoryIndex).isGreaterThan(expiredFactoryIndex);
        assertThat(outcomeSource.substring(assignedNormalFactoryIndex, selfNormalFactoryIndex))
            .contains("true,");
        assertThat(outcomeSource.substring(selfNormalFactoryIndex, expiredFactoryIndex))
            .contains("true,");
        assertThat(outcomeSource.substring(expiredFactoryIndex, selfAbandonFactoryIndex))
            .contains("false,");
        assertThat(outcomeSource.substring(selfAbandonFactoryIndex))
            .contains("false,");
    }

    @Test
    void assignedExplicitExpiryReturnsNonRecordableTerminalizationOutcomeAndDoesNotRecordResult() throws Exception {
        String expirySource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptExpiryTerminalService.java"
        );
        String outcomeSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        );
        String reasonSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationReason.java"
        );

        assertThat(expirySource + outcomeSource + reasonSource)
            
            .contains("AttemptTerminalizationOutcome");
        assertThat(expirySource)
            .contains("AttemptTerminalizationOutcome.assignedExplicitExpiry(actorUserId, expiredAttempt).terminalizedAttempt()")
            .doesNotContain("resultRecordingService.recordResult(terminalizationOutcome.attemptId())")
            .doesNotContain("resultRecordingService.recordResult(expiredAttempt.id())");
        assertThat(outcomeSource)
            .contains("assignedExplicitExpiry(")
            .contains("AttemptTerminalizationReason.ASSIGNED_EXPLICIT_EXPIRY")
            .contains("AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()")
            .contains("false,")
            .doesNotContain("countedHandoffEligible")
            .doesNotContain("countedInAssignment");
        assertThat(reasonSource)
            .contains("ASSIGNED_EXPLICIT_EXPIRY");
    }

    @Test
    void terminalizationOutcomeAloneCannotTriggerCountedHandoff() throws Exception {
        String assignedSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        );
        String selfAbandonSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonSequencingService.java"
        );
        String outcomeSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        );

        assertThat(outcomeSource)
            .contains("boolean resultRecordable")
            .doesNotContain("countedHandoffEligible")
            .doesNotContain("countedInAssignment");
        assertThat(assignedSequencingSource)
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("acceptValidCountedAssignmentResult(");
        assertThat(selfAbandonSequencingSource)
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("acceptValidCountedAssignmentResult(")
            .doesNotContain("ResultRecordingService");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
