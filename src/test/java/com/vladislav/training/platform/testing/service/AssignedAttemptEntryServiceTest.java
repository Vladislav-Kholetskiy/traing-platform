package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignedAttemptEntry}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptEntryServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T10:45:00Z");

    @Mock
    private AssignmentAssignedExecutionAdmissionFoundationStateReadService foundationStateReadService;
    @Mock
    private AssignedAttemptAdmissionSupport assignedAttemptAdmissionSupport;
    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private UtcClock utcClock;

    private AssignedAttemptEntryService service;

    @BeforeEach
    void setUp() {
        service = new AssignedAttemptEntryService(
            foundationStateReadService,
            assignedAttemptAdmissionSupport,
            testAttemptRepository,
            criticalCommandAuditSupport,
            utcClock
        );
    }

    @Test
    void createsNewActiveAssignedAttemptWhenNoCurrentAttemptExistsAndAdmissionAllowsStart() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(null);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> {
            TestAttempt saved = invocation.getArgument(0, TestAttempt.class);
            return new TestAttempt(
                9001L,
                saved.userId(),
                saved.testId(),
                saved.assignmentTestId(),
                saved.attemptMode(),
                saved.status(),
                saved.startedAt(),
                saved.completedAt(),
                saved.expiredAt(),
                saved.abandonedAt(),
                saved.lastActivityAt(),
                saved.createdAt(),
                saved.updatedAt()
            );
        });
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_START),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_START\"}"));

        TestAttempt created = service.enterAssignedAttempt(77L, 701L);

        assertThat(created.id()).isEqualTo(9001L);
        assertThat(created.userId()).isEqualTo(101L);
        assertThat(created.testId()).isEqualTo(501L);
        assertThat(created.assignmentTestId()).isEqualTo(701L);
        assertThat(created.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(created.status()).isEqualTo(TestAttemptStatus.STARTED);
        assertThat(created.startedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(created.lastActivityAt()).isEqualTo(FIXED_INSTANT);
        assertThat(created.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(created.updatedAt()).isEqualTo(FIXED_INSTANT);

        InOrder inOrder = inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            assignedAttemptAdmissionSupport,
            utcClock
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService)
            .findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L);
        inOrder.verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        inOrder.verify(assignedAttemptAdmissionSupport).checkAssignedAttemptStart(77L, 701L);
        inOrder.verify(utcClock).now();
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(utcClock, times(1)).now();
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_START),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(101L),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9001L),
            org.mockito.ArgumentMatchers.isNull(),
            any(),
            any(AuditContext.class)
        );
        verifyNoMoreInteractions(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            assignedAttemptAdmissionSupport,
            utcClock
        );
    }

    @Test
    void assignedEntryAuditPayloadUsesCreatedAttemptFactsWithoutSyntheticFallback() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(null);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> {
            TestAttempt saved = invocation.getArgument(0, TestAttempt.class);
            return new TestAttempt(
                9001L,
                saved.userId(),
                saved.testId(),
                saved.assignmentTestId(),
                saved.attemptMode(),
                saved.status(),
                saved.startedAt(),
                saved.completedAt(),
                saved.expiredAt(),
                saved.abandonedAt(),
                saved.lastActivityAt(),
                saved.createdAt(),
                saved.updatedAt()
            );
        });
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_START),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_START\"}"));

        service.enterAssignedAttempt(77L, 701L);

        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Map> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_START),
            detailsCaptor.capture()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(101L),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9001L),
            org.mockito.ArgumentMatchers.isNull(),
            payloadAfterCaptor.capture(),
            any(AuditContext.class)
        );

        assertThat(detailsCaptor.getValue())
            .containsEntry("commandType", "assigned_attempt_start")
            .containsEntry("entryMode", "create")
            .containsEntry("assignmentId", 77L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("testId", 501L);
        assertThat(detailsCaptor.getValue()).doesNotContainKeys("actorSource", "completedAt", "expiredAt");

        assertThat(payloadAfterCaptor.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadAfter = (Map<String, Object>) payloadAfterCaptor.getValue();
        assertThat(payloadAfter).containsEntry("assignmentId", 77L);
        assertThat(payloadAfter.get("attempt")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> attemptPayload = (Map<String, Object>) payloadAfter.get("attempt");
        assertThat(attemptPayload)
            .containsEntry("id", 9001L)
            .containsEntry("userId", 101L)
            .containsEntry("testId", 501L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("attemptMode", AttemptMode.ASSIGNED)
            .containsEntry("status", TestAttemptStatus.STARTED)
            .containsEntry("startedAt", FIXED_INSTANT)
            .containsEntry("lastActivityAt", FIXED_INSTANT)
            .containsEntry("createdAt", FIXED_INSTANT)
            .containsEntry("updatedAt", FIXED_INSTANT);
        assertThat(attemptPayload).doesNotContainKeys("assignmentId", "completedAt", "expiredAt", "abandonedAt");
    }

    @Test
    void assignedContinuationDoesNotCreateSecondActiveAttempt() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        TestAttempt existingAttempt = assignedAttempt(8001L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(existingAttempt);

        TestAttempt returned = service.enterAssignedAttempt(77L, 701L);

        assertThat(returned).isSameAs(existingAttempt);
        InOrder inOrder = inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            assignedAttemptAdmissionSupport,
            utcClock
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService)
            .findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L);
        inOrder.verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        inOrder.verify(assignedAttemptAdmissionSupport).checkAssignedAttemptContinue(77L, 701L);
        inOrder.verify(utcClock).now();
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(utcClock, times(1)).now();
    }

    @Test
    void assignedContinuationRequiresCapabilityAdmissionBeforeReturn() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        TestAttempt existingAttempt = assignedAttempt(8001L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(existingAttempt);

        TestAttempt returned = service.enterAssignedAttempt(77L, 701L);

        assertThat(returned).isSameAs(existingAttempt);
        verify(assignedAttemptAdmissionSupport).checkAssignedAttemptContinue(77L, 701L);
        InOrder inOrder = inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            assignedAttemptAdmissionSupport,
            utcClock
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService)
            .findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L);
        inOrder.verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        inOrder.verify(assignedAttemptAdmissionSupport).checkAssignedAttemptContinue(77L, 701L);
        inOrder.verify(utcClock).now();
    }

    @Test
    void assignedContinuationUsesActorScopedActiveAttemptLookup() throws IOException {
        String serviceSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptEntryService.java"
        ));

        assertThat(com.vladislav.training.platform.application.policy.CapabilityOperationCode.valueOf(
            "TESTING_ASSIGNED_ATTEMPT_CONTINUE"
        )).isNotNull();

        int activeLookupIndex = serviceSource.indexOf("findAndLockActiveAssignedAttemptForActor(");
        int continuationReturnIndex = serviceSource.indexOf("return consistentAttempt;", activeLookupIndex);
        assertThat(activeLookupIndex).isGreaterThanOrEqualTo(0);
        assertThat(continuationReturnIndex).isGreaterThan(activeLookupIndex);

        String continuationBranch = serviceSource.substring(activeLookupIndex, continuationReturnIndex);
        assertThat(continuationBranch)
            .contains("findAndLockActiveAssignedAttemptForActor(")
            .contains("actorUserId")
            .contains("assignmentTestId")
            .contains("checkAssignedAttemptContinue(")
            .contains("requireConsistentAssignedAttempt(")
            .doesNotContain("checkAssignedAttemptStart(");
    }

    @Test
    void existingActiveAssignedAttemptMustFailClosedWhenAssignmentIsAlreadyClosed() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, true, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOfAny(PolicyViolationException.class, ConflictException.class, NotFoundException.class);

        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock, testAttemptRepository);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void existingActiveAssignedAttemptMustFailClosedWhenAssignmentTestIsAlreadyClosed() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, true);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOfAny(PolicyViolationException.class, ConflictException.class, NotFoundException.class);

        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock, testAttemptRepository);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void existingActiveAssignedAttemptMustFailClosedWhenAssignmentIsCancelled() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), true, false, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOfAny(PolicyViolationException.class, ConflictException.class, NotFoundException.class);

        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock, testAttemptRepository);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignedExecutionEntryFoundationOrServiceMustExposeExplicitDeadlineGateBeforeReturningExistingActiveAttempt()
        throws IOException {
        String serviceSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptEntryService.java"
        ));
        String foundationSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentAssignedExecutionAdmissionFoundationStateReadService.java"
        ));

        boolean foundationCarriesDeadlineFacts = foundationSource.contains("deadline")
            || foundationSource.contains("effectiveAt")
            || foundationSource.contains("dueAt");
        boolean serviceHasExplicitDeadlineGate = serviceSource.contains("deadline")
            || serviceSource.contains("effectiveAt")
            || serviceSource.contains("dueAt")
            || serviceSource.contains("expired");

        assertThat(foundationCarriesDeadlineFacts || serviceHasExplicitDeadlineGate)
            
            .isTrue();
    }

    @Test
    void failsClosedWhenAssignedFoundationIsUnavailable() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(null);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("assignmentId=77")
            .hasMessageContaining("assignmentTestId=701");

        verify(testAttemptRepository, never()).findAndLockActiveAssignedAttemptForActor(any(), any());
        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenLockedActiveAttemptDoesNotMatchAssignedAnchor() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        TestAttempt foreignAttempt = assignedAttempt(8002L, 999L, 501L, 701L, TestAttemptStatus.STARTED);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(foreignAttempt);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("assignmentTestId=701");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void foreignActiveAssignedAttemptCannotBeContinued() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        TestAttempt inconsistentAttempt = assignedAttempt(8005L, 101L, 501L, 702L, TestAttemptStatus.STARTED);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(inconsistentAttempt);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("assignmentTestId=701");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenLockedActiveAttemptHasWrongTestIdForAssignedAnchor() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        TestAttempt inconsistentAttempt = assignedAttempt(8006L, 101L, 999L, 701L, TestAttemptStatus.STARTED);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(inconsistentAttempt);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("assignmentTestId=701");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenLockedActiveAttemptHasWrongModeForAssignedAnchor() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        TestAttempt inconsistentAttempt = new TestAttempt(
            8007L,
            101L,
            501L,
            701L,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            FIXED_INSTANT,
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(inconsistentAttempt);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("assignmentTestId=701");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(assignedAttemptAdmissionSupport, utcClock);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignedContinuationDoesNotWriteCreateAudit() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        TestAttempt existingAttempt = assignedAttempt(8001L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(existingAttempt);
        org.mockito.Mockito.doThrow(new PolicyViolationException("CONTINUE_DENY"))
            .when(assignedAttemptAdmissionSupport).checkAssignedAttemptContinue(77L, 701L);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("CONTINUE_DENY");

        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(utcClock, never()).now();
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void doesNotWriteSuccessAuditWhenAssignedAdmissionFailsBeforeCreate() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(null);
        org.mockito.Mockito.doThrow(new com.vladislav.training.platform.common.exception.PolicyViolationException("DENY"))
            .when(assignedAttemptAdmissionSupport).checkAssignedAttemptStart(77L, 701L);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("DENY");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(utcClock, never()).now();
        verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void doesNotWriteSuccessAuditWhenAssignedAttemptCreateOwnerWriteFails() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(3600), false, false, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(null);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class)))
            .thenThrow(new com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException(
                "Failed to persist test_attempt"
            ));

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException.class)
            .hasMessageContaining("Failed to persist test_attempt");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        verify(assignedAttemptAdmissionSupport).checkAssignedAttemptStart(77L, 701L);
        verify(utcClock).now();
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport, never()).buildAuditContext(
            any(),
            any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class),
            any()
        );
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void existingActiveAssignedAttemptIsReturnedBeforeDeadline() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(60), false, false, false);
        TestAttempt existingAttempt = assignedAttempt(8001L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(existingAttempt);

        TestAttempt returned = service.enterAssignedAttempt(77L, 701L);

        assertThat(returned).isSameAs(existingAttempt);
        InOrder inOrder = inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            assignedAttemptAdmissionSupport,
            utcClock
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService)
            .findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L);
        inOrder.verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        inOrder.verify(assignedAttemptAdmissionSupport).checkAssignedAttemptContinue(77L, 701L);
        inOrder.verify(utcClock).now();
    }

    @Test
    void existingActiveAssignedAttemptIsReturnedExactlyAtDeadline() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT, false, false, false);
        TestAttempt existingAttempt = assignedAttempt(8001L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(existingAttempt);

        TestAttempt returned = service.enterAssignedAttempt(77L, 701L);

        assertThat(returned).isSameAs(existingAttempt);
        verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
    }

    @Test
    void existingActiveAssignedAttemptIsRejectedAfterDeadlineAfterContinuationAdmissionBeforeReturn() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.minusSeconds(1), false, false, false);
        TestAttempt existingAttempt = assignedAttempt(8001L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(existingAttempt);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("after deadline")
            .hasMessageContaining("assignmentTestId=701");

        verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        verify(assignedAttemptAdmissionSupport).checkAssignedAttemptContinue(77L, 701L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createBranchFailsClosedAfterDeadlineAfterStartAdmissionBeforeAttemptCreation() {
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.minusSeconds(1), false, false, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(null);

        assertThatThrownBy(() -> service.enterAssignedAttempt(77L, 701L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("after deadline");

        verify(testAttemptRepository).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        verify(assignedAttemptAdmissionSupport).checkAssignedAttemptStart(77L, 701L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(utcClock).now();
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createBranchUsesSingleEffectiveTimestampForDeadlineGateAndAttemptMaterialization() {
        Instant beforeDeadline = FIXED_INSTANT;
        Instant afterDeadline = FIXED_INSTANT.plusSeconds(2);
        var foundation = foundationState(77L, 701L, 501L, FIXED_INSTANT.plusSeconds(1), false, false, false);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L))
            .thenReturn(foundation);
        when(utcClock.now()).thenReturn(beforeDeadline, afterDeadline);
        when(testAttemptRepository.findAndLockActiveAssignedAttemptForActor(101L, 701L)).thenReturn(null);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> {
            TestAttempt saved = invocation.getArgument(0, TestAttempt.class);
            return new TestAttempt(
                9002L,
                saved.userId(),
                saved.testId(),
                saved.assignmentTestId(),
                saved.attemptMode(),
                saved.status(),
                saved.startedAt(),
                saved.completedAt(),
                saved.expiredAt(),
                saved.abandonedAt(),
                saved.lastActivityAt(),
                saved.createdAt(),
                saved.updatedAt()
            );
        });
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_START),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_START\"}"));

        TestAttempt created = service.enterAssignedAttempt(77L, 701L);

        assertThat(created.startedAt()).isEqualTo(beforeDeadline);
        assertThat(created.lastActivityAt()).isEqualTo(beforeDeadline);
        assertThat(created.createdAt()).isEqualTo(beforeDeadline);
        assertThat(created.updatedAt()).isEqualTo(beforeDeadline);
        verify(utcClock, times(1)).now();
    }

    private AssignmentAssignedExecutionAdmissionFoundationStateReadService.AssignmentAssignedExecutionAdmissionFoundationState
    foundationState(
        Long assignmentId,
        Long assignmentTestId,
        Long testId,
        Instant deadlineAt,
        boolean assignmentCancelled,
        boolean assignmentClosed,
        boolean assignmentTestClosed
    ) {
        return new AssignmentAssignedExecutionAdmissionFoundationStateReadService
            .AssignmentAssignedExecutionAdmissionFoundationState(
                assignmentId,
                assignmentTestId,
                testId,
                deadlineAt,
                assignmentCancelled,
                assignmentClosed,
                assignmentTestClosed
            );
    }

    private TestAttempt assignedAttempt(
        Long id,
        Long userId,
        Long testId,
        Long assignmentTestId,
        TestAttemptStatus status
    ) {
        return new TestAttempt(
            id,
            userId,
            testId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            status,
            FIXED_INSTANT,
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
