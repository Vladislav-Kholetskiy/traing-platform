package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.time.Instant;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AttemptStatusRecalculationService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AttemptStatusRecalculationServiceBoundaryTest {

    @Test
    void attemptStatusRecalculationImplementationDependsOnlyOnAttemptOwnedRecoveryFacts() {
        assertThat(fieldTypes(AttemptStatusRecalculationServiceImpl.class))
            .containsExactlyInAnyOrder(
                TestAttemptRepository.class,
                AssignmentRepository.class,
                AssignmentTestRepository.class
            )
            .doesNotContain(
                ResultRecordingService.class,
                AssignmentStatusRecalculationService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class
            );
    }

    @Test
    void attemptStatusRecalculationKeepsRefreshRecoveryVocabularyWithoutGenericLifecycleDrift() {
        assertThat(Arrays.stream(AttemptStatusRecalculationService.class.getDeclaredMethods())
            .map(Method::getName)
            .distinct()
            .toList())
            .containsExactlyInAnyOrder(
                "recalculateAttemptStatus",
                "refreshAttemptStatusCache",
                "refreshAttemptStatusCacheWithVerdict",
                "refreshAssignedAttemptStatusCacheWithVerdict"
            )
            .doesNotContain(
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "expireAssignedAttempt",
                "abandonSelfAttempt",
                "closeAttempt",
                "recordResult"
            );

        assertThat(Arrays.stream(AttemptStatusRecalculationService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(this::methodSignature)
            .toList())
            .containsExactlyInAnyOrder(
                "recalculateAttemptStatus(Long, Instant): TestAttemptStatus",
                "refreshAttemptStatusCache(Long, Instant): TestAttempt",
                "refreshAttemptStatusCache(Long, Long, Instant): TestAttempt",
                "refreshAttemptStatusCacheWithVerdict(Long, Long, Instant): AttemptStatusRefreshResult",
                "refreshAssignedAttemptStatusCacheWithVerdict(Long, Long, Long, Instant): AttemptStatusRefreshResult"
            )
            .doesNotContain(
                "submitAssignedAttempt(Long): Long",
                "submitSelfAttempt(Long): Long",
                "expireAssignedAttempt(Long, Instant): TestAttempt",
                "abandonSelfAttempt(Long): TestAttempt",
                "closeAttempt(Long): TestAttempt",
                "recordResult(Long): Long"
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private String methodSignature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
        return method.getName() + "(" + parameters + "): " + method.getReturnType().getSimpleName();
    }
}
