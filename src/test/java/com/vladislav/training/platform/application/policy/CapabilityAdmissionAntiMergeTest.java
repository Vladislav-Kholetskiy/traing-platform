package com.vladislav.training.platform.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code CapabilityAdmissionAntiMerge}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class CapabilityAdmissionAntiMergeTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T15:00:00Z");

    @Mock
    private InteractiveActorResolver interactiveActorResolver;

    private CapabilityAdmissionRequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        UtcClock utcClock = () -> FIXED_INSTANT;
        requestFactory = new CapabilityAdmissionRequestFactory(interactiveActorResolver, utcClock);
    }

    @Test
    void assignedAndSelfExecutionOperationsRemainDistinctRatherThanGenericAliases() {
        assertThat(CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START)
            .isNotEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START);
        assertThat(CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_SUBMIT)
            .isNotEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_SUBMIT);
        assertThat(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_SUBMIT)
            .isNotEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_ABANDON);
    }

    @Test
    void requestFactoryKeepsStage4AdmissionFocusedOnDistinctAssignedAndSelfStartShapes() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);

        CapabilityAdmissionRequest assignedRequest = requestFactory.createAssignedAttemptStart(77L, 701L);
        CapabilityAdmissionRequest selfRequest = requestFactory.createSelfAttemptStart(501L);
        CapabilityAdmissionRequest assignedSubmitRequest = requestFactory.createAssignedAttemptSubmit(77L, 701L);
        CapabilityAdmissionRequest selfSubmitRequest = requestFactory.createSelfAttemptSubmit(501L);
        CapabilityAdmissionRequest selfAbandonRequest = requestFactory.createSelfAttemptAbandon(501L);

        assertThat(assignedRequest.operationCode()).isEqualTo(CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START);
        assertThat(selfRequest.operationCode()).isEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START);
        assertThat(assignedSubmitRequest.operationCode()).isEqualTo(CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_SUBMIT);
        assertThat(selfSubmitRequest.operationCode()).isEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_SUBMIT);
        assertThat(selfAbandonRequest.operationCode()).isEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_ABANDON);
        assertThat(assignedRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.ASSIGNMENT_TEST);
        assertThat(selfRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.TEST);
        assertThat(assignedSubmitRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.ASSIGNMENT_TEST);
        assertThat(selfSubmitRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.TEST);
        assertThat(selfAbandonRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.TEST);
        assertThat(assignedRequest.payloadContext()).isInstanceOf(CapabilityAdmissionPayload.AssignedExecution.class);
        assertThat(selfRequest.payloadContext()).isSameAs(CapabilityAdmissionPayload.SelfExecution.INSTANCE);
        assertThat(assignedSubmitRequest.payloadContext()).isInstanceOf(CapabilityAdmissionPayload.AssignedExecution.class);
        assertThat(selfSubmitRequest.payloadContext()).isSameAs(CapabilityAdmissionPayload.SelfExecution.INSTANCE);
        assertThat(selfAbandonRequest.payloadContext()).isSameAs(CapabilityAdmissionPayload.SelfExecution.INSTANCE);
    }

    @Test
    void requestFactoryKeepsAssignedAndSelfExecutionShapesDistinct() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);

        CapabilityAdmissionRequest assignedRequest = requestFactory.createAssignedAttemptStart(77L, 701L);
        CapabilityAdmissionRequest selfRequest = requestFactory.createSelfAttemptStart(501L);

        assertThat(assignedRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.ASSIGNMENT_TEST);
        assertThat(selfRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.TEST);
        assertThat(assignedRequest.payloadContext()).isInstanceOf(CapabilityAdmissionPayload.AssignedExecution.class);
        assertThat(selfRequest.payloadContext()).isSameAs(CapabilityAdmissionPayload.SelfExecution.INSTANCE);
        assertThat(assignedRequest.operationCode()).isNotEqualTo(selfRequest.operationCode());
    }

    @Test
    void foundationFactsKeepSeparateResolutionMethodsForAssignedAndSelfExecution() {
        assertThat(Arrays.stream(CapabilityAdmissionFoundationFacts.class.getDeclaredMethods())
            .map(Method::getName)
            .toList())
            .contains("resolveAssignmentAssignedExecutionFoundationState", "resolveSelfExecutionFoundationState")
            .doesNotContain("resolveExecutionFoundationState", "execution", "resolveTestingExecutionFoundationState");
    }
}
