package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.controller.dto.CurrentAttemptResponse;
import com.vladislav.training.platform.testing.service.ActiveAttemptOwnerLocalReadService;
import com.vladislav.training.platform.testing.service.AssignedAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Проверяет граничные случаи для {@code AttemptAnswerMutationApiSplit}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AttemptAnswerMutationApiSplitBoundaryTest {

    @Test
    void selfAndAssignedAnswerMutationControllersUseScenarioSpecificApiRoots() {
        assertThat(SelfAttemptAnswerMutationController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/self-attempt-answers");
        assertThat(AssignedAttemptAnswerMutationController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/api/v1/assigned-attempt-answers");
    }

    @Test
    void answerMutationControllersExposeOnlyScenarioSpecificAnswerMutationHandlers() {
        assertThat(mappingMethodNames(SelfAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceSelfAnswer", "clearSelfAnswer")
            .doesNotContain(
                "saveOrReplaceAnswer",
                "clearAnswer",
                "findCurrentAssignedAttempt",
                "findCurrentSelfAttempt",
                "startAssignedAttempt",
                "startSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt"
            );
        assertThat(mappingMethodNames(AssignedAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceAssignedAnswer", "clearAssignedAnswer")
            .doesNotContain(
                "saveOrReplaceAnswer",
                "clearAnswer",
                "findCurrentAssignedAttempt",
                "findCurrentSelfAttempt",
                "startAssignedAttempt",
                "startSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt"
            );
    }

    @Test
    void answerMutationControllersDependOnlyOnScenarioSpecificEntryServices() {
        assertThat(fieldTypes(SelfAttemptAnswerMutationController.class))
            .containsExactly(SelfAttemptAnswerMutationEntryService.class)
            .doesNotContain(
                ActiveAttemptOwnerLocalReadService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                InteractiveActorResolver.class,
                UtcClock.class
            );
        assertThat(fieldTypes(AssignedAttemptAnswerMutationController.class))
            .containsExactly(AssignedAttemptAnswerMutationEntryService.class)
            .doesNotContain(
                ActiveAttemptOwnerLocalReadService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                InteractiveActorResolver.class,
                UtcClock.class
            );
    }

    @Test
    void scenarioSplitAnswerMutationApisDoNotDriftIntoGenericExecutionVocabulary() {
        assertThat(SelfAttemptAnswerMutationController.class.getSimpleName())
            .isEqualTo("SelfAttemptAnswerMutationController")
            .isNotEqualTo("ActiveAttemptAnswerMutationController")
            .isNotEqualTo("AttemptController")
            .isNotEqualTo("ExecutionController")
            .isNotEqualTo("AttemptApiFacade");
        assertThat(AssignedAttemptAnswerMutationController.class.getSimpleName())
            .isEqualTo("AssignedAttemptAnswerMutationController")
            .isNotEqualTo("ActiveAttemptAnswerMutationController")
            .isNotEqualTo("AttemptController")
            .isNotEqualTo("ExecutionController")
            .isNotEqualTo("AttemptApiFacade");
    }

    @Test
    void scenarioSplitAnswerMutationApisDoNotReuseCurrentAttemptReadDtoAsExternalContract() {
        assertThat(returnTypes(SelfAttemptAnswerMutationController.class))
            .containsOnly(ActiveAttemptAnswerMutationResponse.class)
            .doesNotContain(CurrentAttemptResponse.class);
        assertThat(returnTypes(AssignedAttemptAnswerMutationController.class))
            .containsOnly(ActiveAttemptAnswerMutationResponse.class)
            .doesNotContain(CurrentAttemptResponse.class);
    }

    private List<String> mappingMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(DeleteMapping.class))
            .map(Method::getName)
            .toList();
    }

    private List<Class<?>> returnTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(DeleteMapping.class))
            .map(Method::getReturnType)
            .toList();
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
