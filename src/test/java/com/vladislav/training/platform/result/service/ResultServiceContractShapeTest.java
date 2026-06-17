package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет форму и состав {@code ResultServiceContract}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class ResultServiceContractShapeTest {

    @Test
    void resultRecordingServiceContractIsLockedToExactStage1Shape() {
        assertInterfaceContract(
            ResultRecordingService.class,
            Set.of(signature("recordResult", Long.class, Long.class))
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
