package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.CurrentAttemptResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptSubmitResponse;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import com.vladislav.training.platform.testing.service.ActiveAttemptOwnerLocalReadService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Проверяет граничные случаи для {@code SelfAttemptSubmit}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class SelfAttemptSubmitBoundaryTest {

    @Test
    void selfSubmitControllerUsesDedicatedSelfSubmitApiRoot() {
        assertThat(SelfAttemptSubmitController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/self-attempt-submissions");
    }

    @Test
    void selfSubmitControllerExposesOnlySelfSubmitHandler() {
        assertThat(Arrays.stream(SelfAttemptSubmitController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .map(Method::getName)
            .toList())
            .containsExactly("submitSelfAttempt")
            .doesNotContain(
                "findCurrentAssignedAttempt",
                "findCurrentSelfAttempt",
                "saveOrReplaceAnswer",
                "clearAnswer",
                "startOrContinueAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "abandonSelfAttempt",
                "recordResult"
            );
    }

    @Test
    void selfSubmitControllerDependsOnlyOnSelfAttemptSubmitSequencingService() {
        assertThat(fieldTypes(SelfAttemptSubmitController.class))
            .containsExactly(SelfAttemptSubmitSequencingService.class)
            .doesNotContain(
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptEntryService.class,
                ActiveAttemptOwnerLocalReadService.class,
                ActiveAttemptAnswerMutationService.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                UtcClock.class,
                InteractiveActorResolver.class
            );
    }

    @Test
    void selfSubmitApiDoesNotDriftIntoAssignedSubmitAbandonEntryReadOrAnswerMutationVocabulary() {
        assertThat(SelfAttemptSubmitController.class.getSimpleName())
            .isEqualTo("SelfAttemptSubmitController")
            .isNotEqualTo("AttemptController")
            .isNotEqualTo("ExecutionController")
            .isNotEqualTo("AttemptApiFacade")
            .isNotEqualTo("SubmissionController");
    }

    @Test
    void selfSubmitApiDoesNotReuseOtherExecutionDtosAsItsExternalContract() {
        assertThat(Arrays.stream(SelfAttemptSubmitController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .map(Method::getReturnType)
            .toList())
            .containsOnly(SelfAttemptSubmitResponse.class)
            .doesNotContain(
                CurrentAttemptResponse.class,
                ActiveAttemptAnswerMutationResponse.class,
                AssignedAttemptEntryResponse.class,
                SelfAttemptEntryResponse.class,
                AssignedAttemptSubmitResponse.class
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
