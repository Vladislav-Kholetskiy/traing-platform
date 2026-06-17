package com.vladislav.training.platform.testing.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignedCurrentAttemptReadFoundationStateRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedCurrentAttemptReadFoundationStateReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-23T10:00:00Z");

    @Mock
    private AssignmentSelfScopedReadRepository assignmentSelfScopedReadRepository;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    private AssignedCurrentAttemptReadFoundationStateReadService service;

    @BeforeEach
    void setUp() {
        service = new AssignedCurrentAttemptReadFoundationStateReadServiceImpl(
            assignmentSelfScopedReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
    }

    @Test
    void returnsFoundationWhenAssignmentAndAssignmentTestStayInsideActorScopedReadPath() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        ))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(101L, 71L)).thenReturn(assignment(71L, 101L));
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentTestsByAssignmentId(101L, 71L))
            .thenReturn(List.of(assignmentTest(501L, 71L)));

        var foundation = service.findAssignedCurrentAttemptReadFoundationState(101L, 71L, 501L);

        assertThat(foundation).isEqualTo(
            new AssignedCurrentAttemptReadFoundationStateReadService.AssignedCurrentAttemptReadFoundationState(71L, 501L)
        );
        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentById(101L, 71L);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentTestsByAssignmentId(101L, 71L);
        verifyNoMoreInteractions(assignmentSelfScopedReadRepository, accessSpecificationPolicy, contextResolver);
    }

    @Test
    void returnsNullWhenAssignmentTestDoesNotBelongToActorScopedAssignmentAndDoesNotStartAnything() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        ))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(101L, 71L)).thenReturn(assignment(71L, 101L));
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentTestsByAssignmentId(101L, 71L))
            .thenReturn(List.of(assignmentTest(700L, 71L)));

        assertThat(service.findAssignedCurrentAttemptReadFoundationState(101L, 71L, 999L)).isNull();

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentById(101L, 71L);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentTestsByAssignmentId(101L, 71L);
        verifyNoMoreInteractions(assignmentSelfScopedReadRepository, accessSpecificationPolicy, contextResolver);
    }

    @Test
    void deniedReadStopsBeforeRepositoryAccess() {
        AccessPolicyQueryContext context = assignedCurrentAttemptContext(101L);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        ))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findAssignedCurrentAttemptReadFoundationState(101L, 71L, 501L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned current attempt foundation");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(assignmentSelfScopedReadRepository);
    }

    @Test
    void actorMismatchStopsBeforePolicyAndRepositoryAccess() {
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        ))
            .thenReturn(assignedCurrentAttemptContext(999L));

        assertThatThrownBy(() -> service.findAssignedCurrentAttemptReadFoundationState(101L, 71L, 501L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned current attempt foundation");

        verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        verifyNoInteractions(accessSpecificationPolicy, assignmentSelfScopedReadRepository);
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
            com.vladislav.training.platform.access.service.AccessReadSubjectSemantics.SELF
        );
    }

    private Assignment assignment(Long assignmentId, Long actorUserId) {
        return new Assignment(
            assignmentId,
            901L,
            actorUserId,
            301L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.plusSeconds(3600),
            null,
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private AssignmentTest assignmentTest(Long assignmentTestId, Long assignmentId) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            301L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            null,
            false,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }
}
