package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AssignedAttemptSubmitTerminalService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignedAttemptSubmitTerminalServiceBoundaryTest {

    @Test
    void assignedSubmitTerminalServiceDependsOnlyOnAssignedTerminalOwnerPathCollaborators() {
        assertThat(fieldTypes(AssignedAttemptSubmitTerminalService.class))
            .containsExactlyInAnyOrder(
                AttemptStatusRecalculationService.class,
                CriticalCommandAuditSupport.class,
                com.vladislav.training.platform.testing.repository.TestAttemptRepository.class,
                UtcClock.class,
                AttemptTerminalCriticalAuditPayloadFactory.class
            )
            .doesNotContain(
                SelfAttemptAdmissionSupport.class,
                AssignedAttemptAdmissionSupport.class,
                ResultRecordingService.class,
                com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService.class,
                ActiveAttemptAnswerMutationService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class
            );
    }

    @Test
    void assignedSubmitTerminalServiceKeepsAssignedSubmitVocabularyWithoutGenericOrExpiryMerge() {
        assertThat(Arrays.stream(AssignedAttemptSubmitTerminalService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("submitAssignedAttempt")
            .doesNotContain(
                "submitSelfAttempt",
                "closeAttempt",
                "expireAssignedAttempt",
                "abandonSelfAttempt",
                "recordResult"
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
