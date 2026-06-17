package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandService;
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
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.AssignedAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
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
/**
 * Собирает набор регрессионных проверок вокруг {@code TestingApiHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class TestingApiHardeningRegressionPackTest {

    private static final List<Class<?>> STAGE11_CONTROLLERS = List.of(
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
    void apiLayerDoesNotExposeGenericExecutionControllerVocabulary() {
        assertThat(controllerSimpleNames())
            .containsExactlyInAnyOrder(
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
                "GenericExecutionApi"
            );
    }

    @Test
    void currentReadAndCommandApisRemainSeparateAndDoNotCollapseIntoSingleAttemptApi() {
        assertThat(mappingMethodNames(CurrentAttemptReadController.class))
            .containsExactlyInAnyOrder("findCurrentAssignedAttempt", "findCurrentSelfAttempt")
            .doesNotContain(
                "saveOrReplaceAnswer",
                "clearAnswer",
                "enterAssignedAttempt",
                "startOrContinueSelfAttempt",
                "submitAssignedAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt"
            );

        assertThat(mappingMethodNames(SelfAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceSelfAnswer", "clearSelfAnswer")
            .doesNotContain("findCurrentAssignedAttempt", "findCurrentSelfAttempt");
        assertThat(mappingMethodNames(AssignedAttemptAnswerMutationController.class))
            .containsExactlyInAnyOrder("saveOrReplaceAssignedAnswer", "clearAssignedAnswer")
            .doesNotContain("findCurrentAssignedAttempt", "findCurrentSelfAttempt");

        assertThat(mappingMethodNames(AssignedAttemptEntryController.class)).containsExactly("enterAssignedAttempt");
        assertThat(mappingMethodNames(SelfAttemptEntryController.class)).containsExactly("startOrContinueSelfAttempt");
        assertThat(mappingMethodNames(AssignedAttemptSubmitController.class)).containsExactly("submitAssignedAttempt");
        assertThat(mappingMethodNames(SelfAttemptSubmitController.class)).containsExactly("submitSelfAttempt");
        assertThat(mappingMethodNames(SelfAttemptAbandonController.class)).containsExactly("abandonSelfAttempt");

        assertThat(STAGE11_CONTROLLERS)
            .allSatisfy(controller -> assertThat(mappingMethodNames(controller)).hasSizeLessThanOrEqualTo(2));
    }

    @Test
    void stage11ApiIsMaterializedAsEightDedicatedSurfacesWithoutGenericMerge() {
        assertThat(STAGE11_CONTROLLERS)
            .hasSize(8)
            .containsExactly(
                CurrentAttemptReadController.class,
                SelfAttemptAnswerMutationController.class,
                AssignedAttemptAnswerMutationController.class,
                AssignedAttemptEntryController.class,
                SelfAttemptEntryController.class,
                AssignedAttemptSubmitController.class,
                SelfAttemptSubmitController.class,
                SelfAttemptAbandonController.class
            );

        Set<String> apiRoots = STAGE11_CONTROLLERS.stream()
            .flatMap(controller -> Arrays.stream(controller.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class).value()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(apiRoots).containsExactlyInAnyOrder(
            "/api/v1/current-attempts",
            "/api/v1/self-attempt-answers",
            "/api/v1/assigned-attempt-answers",
            "/api/v1/assigned-attempt-entries",
            "/api/v1/self-attempt-entries",
            "/api/v1/assigned-attempt-submissions",
            "/api/v1/self-attempt-submissions",
            "/api/v1/self-attempt-abandonments"
        );
    }

    @Test
    void controllerSurfacesKeepDedicatedDependencyShapesPerExecutionSlice() {
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
    void commandApisDoNotReuseForeignDtosAcrossExecutionSlices() {
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
            SelfAttemptAbandonResponse.class
        )).hasSize(7);

        assertThat(fieldTypes(SelfAttemptAnswerMutationController.class)).doesNotContain(CurrentAttemptResponse.class);
        assertThat(fieldTypes(AssignedAttemptAnswerMutationController.class)).doesNotContain(CurrentAttemptResponse.class);
        assertThat(fieldTypes(AssignedAttemptEntryController.class))
            .doesNotContain(CurrentAttemptResponse.class, ActiveAttemptAnswerMutationResponse.class);
        assertThat(fieldTypes(SelfAttemptEntryController.class))
            .doesNotContain(CurrentAttemptResponse.class, ActiveAttemptAnswerMutationResponse.class, AssignedAttemptEntryResponse.class);
        assertThat(fieldTypes(AssignedAttemptSubmitController.class))
            .doesNotContain(
                CurrentAttemptResponse.class,
                ActiveAttemptAnswerMutationResponse.class,
                AssignedAttemptEntryResponse.class,
                SelfAttemptEntryResponse.class
            );
        assertThat(fieldTypes(SelfAttemptSubmitController.class))
            .doesNotContain(
                CurrentAttemptResponse.class,
                ActiveAttemptAnswerMutationResponse.class,
                AssignedAttemptEntryResponse.class,
                SelfAttemptEntryResponse.class,
                AssignedAttemptSubmitResponse.class
            );
        assertThat(fieldTypes(SelfAttemptAbandonController.class))
            .doesNotContain(
                CurrentAttemptResponse.class,
                ActiveAttemptAnswerMutationResponse.class,
                AssignedAttemptEntryResponse.class,
                SelfAttemptEntryResponse.class,
                AssignedAttemptSubmitResponse.class,
                SelfAttemptSubmitResponse.class
            );
    }

    @Test
    void controllerLayerDoesNotPullAuditPolicyAdminCampaignOrResultOwnershipContours() {
        Set<Class<?>> allFieldTypes = STAGE11_CONTROLLERS.stream()
            .flatMap(controller -> fieldTypes(controller).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(allFieldTypes)
            .doesNotContain(
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                AssignmentCampaignCommandService.class
            );

        Set<Class<?>> allParameterTypes = STAGE11_CONTROLLERS.stream()
            .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
            .flatMap(method -> Arrays.stream(method.getParameterTypes()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(allParameterTypes)
            .doesNotContain(
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                AssignmentCampaignCommandService.class
            );
    }

    private Set<String> controllerSimpleNames() {
        return STAGE11_CONTROLLERS.stream()
            .map(Class::getSimpleName)
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
}
