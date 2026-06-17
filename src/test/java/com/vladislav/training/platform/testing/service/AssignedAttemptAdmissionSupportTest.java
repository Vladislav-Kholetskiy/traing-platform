package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code AssignedAttemptAdmission}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptAdmissionSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T11:00:00Z");

    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    private AssignedAttemptAdmissionSupport support;

    @BeforeEach
    void setUp() {
        support = new AssignedAttemptAdmissionSupport(capabilityAdmissionPolicy, capabilityAdmissionRequestFactory);
    }

    @Test
    void supportKeepsAdmissionDependenciesOnly() {
        assertThat(Arrays.stream(AssignedAttemptAdmissionSupport.class.getDeclaredFields())
            .map(Field::getType)
            .toList())
            .containsExactlyInAnyOrder(
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class
            );
    }

    @Test
    void checkAssignedAttemptStartBuildsRequestAndRunsAdmissionBeforeAnyFutureMutationCouldBegin() {
        CapabilityAdmissionRequest request = admissionRequest(CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START, 77L, 701L);
        when(capabilityAdmissionRequestFactory.createAssignedAttemptStart(77L, 701L)).thenReturn(request);

        CapabilityAdmissionRequest returned = support.checkAssignedAttemptStart(77L, 701L);

        assertThat(returned).isSameAs(request);
        InOrder inOrder = inOrder(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
        inOrder.verify(capabilityAdmissionRequestFactory).createAssignedAttemptStart(77L, 701L);
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        verifyNoMoreInteractions(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
    }

    @Test
    void checkAssignedAttemptContinueBuildsDedicatedRequestAndRunsAdmissionBeforeAnyFutureContinuationCanResume() {
        CapabilityAdmissionRequest request = admissionRequest(
            com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_CONTINUE.code(),
            77L,
            701L
        );
        when(capabilityAdmissionRequestFactory.createAssignedAttemptContinue(77L, 701L)).thenReturn(request);

        CapabilityAdmissionRequest returned = support.checkAssignedAttemptContinue(77L, 701L);

        assertThat(returned).isSameAs(request);
        InOrder inOrder = inOrder(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
        inOrder.verify(capabilityAdmissionRequestFactory).createAssignedAttemptContinue(77L, 701L);
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        verifyNoMoreInteractions(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
    }

    private CapabilityAdmissionRequest admissionRequest(String operationCode, Long assignmentId, Long assignmentTestId) {
        return new CapabilityAdmissionRequest(
            101L,
            operationCode,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            assignmentTestId,
            new CapabilityAdmissionPayload.AssignedExecution(assignmentId),
            FIXED_INSTANT
        );
    }
}
