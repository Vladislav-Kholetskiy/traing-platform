package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.controller.SelfVisibleTestingReadController;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Проверяет, что {@code SelfEntryNoImplicitStart} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfEntryNoImplicitStartRegressionTest {

    @Test
    void selfEntryReadChainDoesNotDependOnCommandResultAuditOrAdmissionCollaborators() {
        assertThat(fieldTypes(SelfVisibleTestingReadController.class))
            .containsExactly(SelfVisibleTestingReadService.class)
            .doesNotContain(
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AttemptStatusRecalculationService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class,
                AssignedAttemptAdmissionSupport.class
            );

        assertThat(fieldTypes(SelfVisibleTestingReadService.class))
            .doesNotContain(
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AttemptStatusRecalculationService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class,
                AssignedAttemptAdmissionSupport.class
            )
            .extracting(Class::getSimpleName)
            .noneMatch(name -> name.contains("Admission"))
            .noneMatch(name -> name.contains("Audit"));
    }

    @Test
    void selfEntryReadBoundaryKeepsReadOnlyVocabularyInsteadOfImplicitStartVocabulary() {
        assertThat(SelfVisibleTestingReadController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/self-testing/tests");

        assertThat(Arrays.stream(SelfVisibleTestingReadController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(Method::getName)
            .toList())
            .containsExactlyInAnyOrder("findSelfVisibleTests", "findSelfVisibleTestById", "findSelfVisibleTopicById")
            .noneMatch(this::containsExecutionLikeVocabulary);

        assertThat(Arrays.stream(SelfVisibleTestingReadService.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList())
            .containsExactlyInAnyOrder("findSelfVisibleTests", "findSelfVisibleTestById", "findSelfVisibleTopicById")
            .noneMatch(this::containsExecutionLikeVocabulary);
    }

    private boolean containsExecutionLikeVocabulary(String methodName) {
        String normalized = methodName.toLowerCase();
        return normalized.contains("start")
            || normalized.contains("resume")
            || normalized.contains("submit")
            || normalized.contains("launch")
            || normalized.contains("attempt")
            || normalized.contains("record");
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
