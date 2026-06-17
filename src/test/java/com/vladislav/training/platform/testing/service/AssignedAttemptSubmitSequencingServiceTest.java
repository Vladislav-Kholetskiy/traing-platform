package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignedAttemptSubmitSequencing}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptSubmitSequencingServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T19:30:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private AssignedAttemptSubmitTerminalService assignedAttemptSubmitTerminalService;
    @Mock
    private ResultRecordingService resultRecordingService;
    private AssignedAttemptSubmitSequencingService facade;

    @BeforeEach
    void setUp() {
        facade = new AssignedAttemptSubmitSequencingService(
            assignedAttemptSubmitTerminalService,
            resultRecordingService
        );
    }

    @Test
    void submitAssignedAttemptTerminalizesFirstThenRecordsCanonicalResult() {
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9001L);
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9001L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9001L)).thenReturn(7001L);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9001L);

        assertThat(outcome.attemptId()).isEqualTo(9001L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(outcome.recordedResult()).isEqualTo(7001L);
        assertThat(outcome.completedWithResult()).isTrue();
        InOrder inOrder = inOrder(assignedAttemptSubmitTerminalService, resultRecordingService);
        inOrder.verify(assignedAttemptSubmitTerminalService).submitAssignedAttempt(ACTOR_USER_ID, 701L, 9001L);
        inOrder.verify(resultRecordingService).recordResult(9001L);
    }

    @Test
    void resultRecordingUsesTerminalizedAttemptIdReturnedByAssignedSubmitOwnerService() {
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9002L);
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 1234L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9002L)).thenReturn(7002L);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 1234L);

        assertThat(outcome.recordedResult()).isEqualTo(7002L);
        verify(resultRecordingService).recordResult(9002L);
    }

    @Test
    void whenAssignedSubmitFailsResultRecordingIsNotCalled() {
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9003L))
            .thenThrow(new ConflictException("assigned submit failed"));

        assertThatThrownBy(() -> facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9003L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("assigned submit failed");

        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void facadeReturnsCanonicalResultIdFromResultRecordingService() {
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9004L);
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9004L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9004L)).thenReturn(7004L);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9004L);

        assertThat(outcome.recordedResult()).isEqualTo(7004L);
        assertThat(outcome.completedWithResult()).isTrue();
    }

    @Test
    void facadeDoesNotInteractWithAssignmentSideDirectly() {
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9005L);
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9005L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9005L)).thenReturn(7005L);

        facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9005L);

        verify(resultRecordingService).recordResult(9005L);
    }

    @Test
    void expiredAssignedSubmitDoesNotRecordResultByDefault() {
        TestAttempt expiredAttempt = new TestAttempt(
            9006L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.EXPIRED,
            FIXED_INSTANT.minusSeconds(900),
            null,
            FIXED_INSTANT,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT
        );
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9006L))
            .thenReturn(AttemptTerminalizationOutcome.expiredByRefresh(ACTOR_USER_ID, expiredAttempt));

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9006L);

        assertThat(outcome.attemptId()).isEqualTo(9006L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(outcome.recordedResult()).isNull();
        assertThat(outcome.expiredWithoutResult()).isTrue();
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void expiredAssignedSubmitDoesNotTriggerCountedHandoff() {
        TestAttempt expiredAttempt = new TestAttempt(
            9008L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.EXPIRED,
            FIXED_INSTANT.minusSeconds(900),
            null,
            FIXED_INSTANT,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT
        );
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9008L))
            .thenReturn(AttemptTerminalizationOutcome.expiredByRefresh(ACTOR_USER_ID, expiredAttempt));

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9008L);

        assertThat(outcome.attemptId()).isEqualTo(9008L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(outcome.recordedResult()).isNull();
        assertThat(outcome.expiredWithoutResult()).isTrue();
        verify(assignedAttemptSubmitTerminalService).submitAssignedAttempt(ACTOR_USER_ID, 701L, 9008L);
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void assignedSubmitCompletedBranchTriggersCountedHandoffOnlyAfterRecordedResult() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        ));

        int terminalIndex = source.indexOf("assignedAttemptSubmitTerminalService.submitAssignedAttempt(");
        int resultIndex = source.indexOf("resultRecordingService.recordResult(");
        assertThat(terminalIndex).isGreaterThanOrEqualTo(0);
        assertThat(resultIndex)
            
            .isGreaterThan(terminalIndex);
        assertThat(source)
            .contains("resultRecordable()")
            .contains("recordedResult")
            .doesNotContain("countedHandoffEligible()")
            .doesNotContain("assignmentCountedResultHandoffService.acceptValidCountedAssignmentResult(");
    }

    @Test
    void assignedSubmitDoesNotTriggerCountedHandoffWhenResultRecordingFails() {
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9010L);
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9010L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9010L)).thenThrow(new ConflictException("result recording failed"));

        assertThatThrownBy(() -> facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9010L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("result recording failed");

        verify(assignedAttemptSubmitTerminalService).submitAssignedAttempt(ACTOR_USER_ID, 701L, 9010L);
        verify(resultRecordingService).recordResult(9010L);
        verifyNoMoreInteractions(resultRecordingService);
    }

    @Test
    void completedOutcomeRequiresAttemptIdStatusAndRecordedResult() {
        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9001L, 7001L);

        assertThat(outcome.attemptId()).isEqualTo(9001L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(outcome.recordedResult()).isEqualTo(7001L);
        assertThat(outcome.completedWithResult()).isTrue();
        assertThat(outcome.expiredWithoutResult()).isFalse();
    }

    @Test
    void expiredOutcomeRequiresAttemptIdAndHasNoRecordedResult() {
        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.expired(9002L);

        assertThat(outcome.attemptId()).isEqualTo(9002L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(outcome.recordedResult()).isNull();
        assertThat(outcome.expiredWithoutResult()).isTrue();
        assertThat(outcome.completedWithResult()).isFalse();
    }

    @Test
    void completedOutcomeRejectsNullRecordedResult() {
        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9001L,
            TestAttemptStatus.COMPLETED,
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("completed submit outcome requires recordedResult");
    }

    @Test
    void expiredOutcomeRejectsRecordedResult() {
        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9001L,
            TestAttemptStatus.EXPIRED,
            7001L
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("expired submit outcome must not carry recordedResult");
    }

    @Test
    void submitOutcomeRejectsUnsupportedStatuses() {
        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9001L,
            TestAttemptStatus.IN_PROGRESS,
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("supports only COMPLETED with recordedResult or EXPIRED without recordedResult");

        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9001L,
            TestAttemptStatus.ABANDONED,
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("supports only COMPLETED with recordedResult or EXPIRED without recordedResult");
    }

    @Test
    void submitOutcomeRejectsNullAttemptIdAndNullStatus() {
        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            null,
            TestAttemptStatus.COMPLETED,
            7001L
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("attemptId must not be null");

        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9001L,
            null,
            7001L
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("terminalStatus must not be null");
    }

    @Test
    void sequencingSourceDoesNotContainReturnNullAndSkipsRecordResultForExpiredBranch() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        ));

        assertThat(source).doesNotContain("return null");
        int expiredIndex = source.indexOf("if (!terminalizationOutcome.resultRecordable())");
        int recordIndex = source.indexOf("resultRecordingService.recordResult(");
        assertThat(expiredIndex).isGreaterThanOrEqualTo(0);
        assertThat(recordIndex).isGreaterThan(expiredIndex);
    }

    @Test
    void expiredAssignedSubmitBranchShortCircuitsBeforeResultRecordingAndCountedHandoffUsingOutcomeFlags() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        ));

        int expiredGuardIndex = source.indexOf("if (!terminalizationOutcome.resultRecordable())");
        int expiredReturnIndex = source.indexOf("return AssignedAttemptSubmitOutcome.expired(terminalizationOutcome.attemptId());");
        int recordIndex = source.indexOf("resultRecordingService.recordResult(");
        assertThat(expiredGuardIndex).isGreaterThanOrEqualTo(0);
        assertThat(expiredReturnIndex).isGreaterThan(expiredGuardIndex);
        assertThat(recordIndex).isGreaterThan(expiredReturnIndex);
        String expiredBranch = source.substring(expiredGuardIndex, expiredReturnIndex);
        assertThat(expiredBranch)
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("resultRecordingService.recordResult(")
            .doesNotContain("assignmentCountedResultHandoffService.acceptValidCountedAssignmentResult(");
    }

    @Test
    void sequencingSourceHardensAssignedSubmitOutcomeToCompletedOrExpiredOnly() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        ));

        assertThat(source)
            .contains("AssignedAttemptSubmitOutcome")
            .contains("TestAttemptStatus.COMPLETED")
            .contains("TestAttemptStatus.EXPIRED")
            .contains("IllegalArgumentException")
            .contains("supports only COMPLETED with recordedResult or EXPIRED without recordedResult")
            .doesNotContain("return null");
    }

    @Test
    void unsupportedNonCompletedNonExpiredTerminalStatusFailsClosed() {
        TestAttempt abandonedAttempt = new TestAttempt(
            9007L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.ABANDONED,
            FIXED_INSTANT.minusSeconds(900),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT
        );
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9007L))
            .thenReturn(new AttemptTerminalizationOutcome(
                abandonedAttempt.id(),
                ACTOR_USER_ID,
                AttemptMode.ASSIGNED,
                TestAttemptStatus.ABANDONED,
                FIXED_INSTANT,
                AttemptTerminalizationReason.NORMAL_SUBMIT,
                false,
                AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_SUBMITTED.auditEventType(),
                abandonedAttempt
            ));

        assertThatThrownBy(() -> facade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9007L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-recordable terminalization outcome")
            .hasMessageContaining("ABANDONED");

        verify(resultRecordingService, never()).recordResult(org.mockito.ArgumentMatchers.anyLong());
    }

    private TestAttempt completedAssignedAttempt(Long attemptId) {
        return new TestAttempt(
            attemptId,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT
        );
    }
}
