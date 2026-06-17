package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AssignedAttemptExpiryTerminalService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignedAttemptExpiryTerminalServiceBoundaryTest {

    @Test
    void assignedExpiryTerminalServiceDependsOnlyOnAssignedExpiryOwnerPathCollaborators() {
        assertThat(fieldTypes(AssignedAttemptExpiryTerminalService.class))
            .containsExactlyInAnyOrder(
                CriticalCommandAuditSupport.class,
                SystemActorResolver.class,
                TestAttemptRepository.class,
                UtcClock.class,
                AttemptTerminalCriticalAuditPayloadFactory.class
            )
            .doesNotContain(
                AssignedAttemptSubmitTerminalService.class,
                SelfAttemptAdmissionSupport.class
            );
    }

    @Test
    void assignedExpiryTerminalServiceKeepsAssignedExpiryVocabularyWithoutGenericOrSubmitMerge() {
        assertThat(Arrays.stream(AssignedAttemptExpiryTerminalService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactly("expireAssignedAttempt")
            .doesNotContain(
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "closeAttempt",
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
