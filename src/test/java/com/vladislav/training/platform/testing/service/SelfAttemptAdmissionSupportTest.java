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
 * Проверяет вспомогательную логику {@code SelfAttemptAdmission}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptAdmissionSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T14:00:00Z");

    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    private SelfAttemptAdmissionSupport support;

    @BeforeEach
    void setUp() {
        support = new SelfAttemptAdmissionSupport(capabilityAdmissionPolicy, capabilityAdmissionRequestFactory);
    }

    @Test
    void supportKeepsAdmissionDependenciesOnly() {
        assertThat(Arrays.stream(SelfAttemptAdmissionSupport.class.getDeclaredFields())
            .map(Field::getType)
            .toList())
            .containsExactlyInAnyOrder(
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class
            );
    }

    @Test
    void checkSelfAttemptStartBuildsRequestAndRunsAdmissionBeforeAnyFutureMutationCouldBegin() {
        CapabilityAdmissionRequest request = admissionRequest(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START, 501L);
        when(capabilityAdmissionRequestFactory.createSelfAttemptStart(501L)).thenReturn(request);

        CapabilityAdmissionRequest returned = support.checkSelfAttemptStart(501L);

        assertThat(returned).isSameAs(request);
        InOrder inOrder = inOrder(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
        inOrder.verify(capabilityAdmissionRequestFactory).createSelfAttemptStart(501L);
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        verifyNoMoreInteractions(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
    }

    @Test
    void checkSelfAttemptContinueBuildsDedicatedRequestAndRunsAdmissionBeforeContinuation() {
        CapabilityAdmissionRequest request = admissionRequest(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_CONTINUE, 501L);
        when(capabilityAdmissionRequestFactory.createSelfAttemptContinue(501L)).thenReturn(request);

        CapabilityAdmissionRequest returned = support.checkSelfAttemptContinue(501L);

        assertThat(returned).isSameAs(request);
        InOrder inOrder = inOrder(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
        inOrder.verify(capabilityAdmissionRequestFactory).createSelfAttemptContinue(501L);
        inOrder.verify(capabilityAdmissionPolicy).check(request);
        verifyNoMoreInteractions(capabilityAdmissionRequestFactory, capabilityAdmissionPolicy);
    }

    private CapabilityAdmissionRequest admissionRequest(String operationCode, Long testId) {
        return new CapabilityAdmissionRequest(
            101L,
            operationCode,
            CapabilityTargetEntityType.TEST,
            testId,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );
    }
}
