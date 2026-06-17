package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code ActiveAttemptOwnerLocalReadService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class ActiveAttemptOwnerLocalReadServiceBoundaryTest {

    @Test
    void activeAttemptReadServiceDependsOnTestingOwnedRepositoryAndAssignedReadPolicyPathOnly() {
        assertThat(fieldTypes(ActiveAttemptOwnerLocalReadService.class))
            .containsExactlyInAnyOrder(
                TestAttemptRepository.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AttemptStatusRecalculationService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class
            );
    }

    @Test
    void activeAttemptReadServiceKeepsQueryOnlyVocabularyWithoutMutationOrTerminalizationDrift() {
        assertThat(Arrays.stream(ActiveAttemptOwnerLocalReadService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactlyInAnyOrder("findActiveAssignedAttemptForActor", "findActiveSelfAttempt")
            .doesNotContain(
                "startAttempt",
                "startOrContinueAssignedAttempt",
                "startOrContinueSelfAttempt",
                "saveUserAnswer",
                "submitAttempt",
                "expireAssignedAttempt",
                "abandonSelfAttempt",
                "recordResult"
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .toList();
    }
}
