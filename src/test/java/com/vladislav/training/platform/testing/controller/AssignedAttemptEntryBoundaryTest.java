package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.CurrentAttemptResponse;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import com.vladislav.training.platform.testing.service.ActiveAttemptOwnerLocalReadService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Проверяет граничные случаи для {@code AssignedAttemptEntry}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignedAttemptEntryBoundaryTest {

    @Test
    void assignedEntryControllerUsesDedicatedAssignedEntryApiRoot() {
        assertThat(AssignedAttemptEntryController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/assigned-attempt-entries");
    }

    @Test
    void assignedEntryControllerExposesOnlyAssignedEntryHandler() {
        assertThat(Arrays.stream(AssignedAttemptEntryController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .map(Method::getName)
            .toList())
            .containsExactly("enterAssignedAttempt")
            .doesNotContain(
                "findCurrentAssignedAttempt",
                "findCurrentSelfAttempt",
                "saveOrReplaceAnswer",
                "clearAnswer",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt",
                "recordResult"
            );
    }

    @Test
    void assignedEntryControllerDependsOnlyOnAssignedAttemptEntryService() {
        assertThat(fieldTypes(AssignedAttemptEntryController.class))
            .containsExactly(AssignedAttemptEntryService.class)
            .doesNotContain(
                SelfAttemptEntryService.class,
                ActiveAttemptOwnerLocalReadService.class,
                ActiveAttemptAnswerMutationService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                UtcClock.class,
                InteractiveActorResolver.class
            );
    }

    @Test
    void assignedEntryApiDoesNotDriftIntoSelfEntryReadAnswerMutationOrSubmitVocabulary() {
        assertThat(AssignedAttemptEntryController.class.getSimpleName())
            .isEqualTo("AssignedAttemptEntryController")
            .isNotEqualTo("AttemptController")
            .isNotEqualTo("ExecutionController")
            .isNotEqualTo("AttemptApiFacade");
    }

    @Test
    void assignedEntryApiDoesNotReuseCurrentReadOrAnswerMutationDtosAsItsExternalContract() {
        assertThat(Arrays.stream(AssignedAttemptEntryController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .map(Method::getReturnType)
            .toList())
            .containsOnly(AssignedAttemptEntryResponse.class)
            .doesNotContain(CurrentAttemptResponse.class, ActiveAttemptAnswerMutationResponse.class);
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
