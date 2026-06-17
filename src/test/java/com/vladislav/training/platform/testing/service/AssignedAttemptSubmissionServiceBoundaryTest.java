package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AssignedAttemptSubmissionService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignedAttemptSubmissionServiceBoundaryTest {

    @Test
    void assignedSubmitEntryDependsOnlyOnAssignedSubmitOrchestrationFacade() {
        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .containsExactlyInAnyOrder(
                AssignedAttemptSubmitSequencingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                AssignedAttemptSubmitAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                AssignedAttemptSubmitTerminalService.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentStatusRecalculationService.class,
                TestAttemptRepository.class,
                ResultRepository.class,
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonTerminalService.class
            );
    }

    @Test
    void assignedSubmitEntryKeepsAssignedOnlyVocabularyWithoutGenericSubmissionManagerDrift() {
        assertThat(Arrays.stream(AssignedAttemptSubmissionService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("submitAssignedAttempt")
            .doesNotContain(
                "submitAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt",
                "closeAttempt",
                "recordResult"
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
