package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.service.ActiveAttemptAnswerMutationService;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
/**
 * Проверяет граничные случаи для {@code CurrentAttemptRead}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class CurrentAttemptReadBoundaryTest {

    @Test
    void currentAttemptControllerExposesOnlyReadVocabularyAndGetHandlers() {
        assertThat(Arrays.stream(CurrentAttemptReadController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(Method::getName)
            .toList())
            .contains("findCurrentSelfAttempt")
            .doesNotContain(
                "startAssignedAttempt",
                "startSelfAttempt",
                "resumeAttempt",
                "submitAssignedAttempt",
                "submitAttempt",
                "saveAnswer",
                "clearAnswer",
                "recordResult"
            );
    }

    @Test
    void currentAttemptControllerStaysReadOnlyBoundaryAndDoesNotPullWriteSideExecutionDependencies() {
        assertThat(fieldTypes(CurrentAttemptReadController.class))
            .contains(
                AssignedCurrentAttemptReadService.class,
                SelfCurrentAttemptReadService.class,
                InteractiveActorResolver.class
            )
            .doesNotContain(
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                ActiveAttemptAnswerMutationService.class,
                AssignedAttemptSubmissionService.class,
                AssignedAttemptSubmitSequencingService.class,
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentStatusRecalculationService.class
            );
    }

    @Test
    void currentAttemptApiDoesNotDriftIntoGenericExecutionControllerVocabulary() {
        assertThat(CurrentAttemptReadController.class.getSimpleName())
            .isEqualTo("CurrentAttemptReadController")
            .isNotEqualTo("AttemptController")
            .isNotEqualTo("ExecutionController")
            .isNotEqualTo("AttemptApiFacade");
    }

    @Test
    void currentAttemptControllerSourceStaysThinWithoutRepositoryOrOwnerLocalShortcuts() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/controller/CurrentAttemptReadController.java"
        ));

        assertThat(source)
            .doesNotContain("Repository")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("findActiveAssignedAttemptForActor(")
            .doesNotContain("findActiveSelfAttempt(")
            .doesNotContain("startOrContinueAssignedAttempt(")
            .doesNotContain("startOrContinueSelfAttempt(")
            .doesNotContain("saveOrReplaceAnswer(")
            .doesNotContain("submitAssignedAttempt(")
            .doesNotContain("submitSelfAttempt(")
            .doesNotContain("abandonSelfAttempt(")
            .doesNotContain("recordResult(")
            .contains("assignedCurrentAttemptReadService.findCurrentAssignedAttemptForActor(")
            .contains("selfCurrentAttemptReadService.findCurrentSelfAttemptForActor(");
    }

    @Test
    void currentAttemptExternalReadDtoIsNotReusedByCommandControllers() throws Exception {
        try (Stream<Path> controllerSources = Files.walk(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/controller"
        ))) {
            List<Path> nonReadControllerSources = controllerSources
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.getFileName().toString().equals("CurrentAttemptReadController.java"))
                .filter(path -> !path.getFileName().toString().equals("CurrentAttemptResponse.java"))
                .toList();

            for (Path sourcePath : nonReadControllerSources) {
                assertThat(Files.readString(sourcePath))
                    
                    .doesNotContain("CurrentAttemptResponse");
            }
        }
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}
