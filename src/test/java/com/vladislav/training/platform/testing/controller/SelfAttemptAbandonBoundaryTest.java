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
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptAbandonResponse;
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
 * Проверяет граничные случаи для {@code SelfAttemptAbandon}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class SelfAttemptAbandonBoundaryTest {

    @Test
    void selfAbandonControllerUsesDedicatedSelfAbandonApiRoot() {
        assertThat(SelfAttemptAbandonController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/self-attempt-abandonments");
    }

    @Test
    void selfAbandonControllerExposesOnlySelfAbandonHandler() {
        assertThat(Arrays.stream(SelfAttemptAbandonController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .map(Method::getName)
            .toList())
            .containsExactly("abandonSelfAttempt")
            .doesNotContain(
                "findCurrentAssignedAttempt",
                "findCurrentSelfAttempt",
                "saveOrReplaceAnswer",
                "clearAnswer",
                "startOrContinueAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "recordResult"
            );
    }

    @Test
    void selfAbandonControllerDependsOnlyOnSelfAttemptAbandonSequencingService() {
        assertThat(fieldTypes(SelfAttemptAbandonController.class))
            .containsExactly(SelfAttemptAbandonSequencingService.class)
            .doesNotContain(
                SelfAttemptSubmitSequencingService.class,
                AssignedAttemptSubmitSequencingService.class,
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
    void selfAbandonApiDoesNotDriftIntoSubmitEntryReadOrAnswerMutationVocabulary() {
        assertThat(SelfAttemptAbandonController.class.getSimpleName())
            .isEqualTo("SelfAttemptAbandonController")
            .isNotEqualTo("AttemptController")
            .isNotEqualTo("ExecutionController")
            .isNotEqualTo("AttemptApiFacade")
            .isNotEqualTo("CloseManager");
    }

    @Test
    void selfAbandonApiDoesNotReuseOtherExecutionDtosAsItsExternalContract() {
        assertThat(Arrays.stream(SelfAttemptAbandonController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .map(Method::getReturnType)
            .toList())
            .containsOnly(SelfAttemptAbandonResponse.class)
            .doesNotContain(
                CurrentAttemptResponse.class,
                ActiveAttemptAnswerMutationResponse.class,
                AssignedAttemptEntryResponse.class,
                SelfAttemptEntryResponse.class,
                AssignedAttemptSubmitResponse.class,
                SelfAttemptSubmitResponse.class
            );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
