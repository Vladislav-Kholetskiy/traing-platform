package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code ActiveAttemptOwnerLocalRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class ActiveAttemptOwnerLocalReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T09:00:00Z");

    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    private ActiveAttemptOwnerLocalReadService service;

    @BeforeEach
    void setUp() {
        service = new ActiveAttemptOwnerLocalReadService(
            testAttemptRepository,
            accessSpecificationPolicy,
            contextResolver
        );
    }

    @Test
    void returnsExistingActiveAssignedAttemptThroughActorScopedPolicyAwareOwnerLocalAssignedLookupOnly() {
        TestAttempt activeAttempt = assignedAttempt(8001L, 701L, TestAttemptStatus.IN_PROGRESS);
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(testAttemptRepository.findActiveAssignedAttemptForActor(101L, 701L)).thenReturn(activeAttempt);

        TestAttempt returned = service.findActiveAssignedAttemptForActor(101L, 701L);

        assertThat(returned).isSameAs(activeAttempt);
        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(testAttemptRepository).findActiveAssignedAttemptForActor(101L, 701L);
        verify(testAttemptRepository, never()).findTestAttemptsByAssignmentTestId(701L);
        verify(testAttemptRepository, never()).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        verify(testAttemptRepository, never()).saveTestAttempt(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(testAttemptRepository, accessSpecificationPolicy, contextResolver);
    }

    @Test
    void returnsExistingActiveSelfAttemptThroughOwnerLocalSelfLookupOnly() {
        TestAttempt activeAttempt = selfAttempt(8002L, 101L, 501L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findActiveSelfAttempt(101L, 501L)).thenReturn(activeAttempt);

        TestAttempt returned = service.findActiveSelfAttempt(101L, 501L);

        assertThat(returned).isSameAs(activeAttempt);
        verify(testAttemptRepository).findActiveSelfAttempt(101L, 501L);
        verify(testAttemptRepository, never()).findTestAttemptsByUserIdAndTestId(101L, 501L);
        verify(testAttemptRepository, never()).findAndLockActiveSelfAttempt(101L, 501L);
        verify(testAttemptRepository, never()).saveTestAttempt(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(testAttemptRepository);
        verifyNoInteractions(accessSpecificationPolicy, contextResolver);
    }

    @Test
    void returnsNullWhenNoActiveActorScopedAssignedAttemptExistsAndDoesNotImplicitlyStartAssignedAttempt() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(testAttemptRepository.findActiveAssignedAttemptForActor(101L, 702L)).thenReturn(null);

        assertThat(service.findActiveAssignedAttemptForActor(101L, 702L)).isNull();

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(testAttemptRepository).findActiveAssignedAttemptForActor(101L, 702L);
        verify(testAttemptRepository, never()).findAndLockActiveAssignedAttemptForActor(101L, 702L);
        verify(testAttemptRepository, never()).findTestAttemptsByAssignmentTestId(702L);
        verify(testAttemptRepository, never()).saveTestAttempt(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(testAttemptRepository, accessSpecificationPolicy, contextResolver);
    }

    @Test
    void deniedAssignedCurrentAttemptStopsBeforeRepositoryAccess() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findActiveAssignedAttemptForActor(101L, 702L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned current attempt");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(testAttemptRepository);
    }

    @Test
    void actorMismatchStopsBeforePolicyAndRepositoryAccessForAssignedCurrentAttempt() {
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        )).thenReturn(assignedCurrentAttemptContext(999L));

        assertThatThrownBy(() -> service.findActiveAssignedAttemptForActor(101L, 702L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned current attempt");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verifyNoInteractions(accessSpecificationPolicy, testAttemptRepository);
    }

    @Test
    void returnsNullWhenNoActiveAttemptExistsAndDoesNotImplicitlyStartSelfAttempt() {
        when(testAttemptRepository.findActiveSelfAttempt(102L, 502L)).thenReturn(null);

        assertThat(service.findActiveSelfAttempt(102L, 502L)).isNull();

        verify(testAttemptRepository).findActiveSelfAttempt(102L, 502L);
        verify(testAttemptRepository, never()).findAndLockActiveSelfAttempt(102L, 502L);
        verify(testAttemptRepository, never()).findTestAttemptsByUserIdAndTestId(102L, 502L);
        verify(testAttemptRepository, never()).saveTestAttempt(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(testAttemptRepository);
        verifyNoInteractions(accessSpecificationPolicy, contextResolver);
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
            AccessReadSubjectScope.ACTOR_SELF
        );
    }

    private TestAttempt assignedAttempt(Long id, Long assignmentTestId, TestAttemptStatus status) {
        return new TestAttempt(
            id,
            101L,
            601L,
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

    private TestAttempt selfAttempt(Long id, Long userId, Long testId, TestAttemptStatus status) {
        return new TestAttempt(
            id,
            userId,
            testId,
            null,
            AttemptMode.SELF,
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
