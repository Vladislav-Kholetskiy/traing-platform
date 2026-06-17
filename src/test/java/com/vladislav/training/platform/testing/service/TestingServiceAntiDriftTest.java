package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
/**
 * Не даёт {@code TestingService} незаметно уйти от текущего замысла.
 * Тест страхует важные ограничения и исходную идею решения.
 */
class TestingServiceAntiDriftTest {

    private static final String TESTING_SERVICE_PACKAGE = "com.vladislav.training.platform.testing.service";

    @Test
    void testingServicePackageRetainsCanonicalRecalculationInterfaceWithoutFreezingExactTopLevelShape() {
        Set<String> discoveredInterfaceNames = discoverTopLevelInterfaces(TESTING_SERVICE_PACKAGE);

        assertThat(discoveredInterfaceNames).contains("AttemptStatusRecalculationService");
    }

    @Test
    void testingServicePackageDoesNotAllowGenericDriftNames() {
        assertThat(discoverTopLevelInterfaces(TESTING_SERVICE_PACKAGE))
            .doesNotContain(
                "AttemptService",
                "TestingService",
                "ExecutionService",
                "AttemptFacade",
                "TestingFacade",
                "CompletionService",
                "ResultFacade",
                "AttemptSubmissionService",
                "AttemptCommandService",
                "AttemptLifecycleService",
                "AttemptAnswerService",
                "AttemptQueryService"
            );
    }

    private Set<String> discoverTopLevelInterfaces(String packageName) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter(interfaceTypeFilter());

        return scanner.findCandidateComponents(packageName).stream()
            .map(beanDefinition -> beanDefinition.getBeanClassName())
            .filter(className -> !className.endsWith(".package-info"))
            .map(this::loadClass)
            .filter(Class::isInterface)
            .filter(type -> !type.getName().contains("$"))
            .map(Class::getSimpleName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private TypeFilter interfaceTypeFilter() {
        return (metadataReader, metadataReaderFactory) -> metadataReader.getClassMetadata().isInterface();
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to load discovered service type: " + className, exception);
        }
    }
}
