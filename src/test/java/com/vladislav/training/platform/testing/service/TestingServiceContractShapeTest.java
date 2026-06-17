package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет форму и состав {@code TestingServiceContract}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class TestingServiceContractShapeTest {

    @Test
    void attemptStatusRecalculationServiceContractStaysOwnerLocalAndAssignmentScopedWithoutGenericLifecycleDrift() {
        assertInterfaceContract(
            AttemptStatusRecalculationService.class,
            Set.of(
                signature("recalculateAttemptStatus", TestAttemptStatus.class, Long.class, Instant.class),
                signature("refreshAttemptStatusCache", TestAttempt.class, Long.class, Instant.class),
                signature("refreshAttemptStatusCache", TestAttempt.class, Long.class, Long.class, Instant.class),
                signature(
                    "refreshAttemptStatusCacheWithVerdict",
                    AttemptStatusRecalculationService.AttemptStatusRefreshResult.class,
                    Long.class,
                    Long.class,
                    Instant.class
                ),
                signature(
                    "refreshAssignedAttemptStatusCacheWithVerdict",
                    AttemptStatusRecalculationService.AttemptStatusRefreshResult.class,
                    Long.class,
                    Long.class,
                    Long.class,
                    Instant.class
                )
            )
        );
    }

    private void assertInterfaceContract(Class<?> type, Set<MethodSignature> expectedSignatures) {
        assertThat(type.isInterface()).isTrue();
        assertThat(type.getDeclaredFields()).isEmpty();
        assertThat(Arrays.stream(type.getDeclaredMethods()).anyMatch(Method::isDefault)).isFalse();
        assertThat(Arrays.stream(type.getDeclaredMethods())
            .map(this::signatureOf)
            .collect(Collectors.toUnmodifiableSet()))
            .isEqualTo(expectedSignatures);
    }

    private MethodSignature signatureOf(Method method) {
        return new MethodSignature(method.getName(), method.getReturnType(), Arrays.asList(method.getParameterTypes()));
    }

    private MethodSignature signature(String name, Class<?> returnType, Class<?>... parameterTypes) {
        return new MethodSignature(name, returnType, Arrays.asList(parameterTypes));
    }

    private record MethodSignature(String name, Class<?> returnType, java.util.List<Class<?>> parameterTypes) {
    }
}
