package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader.CountedAssignmentResultHandoffFacts;
import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader.StatusDefiningCountedResultFacts;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Собирает набор регрессионных проверок вокруг {@code AssignmentAssignedCompletionSchema}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAssignedCompletionSchemaRegressionPackTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T14:00:00Z");

    @Mock
    private AssignmentStatusDefiningCountedResultFactsReader handoffFactsReader;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;
    @Mock
    private AssignmentCommandService assignmentCommandService;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    @Mock
    private AssignmentStatusDefiningCountedResultFactsReader countedResultFactsReader;

    @Test
    void handoffDelegatesOwnerLocalClosureAndDoesNotWriteAssignmentTestDirectly() {
        AssignmentCountedResultHandoffServiceImpl handoff = new AssignmentCountedResultHandoffServiceImpl(
            handoffFactsReader,
            assignmentTestRepository,
            assignmentCommandService
        );
        CountedAssignmentResultHandoffFacts canonicalResult = assignedResult(501L, 801L, 701L);
        when(handoffFactsReader.findCountedAssignmentResultHandoffFactsByResultId(501L)).thenReturn(canonicalResult);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(openAssignmentTest(701L, 801L, null));

        handoff.acceptValidCountedAssignmentResult(501L);

        verify(assignmentCommandService).closeAssignmentTestWithCountedResult(701L, 501L, FIXED_INSTANT);
        verify(assignmentTestRepository, never()).saveAssignmentTest(any());
    }

    @Test
    void assignmentOwnerClosureMaterializesSchemaConsistentClosedAssignmentTestState() {
        AssignmentCommandServiceImpl service = new AssignmentCommandServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            assignmentStatusRecalculationService
        );
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(openAssignmentTest(77L, 41L, null));
        when(assignmentTestRepository.saveAssignmentTest(any(AssignmentTest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, AssignmentTest.class));

        AssignmentTest closed = service.closeAssignmentTestWithCountedResult(77L, 901L, FIXED_INSTANT);

        assertThat(closed.countedResultId()).isEqualTo(901L);
        assertThat(closed.closedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(closed.isClosed()).isTrue();
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, FIXED_INSTANT);
    }

    @Test
    void halfStateRepairIsFixedAsOwnerLocalClosureInsteadOfLeavingAssignmentTestOpen() {
        AssignmentCommandServiceImpl service = new AssignmentCommandServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            assignmentStatusRecalculationService
        );
        AssignmentTest halfClosed = openAssignmentTest(77L, 41L, 901L);
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(halfClosed);
        when(assignmentTestRepository.saveAssignmentTest(any(AssignmentTest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, AssignmentTest.class));

        service.closeAssignmentTestWithCountedResult(77L, 901L, FIXED_INSTANT);

        ArgumentCaptor<AssignmentTest> savedCaptor = ArgumentCaptor.forClass(AssignmentTest.class);
        verify(assignmentTestRepository).saveAssignmentTest(savedCaptor.capture());
        assertThat(savedCaptor.getValue().countedResultId()).isEqualTo(901L);
        assertThat(savedCaptor.getValue().closedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(savedCaptor.getValue().isClosed()).isTrue();
    }

    @Test
    void differentCanonicalResultOverwriteRemainsForbidden() {
        AssignmentCommandServiceImpl service = new AssignmentCommandServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            assignmentStatusRecalculationService
        );
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(closedAssignmentTest(77L, 41L, 901L));

        assertThatThrownBy(() -> service.closeAssignmentTestWithCountedResult(77L, 999L, FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already closed by counted result");

        verify(assignmentTestRepository, never()).saveAssignmentTest(any());
    }

    @Test
    void statusRecalculationDoesNotTreatCountedResultIdWithoutClosureAsCompletedOwnerState() {
        AssignmentStatusRecalculationServiceImpl service = new AssignmentStatusRecalculationServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            countedResultFactsReader
        );
        when(assignmentRepository.findAssignmentById(42L)).thenReturn(activeAssignment(42L));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(42L)).thenReturn(List.of(
            openAssignmentTest(52L, 42L, 7002L)
        ));

        AssignmentStatus status = service.recalculateAssignmentStatus(42L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.ASSIGNED);
        verify(countedResultFactsReader, never()).findStatusDefiningFactsByCountedResultId(any());
    }

    @Test
    void statusRecalculationCanProduceCompletedOnlyAfterCanonicalProofAndClosedAssignmentTest() {
        AssignmentStatusRecalculationServiceImpl service = new AssignmentStatusRecalculationServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            countedResultFactsReader
        );
        when(assignmentRepository.findAssignmentById(43L)).thenReturn(activeAssignment(43L));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(43L)).thenReturn(List.of(
            closedAssignmentTest(53L, 43L, 7003L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7003L))
            .thenReturn(new StatusDefiningCountedResultFacts(true, true, true));

        AssignmentStatus status = service.recalculateAssignmentStatus(43L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    void selfResultPathDoesNotReceiveAssignmentOwnedClosure() {
        AssignmentCountedResultHandoffServiceImpl handoff = new AssignmentCountedResultHandoffServiceImpl(
            handoffFactsReader,
            assignmentTestRepository,
            assignmentCommandService
        );
        when(handoffFactsReader.findCountedAssignmentResultHandoffFactsByResultId(601L)).thenReturn(selfResult(601L));

        handoff.acceptValidCountedAssignmentResult(601L);

        verify(assignmentTestRepository, never()).findAssignmentTestById(any());
        verify(assignmentCommandService, never()).closeAssignmentTestWithCountedResult(any(), any(), any());
    }

    @Test
    void assignedCompletionChainDoesNotDriftIntoGenericResultCompletesEverythingVocabulary() throws IOException {
        String handoffSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCountedResultHandoffServiceImpl.java"
        ));
        String commandSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCommandServiceImpl.java"
        ));

        assertThat(handoffSource)
            .contains("closeAssignmentTestWithCountedResult")
            .doesNotContain(
                "completeAssignmentFromResult",
                "completeEverything",
                "generic completion",
                "saveAssignmentTest("
            );
        assertThat(commandSource)
            .contains("closeAssignmentTestWithCountedResult")
            .doesNotContain(
                "completeAssignmentFromResult",
                "ResultRecordingService",
                "AssignedAttemptSubmitSequencingService",
                "SelfAttemptSubmitSequencingService"
            );
    }

    private Assignment activeAssignment(Long assignmentId) {
        return new Assignment(
            assignmentId,
            11L,
            21L,
            31L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.plusSeconds(3600),
            null,
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(60)
        );
    }

    private AssignmentTest openAssignmentTest(Long assignmentTestId, Long assignmentId, Long countedResultId) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            countedResultId,
            null,
            false,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(300)
        );
    }

    private AssignmentTest closedAssignmentTest(Long assignmentTestId, Long assignmentId, Long countedResultId) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            countedResultId,
            FIXED_INSTANT.minusSeconds(30),
            true,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(30)
        );
    }

    private CountedAssignmentResultHandoffFacts assignedResult(Long resultId, Long assignmentId, Long assignmentTestId) {
        return new CountedAssignmentResultHandoffFacts(
            resultId,
            assignmentId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            true,
            true,
            true,
            true,
            FIXED_INSTANT
        );
    }

    private CountedAssignmentResultHandoffFacts selfResult(Long resultId) {
        return new CountedAssignmentResultHandoffFacts(
            resultId,
            null,
            null,
            AttemptMode.SELF,
            true,
            false,
            false,
            false,
            FIXED_INSTANT
        );
    }
}
