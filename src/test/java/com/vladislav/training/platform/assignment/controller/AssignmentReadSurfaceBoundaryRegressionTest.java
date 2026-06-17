package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
/**
 * Проверяет, что {@code AssignmentReadSurfaceBoundary} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AssignmentReadSurfaceBoundaryRegressionTest {

    @Test
    void archivedFoundationToAssignmentArchiveMustNotCanonicalizeGenericAssignmentsRootAsExternalScn20ReadContour() {
        assertThat(classPresent("com.vladislav.training.platform.assignment.controller.AssignmentReadController"))
            
            .isFalse();
    }

    @Test
    void archivedFoundationToAssignmentArchiveMustNotTreatCurrentAssignmentDetailTestsAndAdministrativeHistoryAsAcceptedFinalUserFacingSurface() {
        assertThat(classPresent("com.vladislav.training.platform.assignment.controller.AssignmentReadController"))
            
            .isFalse();
    }

    @Test
    void launchControllerMustNotCarryReadEndpointsInsideLaunchCommandBoundary() {
        assertThat(getMappedMethodNames(AssignmentCampaignLaunchController.class))
            
            .isEmpty();
    }

    @Test
    void postLaunchCampaignReadsMustNotBeCanonizedAsPartOfLaunchContour() {
        assertThat(getMappingPaths(AssignmentCampaignLaunchController.class))
            
            .doesNotContain(
                "/{campaignId}",
                "/{campaignId}/recipient-snapshots",
                "/{campaignId}/assignments"
            );
    }

    @Test
    void selfScopedReadControllerMustUseDedicatedAssignedLearningRootRatherThanBroadAssignmentsRoot() {
        assertThat(requestMappingRoot(AssignmentSelfScopedReadController.class))
            
            .isEqualTo("/api/v1/assigned-learning/assignments")
            .isNotEqualTo("/api/v1/assignments");

        assertThat(getMappingPaths(AssignmentSelfScopedReadController.class))
            .containsExactlyInAnyOrder(
                "",
                "/{assignmentId}",
                "/{assignmentId}/learning-context",
                "/{assignmentId}/materials/{materialId}",
                "/{assignmentId}/assignment-tests/{assignmentTestId}/test-context"
            )
            .doesNotContain(
                "/by-user/{userId}",
                "/users/{userId}",
                "/campaigns/{campaignId}",
                "/administrative-actions",
                "/tests",
                "/attempts",
                "/results",
                "/start"
            );
    }

    @Test
    void selfScopedReadControllerMustNotExposeArbitrarySubjectSelectorShape() {
        assertThat(Stream.of(AssignmentSelfScopedReadController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .flatMap(method -> Arrays.stream(method.getParameters()))
            .filter(parameter -> parameter.isAnnotationPresent(RequestParam.class))
            .map(Parameter::getName)
            .toList()).isEmpty();
    }

    private boolean classPresent(String fqcn) {
        try {
            Class.forName(fqcn);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private List<String> getMappedMethodNames(Class<?> controllerClass) {
        return Stream.of(controllerClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .map(Method::getName)
            .toList();
    }

    private List<String> getMappingPaths(Class<?> controllerClass) {
        return Stream.of(controllerClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .flatMap(method -> {
                String[] values = method.getAnnotation(GetMapping.class).value();
                return values.length == 0 ? Stream.of("") : Arrays.stream(values);
            })
            .toList();
    }

    private String requestMappingRoot(Class<?> controllerClass) {
        return controllerClass.getAnnotation(RequestMapping.class).value()[0];
    }
}
