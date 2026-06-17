package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
/**
 * Следит за тем, чтобы {@code AssignmentControllerRuntime} не менялся незаметно.
 * Тест держит под контролем важные договорённости.
 */
class AssignmentControllerRuntimeRegressionGateTest {

    @Test
    void launchAndAdministrativeEndpointRuntimeCoverageRemainsPresentWithoutLegitimizingReadSurfaceDrift() {
        assertThat(methodNames(AssignmentCampaignLaunchControllerTest.class))
            .contains(
                "launchEndpointCallsScenarioOrientedCommandServiceAndReturnsCreatedCampaign",
                "launchEndpointPropagatesConflictThroughExistingExceptionMapping",
                "invalidLaunchRequestFailsValidationBeforeServiceCall"
            );

        assertThat(methodNames(AssignmentAdministrativeActionControllerTest.class))
            .contains(
                "cancelEndpointCallsTypedOwnerServiceAndReturnsAssignmentRootResponse",
                "deadlineExtendEndpointCallsTypedOwnerServiceAndReturnsAssignmentRootResponse",
                "replaceWithNewEndpointCallsTypedOwnerServiceAndReturnsAssignmentRootResponse",
                "cancelEndpointPropagatesNotFoundThroughExistingExceptionMapping",
                "deadlineExtendEndpointPropagatesConflictThroughExistingExceptionMapping",
                "cancelEndpointPropagatesPolicyDeniedThroughExistingExceptionMapping",
                "invalidDeadlineExtendBodyFailsValidationBeforeServiceCall",
                "replaceWithNewEndpointPropagatesNotFoundThroughExistingExceptionMapping",
                "replaceWithNewEndpointPropagatesConflictThroughExistingExceptionMapping",
                "replaceWithNewEndpointPropagatesPolicyDeniedThroughExistingExceptionMapping",
                "invalidReplaceBodyFailsValidationBeforeServiceCall",
                "administrativeControllerSurfaceRemainsTypedWithoutPatchCrudOrReadEndpoints"
            );

        assertThat(methodNames(AssignmentReadSurfaceBoundaryRegressionTest.class))
            .contains(
                "archivedFoundationToAssignmentArchiveMustNotCanonicalizeGenericAssignmentsRootAsExternalScn20ReadContour",
                "archivedFoundationToAssignmentArchiveMustNotTreatCurrentAssignmentDetailTestsAndAdministrativeHistoryAsAcceptedFinalUserFacingSurface",
                "launchControllerMustNotCarryReadEndpointsInsideLaunchCommandBoundary",
                "postLaunchCampaignReadsMustNotBeCanonizedAsPartOfLaunchContour",
                "selfScopedReadControllerMustUseDedicatedAssignedLearningRootRatherThanBroadAssignmentsRoot",
                "selfScopedReadControllerMustNotExposeArbitrarySubjectSelectorShape"
            );

        assertThat(methodNames(AssignmentSelfScopedReadControllerTest.class))
            .contains(
                "selfScopedListEndpointUsesTrustedActorIdentityAndReturnsAssignments",
                "selfScopedDetailEndpointUsesTrustedActorIdentityAndReturnsAssignment",
                "assignedLearningContextEndpointUsesTrustedActorIdentityAndReturnsOwnedAssignmentLinkedContext",
                "assignedTestContextEndpointUsesTrustedActorIdentityAndReturnsOwnedAssignmentBoundComposition",
                "requestUserIdParameterDoesNotOverrideResolvedActorIdentity",
                "deniedSelfScopedReadPropagatesThroughExistingExceptionMapping",
                "ownerSafeNotFoundPropagatesFromSelfScopedDetailPath",
                "deniedAssignedLearningContextPropagatesThroughExistingExceptionMapping",
                "ownerSafeNotFoundPropagatesFromAssignedLearningContextPath",
                "ownerSafeNotFoundPropagatesFromAssignedTestContextPath"
            );
    }

    @Test
    void controllerBoundaryAndDtoGuardsRemainPresentForAssignmentHttpSurface() {
        assertThat(methodNames(AssignmentControllerSurfaceBoundaryTest.class))
            .contains(
                "commandAndAdministrativeControllersRemainSeparatedByScenarioTaxonomyAndValidatedRestBoundary",
                "commandAndAdministrativeControllersDoNotExposeGenericCrudOrMutationDrift",
                "commandAndAdministrativeParametersCarryScenarioValidationAnnotations"
            );
        assertThat(methodNames(AssignmentControllerDtoContractTest.class))
            .contains(
                "launchRequestRemainsScenarioOrientedAndDoesNotExpressGenericCampaignCrud",
                "administrativeRequestsRemainTypedAndDoNotDegenerateIntoGenericPatchShape",
                "responsesRemainNarrowRootLevelCarriersWithoutPortalOrExecutionPayloads"
            );
    }

    @Test
    void runtimeSurfaceStillMapsAssignmentExceptionsThroughExistingGlobalHandler() {
        assertThat(handlerMethodNames(GlobalExceptionHandler.class))
            .contains(
                "handleNotFound",
                "handleConflict",
                "handleMethodArgumentNotValid",
                "handleHttpMessageNotReadable",
                "handleHandlerMethodValidation",
                "handlePolicyViolation",
                "handlePersistenceConstraint"
            );

        assertThat(parameterType(GlobalExceptionHandler.class, "handleNotFound")).isEqualTo(NotFoundException.class);
        assertThat(parameterType(GlobalExceptionHandler.class, "handleConflict")).isEqualTo(ConflictException.class);
        assertThat(parameterType(GlobalExceptionHandler.class, "handlePolicyViolation")).isEqualTo(PolicyViolationException.class);
        assertThat(parameterType(GlobalExceptionHandler.class, "handlePersistenceConstraint"))
            .isEqualTo(PersistenceConstraintViolationException.class);
        assertThat(parameterType(GlobalExceptionHandler.class, "handleMethodArgumentNotValid"))
            .isEqualTo(MethodArgumentNotValidException.class);
        assertThat(parameterType(GlobalExceptionHandler.class, "handleHandlerMethodValidation"))
            .isEqualTo(HandlerMethodValidationException.class);
        assertThat(parameterType(GlobalExceptionHandler.class, "handleHttpMessageNotReadable"))
            .isEqualTo(HttpMessageNotReadableException.class);
    }

    @Test
    void commandAndAdministrativeSurfaceRemainSeparatedAgainstCrudAndPatchDrift() {
        assertThat(publicMethodNames(AssignmentCampaignLaunchController.class))
            .contains("launchCampaign")
            .doesNotContain(
                "previewCampaign",
                "previewRecipients",
                "updateAssignmentCampaign",
                "patchAssignmentCampaign",
                "findAssignmentById",
                "findAssignmentTestsByAssignmentId",
                "findAssignmentAdministrativeActionsByAssignmentId",
                "findAssignedLearning"
            );

        assertThat(publicMethodNames(AssignmentAdministrativeActionController.class))
            .containsExactlyInAnyOrder("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain(
                "patchAssignment",
                "updateAssignment",
                "findAssignmentById",
                "findAssignmentsByCampaignId",
                "findAuditEvents",
                "findAssignedLearning"
            );
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> handlerMethodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> publicMethodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Class<?> parameterType(Class<?> type, String methodName) {
        return Stream.of(type.getDeclaredMethods())
            .filter(method -> method.getName().equals(methodName))
            .findFirst()
            .orElseThrow()
            .getParameterTypes()[0];
    }
}
