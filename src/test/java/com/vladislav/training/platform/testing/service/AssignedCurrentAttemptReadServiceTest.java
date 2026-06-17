package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.admission.AssignedCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignedCurrentAttemptRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedCurrentAttemptReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T11:00:00Z");

    @Mock
    private AssignedCurrentAttemptReadFoundationStateReadService assignedCurrentAttemptReadFoundationStateReadService;
    @Mock
    private ActiveAttemptOwnerLocalReadService activeAttemptOwnerLocalReadService;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    private AssignedCurrentAttemptReadService service;

    @BeforeEach
    void setUp() {
        service = new AssignedCurrentAttemptReadService(
            assignedCurrentAttemptReadFoundationStateReadService,
            activeAttemptOwnerLocalReadService,
            accessSpecificationPolicy,
            contextResolver
        );
    }

    @Test
    void allowedPolicyResolvesFoundationThenOwnerLocalActiveAttempt() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        TestAttempt activeAttempt = activeAssignedAttempt(9001L, 101L, 701L, 801L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignedCurrentAttemptReadFoundationStateReadService.findAssignedCurrentAttemptReadFoundationState(101L, 701L, 801L))
            .thenReturn(
                new AssignedCurrentAttemptReadFoundationStateReadService.AssignedCurrentAttemptReadFoundationState(701L, 801L)
            );
        when(activeAttemptOwnerLocalReadService.findActiveAssignedAttemptForActor(101L, 801L)).thenReturn(activeAttempt);

        TestAttempt returned = service.findCurrentAssignedAttemptForActor(101L, 701L, 801L);

        assertThat(returned).isEqualTo(activeAttempt);
        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignedCurrentAttemptReadFoundationStateReadService)
            .findAssignedCurrentAttemptReadFoundationState(101L, 701L, 801L);
        verify(activeAttemptOwnerLocalReadService).findActiveAssignedAttemptForActor(101L, 801L);
        var ordered = inOrder(contextResolver, accessSpecificationPolicy,
            assignedCurrentAttemptReadFoundationStateReadService, activeAttemptOwnerLocalReadService);
        ordered.verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        ordered.verify(accessSpecificationPolicy).canRead(context);
        ordered.verify(assignedCurrentAttemptReadFoundationStateReadService)
            .findAssignedCurrentAttemptReadFoundationState(101L, 701L, 801L);
        ordered.verify(activeAttemptOwnerLocalReadService).findActiveAssignedAttemptForActor(101L, 801L);
    }

    @Test
    void deniedPolicyStopsBeforeFoundationAndOwnerLocalLookup() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findCurrentAssignedAttemptForActor(101L, 701L, 801L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned current attempt");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(assignedCurrentAttemptReadFoundationStateReadService, activeAttemptOwnerLocalReadService);
    }

    @Test
    void actorMismatchStopsBeforePolicyFoundationAndOwnerLocalLookup() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(999L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);

        assertThatThrownBy(() -> service.findCurrentAssignedAttemptForActor(101L, 701L, 801L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned current attempt");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy, never()).canRead(context);
        verifyNoInteractions(assignedCurrentAttemptReadFoundationStateReadService, activeAttemptOwnerLocalReadService);
    }

    @Test
    void missingFoundationStopsBeforeOwnerLocalLookup() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignedCurrentAttemptReadFoundationStateReadService.findAssignedCurrentAttemptReadFoundationState(101L, 701L, 801L))
            .thenReturn(null);

        assertThatThrownBy(() -> service.findCurrentAssignedAttemptForActor(101L, 701L, 801L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Assignment test not found in self-scoped assignment context");

        verify(accessSpecificationPolicy).canRead(context);
        verify(assignedCurrentAttemptReadFoundationStateReadService)
            .findAssignedCurrentAttemptReadFoundationState(101L, 701L, 801L);
        verify(activeAttemptOwnerLocalReadService, never()).findActiveAssignedAttemptForActor(101L, 801L);
    }

    @Test
    void missingActiveAttemptFailsAfterAllowedPolicyAndExistingFoundation() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignedCurrentAttemptReadFoundationStateReadService.findAssignedCurrentAttemptReadFoundationState(101L, 701L, 801L))
            .thenReturn(
                new AssignedCurrentAttemptReadFoundationStateReadService.AssignedCurrentAttemptReadFoundationState(701L, 801L)
            );
        when(activeAttemptOwnerLocalReadService.findActiveAssignedAttemptForActor(101L, 801L)).thenReturn(null);

        assertThatThrownBy(() -> service.findCurrentAssignedAttemptForActor(101L, 701L, 801L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Active assigned attempt not found");

        verify(accessSpecificationPolicy).canRead(context);
        verify(assignedCurrentAttemptReadFoundationStateReadService)
            .findAssignedCurrentAttemptReadFoundationState(101L, 701L, 801L);
        verify(activeAttemptOwnerLocalReadService).findActiveAssignedAttemptForActor(101L, 801L);
    }

    private AccessPolicyQueryContext assignedCurrentAttemptContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assigned_current_attempt",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private TestAttempt activeAssignedAttempt(Long attemptId, Long actorUserId, Long assignmentId, Long assignmentTestId) {
        return new TestAttempt(
            attemptId,
            actorUserId,
            301L,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.IN_PROGRESS,
            FIXED_INSTANT.minusSeconds(300),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT.minusSeconds(30)
        );
    }
}
