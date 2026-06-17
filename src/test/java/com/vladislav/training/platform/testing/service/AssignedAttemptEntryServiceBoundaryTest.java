package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AssignedAttemptEntryService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignedAttemptEntryServiceBoundaryTest {

    @Test
    void assignedEntryServiceDependsOnlyOnAssignedOwnerPathCollaborators() {
        assertThat(fieldTypes(AssignedAttemptEntryService.class))
            .containsExactlyInAnyOrder(
                AssignmentAssignedExecutionAdmissionFoundationStateReadService.class,
                AssignedAttemptAdmissionSupport.class,
                TestAttemptRepository.class,
                CriticalCommandAuditSupport.class,
                UtcClock.class,
                AssignedAttemptEntryCriticalAuditPayloadFactory.class
            )
            .doesNotContain(
                SelfAttemptAdmissionSupport.class,
                ResultRecordingService.class
            );
    }

    @Test
    void assignedEntryServiceKeepsDedicatedAssignedEntryVocabularyWithoutGenericOrSelfMerge() {
        assertThat(Arrays.stream(AssignedAttemptEntryService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("enterAssignedAttempt")
            .doesNotContain(
                "startAttempt",
                "continueAttempt",
                "startSelfAttempt",
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
