package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code SelfAttemptSubmitTerminalService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class SelfAttemptSubmitTerminalServiceBoundaryTest {

    @Test
    void selfSubmitTerminalServiceDependsOnlyOnSelfSubmitOwnerPathCollaborators() {
        assertThat(fieldTypes(SelfAttemptSubmitTerminalService.class))
            .containsExactlyInAnyOrder(
                CriticalCommandAuditSupport.class,
                TestAttemptRepository.class,
                UtcClock.class,
                AttemptTerminalCriticalAuditPayloadFactory.class
            )
            .doesNotContain(
                AssignmentAssignedExecutionAdmissionFoundationStateReadService.class,
                AssignedAttemptSubmitTerminalService.class,
                AssignedAttemptExpiryTerminalService.class,
                ResultRecordingService.class
            );
    }

    @Test
    void selfSubmitTerminalServiceKeepsSelfSubmitVocabularyWithoutAssignedOrAbandonMerge() {
        assertThat(Arrays.stream(SelfAttemptSubmitTerminalService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("submitSelfAttempt")
            .doesNotContain(
                "submitAssignedAttempt",
                "expireAssignedAttempt",
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
