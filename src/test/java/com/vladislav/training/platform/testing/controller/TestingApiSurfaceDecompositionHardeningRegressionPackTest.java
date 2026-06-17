package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.CurrentAttemptResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptAbandonResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestCatalogEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTopicResponse;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.AssignedAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import com.vladislav.training.platform.testing.service.SelfVisibleTestingReadService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 * Собирает набор регрессионных проверок вокруг {@code TestingApiSurfaceDecompositionHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class TestingApiSurfaceDecompositionHardeningRegressionPackTest {

    private static final List<Class<?>> CANONICAL_TESTING_CONTROLLERS = List.of(
        SelfVisibleTestingReadController.class,
        CurrentAttemptReadController.class,
        SelfAttemptAnswerMutationController.class,
        AssignedAttemptAnswerMutationController.class,
        AssignedAttemptEntryController.class,
        SelfAttemptEntryController.class,
        AssignedAttemptSubmitController.class,
        SelfAttemptSubmitController.class,
        SelfAttemptAbandonController.class
    );

    @Test
    void apiLayerDoesNotMaterializeGenericExecutionControllerVocabularyOrMergedAttemptRoots() {
        assertThat(controllerSimpleNames())
            .containsExactlyInAnyOrder(
                "SelfVisibleTestingReadController",
                "CurrentAttemptReadController",
                "SelfAttemptAnswerMutationController",
                "AssignedAttemptAnswerMutationController",
                "AssignedAttemptEntryController",
                "SelfAttemptEntryController",
                "AssignedAttemptSubmitController",
                "SelfAttemptSubmitController",
                "SelfAttemptAbandonController"
            )
            .doesNotContain(
                "AttemptController",
                "ExecutionController",
                "AttemptApiFacade",
                "SubmissionController",
                "CloseManager",
                "GenericExecutionApi",
                "AssignedAttemptExpiryController"
            );

        assertThat(apiRoots())
            .doesNotContain("/api/v1/attempts", "/api/v1/execution", "/api/v1/attempt-commands");
    }

    @Test
    void readAndCommandSurfacesRemainSeparateWithoutAssignedSelfSubmitAbandonOrExpiryMerge() {
        assertThat(mappingMethodNames(SelfVisibleTestingReadController.class))
            .containsExactlyInAnyOrder("findSelfVisibleTests", "findSelfVisibleTestById", "findSelfVisibleTopicById")
            .doesNotContain(
                "enterAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt",
                "expireAssignedAttempt",
                "submitAttempt",
                "completeAttempt",
                "executeAttemptAction",
                "handleAttemptLifecycle",
                "mutateAttempt",
                "attemptCommand"
            );

        assertThat(mappingMethodNames(CurrentAttemptReadController.class))
            .containsExactlyInAnyOrder("findCurrentAssignedAttempt", "findCurrentSelfAttempt")
            .doesNotContain(
                "saveOrReplaceAnswer",
                "clearAnswer",
                "enterAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt",
                "expireAssignedAttempt"
            );

        assertThat(mappingMethodNames(SelfAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceSelfAnswer", "clearSelfAnswer")
            .doesNotContain(
                "findSelfVisibleTests",
                "findCurrentAssignedAttempt",
                "enterAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt",
                "expireAssignedAttempt"
            );

        assertThat(mappingMethodNames(AssignedAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceAssignedAnswer", "clearAssignedAnswer")
            .doesNotContain(
                "findSelfVisibleTests",
                "findCurrentAssignedAttempt",
                "enterAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt",
                "expireAssignedAttempt"
            );

        assertThat(mappingMethodNames(AssignedAttemptEntryController.class)).containsExactly("enterAssignedAttempt");
        assertThat(mappingMethodNames(SelfAttemptEntryController.class)).containsExactly("startOrContinueSelfAttempt");
        assertThat(mappingMethodNames(AssignedAttemptSubmitController.class)).containsExactly("submitAssignedAttempt");
        assertThat(mappingMethodNames(SelfAttemptSubmitController.class)).containsExactly("submitSelfAttempt");
        assertThat(mappingMethodNames(SelfAttemptAbandonController.class)).containsExactly("abandonSelfAttempt");

        assertThat(CANONICAL_TESTING_CONTROLLERS)
            .allSatisfy(controller -> assertThat(mappingMethodNames(controller))
                .doesNotContain("submitAttempt", "completeAttempt", "executeAttemptAction", "handleAttemptLifecycle", "mutateAttempt", "attemptCommand"));
    }

    @Test
    void controllerLayerDoesNotIntroduceExpiryControllerOrControllerLevelContinuationOwners() {
        Set<Class<?>> allFieldTypes = CANONICAL_TESTING_CONTROLLERS.stream()
            .flatMap(controller -> fieldTypes(controller).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(allFieldTypes)
            .doesNotContain(
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignmentCommandService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                AssignmentCampaignCommandService.class
            );

        assertThat(fieldTypes(SelfVisibleTestingReadController.class)).containsExactly(SelfVisibleTestingReadService.class);
        assertThat(fieldTypes(CurrentAttemptReadController.class))
            .containsExactlyInAnyOrder(
                AssignedCurrentAttemptReadService.class,
                SelfCurrentAttemptReadService.class,
                InteractiveActorResolver.class
            );
        assertThat(fieldTypes(SelfAttemptAnswerMutationController.class))
            .containsExactly(SelfAttemptAnswerMutationEntryService.class);
        assertThat(fieldTypes(AssignedAttemptAnswerMutationController.class))
            .containsExactly(AssignedAttemptAnswerMutationEntryService.class);
        assertThat(fieldTypes(AssignedAttemptEntryController.class)).containsExactly(AssignedAttemptEntryService.class);
        assertThat(fieldTypes(SelfAttemptEntryController.class)).containsExactly(SelfAttemptEntryService.class);
        assertThat(fieldTypes(AssignedAttemptSubmitController.class)).containsExactly(AssignedAttemptSubmissionService.class);
        assertThat(fieldTypes(SelfAttemptSubmitController.class)).containsExactly(SelfAttemptSubmitSequencingService.class);
        assertThat(fieldTypes(SelfAttemptAbandonController.class)).containsExactly(SelfAttemptAbandonSequencingService.class);
    }

    @Test
    void externalDtosRemainDedicatedAndDoNotCollapseIntoGenericExecutionMutationContract() {
        assertThat(returnTypesOf(SelfVisibleTestingReadController.class))
            .containsExactlyInAnyOrder(java.util.List.class, SelfVisibleTestResponse.class, SelfVisibleTopicResponse.class);
        assertThat(returnTypesOf(CurrentAttemptReadController.class)).containsOnly(CurrentAttemptResponse.class);
        assertThat(returnTypesOf(SelfAttemptAnswerMutationController.class)).containsOnly(ActiveAttemptAnswerMutationResponse.class);
        assertThat(returnTypesOf(AssignedAttemptAnswerMutationController.class)).containsOnly(ActiveAttemptAnswerMutationResponse.class);
        assertThat(returnTypesOf(AssignedAttemptEntryController.class)).containsOnly(AssignedAttemptEntryResponse.class);
        assertThat(returnTypesOf(SelfAttemptEntryController.class)).containsOnly(SelfAttemptEntryResponse.class);
        assertThat(returnTypesOf(AssignedAttemptSubmitController.class)).containsOnly(AssignedAttemptSubmitResponse.class);
        assertThat(returnTypesOf(SelfAttemptSubmitController.class)).containsOnly(SelfAttemptSubmitResponse.class);
        assertThat(returnTypesOf(SelfAttemptAbandonController.class)).containsOnly(SelfAttemptAbandonResponse.class);

        assertThat(Set.of(
            CurrentAttemptResponse.class,
            ActiveAttemptAnswerMutationResponse.class,
            AssignedAttemptEntryResponse.class,
            SelfAttemptEntryResponse.class,
            AssignedAttemptSubmitResponse.class,
            SelfAttemptSubmitResponse.class,
            SelfAttemptAbandonResponse.class,
            SelfVisibleTestCatalogEntryResponse.class,
            SelfVisibleTestResponse.class,
            SelfVisibleTopicResponse.class
        )).hasSize(10);

        String selfVisibleReadSource = readControllerSource(SelfVisibleTestingReadController.class);
        assertThat(selfVisibleReadSource)
            .contains("SelfVisibleTestCatalogEntryResponse")
            .contains("SelfVisibleTestResponse")
            .contains("SelfVisibleTopicResponse")
            .doesNotContain(
                "CurrentAttemptResponse",
                "AssignedAttemptEntryResponse",
                "AssignedAttemptSubmitResponse",
                "SelfAttemptSubmitResponse",
                "SelfAttemptAbandonResponse"
            );
    }

    @Test
    void packageAndSourceLevelVocabularyDoNotDriftBackIntoGenericExecutionFacadeWording() throws Exception {
        String packageInfo = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/java/com/vladislav/training/platform/testing/controller/package-info.java")
        );

        assertThat(packageInfo)
            .contains("must not become a single generic execution surface")
            .contains("Read adapters and command adapters remain separate")
            .doesNotContain("single mutation surface", "AttemptController", "ExecutionController");

        String combinedControllerSources = CANONICAL_TESTING_CONTROLLERS.stream()
            .map(this::readControllerSource)
            .collect(Collectors.joining("\n"));

        assertThat(combinedControllerSources)
            .doesNotContain(
                "submitAttempt(",
                "completeAttempt(",
                "executeAttemptAction(",
                "handleAttemptLifecycle(",
                "mutateAttempt(",
                "attemptCommand(",
                "finalizeAttemptAndAssignment",
                "AssignmentCountedResultHandoffService",
                "ResultRecordingService"
            );
    }

    private Set<String> controllerSimpleNames() {
        return CANONICAL_TESTING_CONTROLLERS.stream()
            .map(Class::getSimpleName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> apiRoots() {
        return CANONICAL_TESTING_CONTROLLERS.stream()
            .flatMap(controller -> Arrays.stream(controller.getAnnotation(RequestMapping.class).value()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> mappingMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(this::isRequestHandler)
            .map(Method::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Class<?>> returnTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(this::isRequestHandler)
            .map(Method::getReturnType)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isRequestHandler(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
            || method.isAnnotationPresent(PostMapping.class)
            || method.isAnnotationPresent(PutMapping.class)
            || method.isAnnotationPresent(DeleteMapping.class);
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private String readControllerSource(Class<?> controllerType) {
        String path = "src/main/java/com/vladislav/training/platform/testing/controller/" + controllerType.getSimpleName() + ".java";
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(path));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read controller source: " + path, exception);
        }
    }
}
