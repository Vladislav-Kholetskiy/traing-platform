package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.controller.AssignmentSelfScopedReadController;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContextReader;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.AttemptStatusRecalculationService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
/**
 * Проверяет, что {@code AssignmentScn20NoSideEffect} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AssignmentScn20NoSideEffectRegressionTest {

    private static final List<Class<?>> FORBIDDEN_COMMAND_SIDE_TYPES = List.of(
        AssignedAttemptEntryService.class,
        SelfAttemptEntryService.class,
        AssignedAttemptSubmissionService.class,
        ActiveAttemptAnswerMutationService.class,
        AttemptStatusRecalculationService.class,
        ResultRecordingService.class,
        CriticalCommandAuditSupport.class
    );

    @Test
    void selfScopedReadControllerMustDependOnlyOnReadSideCollaborators() {
        assertThat(declaredFieldTypes(AssignmentSelfScopedReadController.class))
            .containsExactlyInAnyOrder(
                AssignmentSelfScopedQueryService.class,
                InteractiveActorResolver.class,
                PublishedCourseLearningContextReader.class,
                TestRepository.class,
                TopicRepository.class
            );

        assertThat(constructorParameterTypes(AssignmentSelfScopedReadController.class))
            .containsExactlyInAnyOrder(
                AssignmentSelfScopedQueryService.class,
                InteractiveActorResolver.class,
                PublishedCourseLearningContextReader.class,
                TestRepository.class,
                TopicRepository.class
            );

        assertNoForbiddenTypesReferenced(AssignmentSelfScopedReadController.class);
    }

    @Test
    void selfScopedQueryServicesMustStayOnAssignmentAnchoredPolicyAwareReadChain() {
        assertThat(declaredFieldTypes(AssignmentSelfScopedQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                AssignmentSelfScopedReadRepository.class,
                com.vladislav.training.platform.access.service.AccessSpecificationPolicy.class,
                com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver.class,
                PublishedCourseLearningContextReader.class,
                AssignedTestContextProjectionReader.class
            );
        assertThat(constructorParameterTypes(AssignmentSelfScopedQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                AssignmentSelfScopedReadRepository.class,
                com.vladislav.training.platform.access.service.AccessSpecificationPolicy.class,
                com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver.class,
                PublishedCourseLearningContextReader.class,
                AssignedTestContextProjectionReader.class
            );

        assertNoForbiddenTypesReferenced(AssignmentSelfScopedQueryServiceImpl.class);
    }

    @Test
    void selfScopedReadControllerMustNotExposeExecutionLikeHandlerVocabulary() {
        assertThat(Arrays.stream(AssignmentSelfScopedReadController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(java.lang.reflect.Method::getName)
            .toList())
            .containsExactlyInAnyOrder(
                "findSelfAssignments",
                "findSelfAssignmentById",
                "findAssignedLearningContext",
                "findAssignedMaterialContent",
                "findAssignedTestContext"
            )
            .doesNotContain(
                "startAssignedAttempt",
                "startSelfAttempt",
                "resumeAttempt",
                "submitAttempt",
                "recordResult",
                "ensureRecorded"
            );
    }

    private void assertNoForbiddenTypesReferenced(Class<?> type) {
        assertThat(declaredFieldTypes(type))
            .doesNotContainAnyElementsOf(FORBIDDEN_COMMAND_SIDE_TYPES);
        assertThat(constructorParameterTypes(type))
            .doesNotContainAnyElementsOf(FORBIDDEN_COMMAND_SIDE_TYPES);
    }

    private List<Class<?>> declaredFieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .toList();
    }

    private List<Class<?>> constructorParameterTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredConstructors())
            .flatMap(constructor -> Arrays.stream(parameterTypes(constructor)))
            .toList();
    }

    private Class<?>[] parameterTypes(Constructor<?> constructor) {
        return constructor.getParameterTypes();
    }
}
