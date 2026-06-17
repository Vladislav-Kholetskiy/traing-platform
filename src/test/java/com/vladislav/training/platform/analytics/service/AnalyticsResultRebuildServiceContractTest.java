package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AnalyticsResultRebuildService}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AnalyticsResultRebuildServiceContractTest {

    private static final String CONTRACT_MESSAGE =
        "SCN-11 requires explicit result-based analytics rebuild runtime implementation; "
            + "interface/table/repository existence is not enough; "
            + "implementation must be introduced before any aggregate materialization logic is accepted.";

    private static final String IMPLEMENTATION_CLASS_NAME =
        "com.vladislav.training.platform.analytics.service.AnalyticsResultRebuildServiceImpl";

    @Test
    void resultBasedAnalyticsRebuildRuntimeRequiresConcreteProductionImplementation() throws Exception {
        Class<?> implementationClass = loadImplementationClassOrFail();

        assertThat(AnalyticsRebuildService.class.isAssignableFrom(implementationClass))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(implementationClass.isInterface())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(Modifier.isAbstract(implementationClass.getModifiers()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.isEnum())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.isRecord())
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(implementationClass.getSimpleName())
            .withFailMessage(CONTRACT_MESSAGE)
            .doesNotContain("Test")
            .doesNotContain("Fixture");
        assertThat(Files.exists(productionSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isTrue();
        assertThat(Files.exists(testSourcePath()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isFalse();
        assertThat(Arrays.stream(implementationClass.getDeclaredConstructors()))
            .withFailMessage(CONTRACT_MESSAGE)
            .isNotEmpty();
        assertThat(publicBoundedRebuildMethods(implementationClass))
            .withFailMessage(CONTRACT_MESSAGE)
            .isNotEmpty();
    }

    private static Class<?> loadImplementationClassOrFail() {
        try {
            return Class.forName(IMPLEMENTATION_CLASS_NAME);
        } catch (ClassNotFoundException exception) {
            fail(CONTRACT_MESSAGE, exception);
            throw new IllegalStateException("Unreachable");
        }
    }

    private static Path productionSourcePath() {
        return Path.of(
            "src",
            "main",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "analytics",
            "service",
            "AnalyticsResultRebuildServiceImpl.java"
        );
    }

    private static Path testSourcePath() {
        return Path.of(
            "src",
            "test",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "analytics",
            "service",
            "AnalyticsResultRebuildServiceImpl.java"
        );
    }

    private static Stream<Method> publicBoundedRebuildMethods(Class<?> implementationClass) {
        return Arrays.stream(implementationClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> method.getName().toLowerCase().contains("rebuild"))
            .filter(method -> method.getParameterCount() > 0);
    }
}
