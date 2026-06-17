package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
/**
 * Проверяет поведение {@code TestingAdmissionSupportAntiMerge}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class TestingAdmissionSupportAntiMergeTest {

    @Test
    void testingPackageKeepsOnlyScenarioSpecificAdmissionSupportHelpers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(java.util.regex.Pattern.compile(".*AdmissionSupport")));

        Set<String> discoveredTypes = scanner.findCandidateComponents("com.vladislav.training.platform.testing.service")
            .stream()
            .map(beanDefinition -> beanDefinition.getBeanClassName())
            .collect(Collectors.toSet());

        assertThat(discoveredTypes)
            .containsExactlyInAnyOrder(
                "com.vladislav.training.platform.testing.service.AssignedAttemptAdmissionSupport",
                "com.vladislav.training.platform.testing.service.SelfAttemptAdmissionSupport",
                "com.vladislav.training.platform.testing.service.AssignedAttemptAnswerMutationAdmissionSupport",
                "com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationAdmissionSupport"
            )
            .doesNotContain(
                "com.vladislav.training.platform.testing.service.ExecutionSupport",
                "com.vladislav.training.platform.testing.service.ExecutionAdmissionSupport",
                "com.vladislav.training.platform.testing.service.ExecutionAdmissionSupport",
                "com.vladislav.training.platform.testing.service.TestingAdmissionSupport",
                "com.vladislav.training.platform.testing.service.AttemptAdmissionSupport",
                "com.vladislav.training.platform.testing.service.AttemptExecutionSupport",
                "com.vladislav.training.platform.testing.service.AttemptAccessSupport"
            );
    }

    @Test
    void supportHelpersKeepScenarioSpecificMethodVocabularyInsteadOfGenericExecutionChecks() {
        assertThat(publicMethodNames(AssignedAttemptAdmissionSupport.class))
            .containsExactlyInAnyOrder("checkAssignedAttemptStart", "checkAssignedAttemptContinue")
            .doesNotContain("checkExecutionStart", "checkAttemptStart", "checkExecution", "checkAttemptAccess");

        assertThat(publicMethodNames(SelfAttemptAdmissionSupport.class))
            .containsExactlyInAnyOrder("checkSelfAttemptStart", "checkSelfAttemptContinue")
            .doesNotContain("checkExecutionStart", "checkAttemptStart", "checkExecution", "checkAttemptAccess");

        assertThat(publicMethodNames(AssignedAttemptAnswerMutationAdmissionSupport.class))
            .containsExactlyInAnyOrder("checkAssignedAnswerMutation")
            .doesNotContain("checkExecutionMutation", "checkAnswerMutation", "checkExecution", "checkAttemptAccess");

        assertThat(publicMethodNames(SelfAttemptAnswerMutationAdmissionSupport.class))
            .containsExactlyInAnyOrder("checkSelfAnswerMutation")
            .doesNotContain("checkExecutionMutation", "checkAnswerMutation", "checkExecution", "checkAttemptAccess");
    }

    private Set<String> publicMethodNames(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .map(Method::getName)
            .collect(Collectors.toSet());
    }
}
