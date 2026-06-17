package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.SelfAttemptEntryFoundationStateReadService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code SelfAttemptEntryService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class SelfAttemptEntryServiceBoundaryTest {

    @Test
    void selfEntryServiceDependsOnlyOnSelfOwnerPathCollaborators() {
        assertThat(fieldTypes(SelfAttemptEntryService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptEntryFoundationStateReadService.class,
                SelfAttemptAdmissionSupport.class,
                TestAttemptRepository.class,
                CriticalCommandAuditSupport.class,
                UtcClock.class,
                SelfAttemptEntryCriticalAuditPayloadFactory.class
            )
            .doesNotContain(
                AssignmentAssignedExecutionAdmissionFoundationStateReadService.class,
                AssignedAttemptAdmissionSupport.class,
                ResultRecordingService.class
            );
    }

    @Test
    void selfEntryServiceKeepsSelfStartContinueVocabularyWithoutGenericOrAssignedMerge() {
        assertThat(Arrays.stream(SelfAttemptEntryService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("startOrContinueSelfAttempt")
            .doesNotContain(
                "startAttempt",
                "continueAttempt",
                "startAssignedAttempt",
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
