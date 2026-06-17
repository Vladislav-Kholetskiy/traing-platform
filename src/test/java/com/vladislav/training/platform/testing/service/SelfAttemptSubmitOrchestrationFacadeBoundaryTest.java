package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code SelfAttemptSubmitSequencingService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */

class SelfAttemptSubmitSequencingServiceBoundaryTest {

    @Test
    void selfSubmitOrchestrationFacadeDependsOnlyOnSelfSubmitOwnerAndResultRecordingOwner() {
        assertThat(fieldTypes(SelfAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptSubmitTerminalService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                SelfAttemptTerminalAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                TestAttemptRepository.class,
                ResultRepository.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentStatusRecalculationService.class,
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptAbandonTerminalService.class
            );
    }

    @Test
    void selfSubmitOrchestrationFacadeKeepsSelfSubmitOnlyVocabularyWithoutGenericCloseManagerDrift() {
        assertThat(Arrays.stream(SelfAttemptSubmitSequencingService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("submitSelfAttempt")
            .doesNotContain(
                "submitAssignedAttempt",
                "abandonSelfAttempt",
                "closeAttempt",
                "submitAttempt",
                "recordResult"
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
