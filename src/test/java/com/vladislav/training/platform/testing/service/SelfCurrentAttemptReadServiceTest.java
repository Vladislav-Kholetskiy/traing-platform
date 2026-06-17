package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.vladislav.training.platform.testing.admission.SelfCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.common.model.AttemptMode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code SelfCurrentAttemptRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfCurrentAttemptReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T09:00:00Z");

    @Mock
    private SelfCurrentAttemptReadFoundationStateReadService selfCurrentAttemptReadFoundationStateReadService;
    @Mock
    private ActiveAttemptOwnerLocalReadService activeAttemptOwnerLocalReadService;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    private SelfCurrentAttemptReadService service;

    @BeforeEach
    void setUp() {
        service = new SelfCurrentAttemptReadService(
            selfCurrentAttemptReadFoundationStateReadService,
            activeAttemptOwnerLocalReadService,
            accessSpecificationPolicy,
            contextResolver
        );
    }

    @Test
    void allowedPolicyResolvesFoundationThenOwnerLocalActiveAttempt() {
        AccessPolicyQueryContext context = selfCurrentAttemptContext(101L);
        TestAttempt activeAttempt = activeSelfAttempt(9001L, 101L, 501L);
        when(contextResolver.resolveSelfCurrentAttemptContext(101L, 501L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(selfCurrentAttemptReadFoundationStateReadService.findSelfCurrentAttemptReadFoundationState(101L, 501L))
            .thenReturn(new SelfCurrentAttemptReadFoundationStateReadService.SelfCurrentAttemptReadFoundationState(501L));
        when(activeAttemptOwnerLocalReadService.findActiveSelfAttempt(101L, 501L)).thenReturn(activeAttempt);

        TestAttempt returned = service.findCurrentSelfAttemptForActor(101L, 501L);

        assertThat(returned).isEqualTo(activeAttempt);
        InOrder ordered = org.mockito.Mockito.inOrder(
            contextResolver,
            accessSpecificationPolicy,
            selfCurrentAttemptReadFoundationStateReadService,
            activeAttemptOwnerLocalReadService
        );
        ordered.verify(contextResolver).resolveSelfCurrentAttemptContext(101L, 501L);
        ordered.verify(accessSpecificationPolicy).canRead(context);
        ordered.verify(selfCurrentAttemptReadFoundationStateReadService)
            .findSelfCurrentAttemptReadFoundationState(101L, 501L);
        ordered.verify(activeAttemptOwnerLocalReadService).findActiveSelfAttempt(101L, 501L);
        verify(accessSpecificationPolicy).canRead(context);
    }

    @Test
    void deniedPolicyStopsBeforeFoundationAndOwnerLocalLookup() {
        AccessPolicyQueryContext context = selfCurrentAttemptContext(101L);
        when(contextResolver.resolveSelfCurrentAttemptContext(101L, 501L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findCurrentSelfAttemptForActor(101L, 501L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("self current attempt");

        verify(contextResolver).resolveSelfCurrentAttemptContext(101L, 501L);
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(selfCurrentAttemptReadFoundationStateReadService, activeAttemptOwnerLocalReadService);
    }

    @Test
    void resolverMismatchStopsBeforePolicyFoundationAndOwnerLocalLookup() {
        when(contextResolver.resolveSelfCurrentAttemptContext(101L, 501L))
            .thenThrow(new IllegalArgumentException("actorUserId must match the current authenticated actor"));

        assertThatThrownBy(() -> service.findCurrentSelfAttemptForActor(101L, 501L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId must match");

        verify(contextResolver).resolveSelfCurrentAttemptContext(101L, 501L);
        verify(accessSpecificationPolicy, never()).canRead(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(selfCurrentAttemptReadFoundationStateReadService, activeAttemptOwnerLocalReadService);
    }

    @Test
    void missingFoundationStopsBeforeOwnerLocalLookup() {
        AccessPolicyQueryContext context = selfCurrentAttemptContext(101L);
        when(contextResolver.resolveSelfCurrentAttemptContext(101L, 501L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(selfCurrentAttemptReadFoundationStateReadService.findSelfCurrentAttemptReadFoundationState(101L, 501L))
            .thenReturn(null);

        assertThatThrownBy(() -> service.findCurrentSelfAttemptForActor(101L, 501L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self current attempt foundation not found");

        verify(accessSpecificationPolicy).canRead(context);
        verify(selfCurrentAttemptReadFoundationStateReadService).findSelfCurrentAttemptReadFoundationState(101L, 501L);
        verify(activeAttemptOwnerLocalReadService, never()).findActiveSelfAttempt(101L, 501L);
    }

    @Test
    void missingActiveAttemptFailsAfterAllowedPolicyAndExistingFoundation() {
        AccessPolicyQueryContext context = selfCurrentAttemptContext(101L);
        when(contextResolver.resolveSelfCurrentAttemptContext(101L, 501L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(selfCurrentAttemptReadFoundationStateReadService.findSelfCurrentAttemptReadFoundationState(101L, 501L))
            .thenReturn(new SelfCurrentAttemptReadFoundationStateReadService.SelfCurrentAttemptReadFoundationState(501L));
        when(activeAttemptOwnerLocalReadService.findActiveSelfAttempt(101L, 501L)).thenReturn(null);

        assertThatThrownBy(() -> service.findCurrentSelfAttemptForActor(101L, 501L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Active self attempt not found");

        verify(accessSpecificationPolicy).canRead(context);
        verify(selfCurrentAttemptReadFoundationStateReadService).findSelfCurrentAttemptReadFoundationState(101L, 501L);
        verify(activeAttemptOwnerLocalReadService).findActiveSelfAttempt(101L, 501L);
    }

    private AccessPolicyQueryContext selfCurrentAttemptContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            501L,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private TestAttempt activeSelfAttempt(Long attemptId, Long actorUserId, Long testId) {
        return new TestAttempt(
            attemptId,
            actorUserId,
            testId,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            FIXED_INSTANT.minusSeconds(300),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT.minusSeconds(10)
        );
    }
}
