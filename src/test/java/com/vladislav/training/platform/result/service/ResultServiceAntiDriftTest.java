package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
/**
 * Не даёт {@code ResultService} незаметно уйти от текущего замысла.
 * Тест страхует важные ограничения и исходную идею решения.
 */
class ResultServiceAntiDriftTest {

    private static final String RESULT_SERVICE_PACKAGE = "com.vladislav.training.platform.result.service";

    @Test
    void resultServicePackageContainsExactlyCanonicalTopLevelInterfaces() {
        Set<String> discoveredInterfaceNames = discoverTopLevelInterfaces(RESULT_SERVICE_PACKAGE);

        assertThat(discoveredInterfaceNames).isEqualTo(Set.of(
            "ResultRecordingService",
            "SelfCompletionOrgSnapshotFactsReader",
            "ResultQuestionScoringEvaluator",
            "CountedAssignmentResultValidityGate"
        ));
    }

    @Test
    void resultServicePackageDoesNotAllowGenericDriftNames() {
        assertThat(discoverTopLevelInterfaces(RESULT_SERVICE_PACKAGE))
            .doesNotContain(
                "ResultService",
                "ResultFacade",
                "ResultManagementService",
                "ResultLifecycleService",
                "ResultMutationService",
                "ResultUpdateService",
                "ResultRefreshService",
                "ResultRebuildService",
                "ResultRecalculationService",
                "ScoringService",
                "RecordingFacade",
                "ActiveResultService",
                "ExecutionStateService",
                "ResultQueryService"
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
