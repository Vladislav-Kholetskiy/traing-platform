package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.controller.dto.CancelAssignmentRequest;
import com.vladislav.training.platform.assignment.controller.dto.ExtendAssignmentDeadlineRequest;
import com.vladislav.training.platform.assignment.controller.dto.LaunchAssignmentCampaignRequest;
import com.vladislav.training.platform.assignment.controller.dto.ReplaceAssignmentWithNewRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Проверяет граничные случаи для {@code AssignmentControllerSurface}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignmentControllerSurfaceBoundaryTest {

    @Test
    void commandAndAdministrativeControllersRemainSeparatedByScenarioTaxonomyAndValidatedRestBoundary() {
        assertControllerBoundaryAnnotations(AssignmentCampaignLaunchController.class, "/api/v1/assignment-campaigns");
        assertControllerBoundaryAnnotations(AssignmentAdministrativeActionController.class, "/api/v1/assignment-administrative-actions");
    }

    @Test
    void commandAndAdministrativeControllersDoNotExposeGenericCrudOrMutationDrift() {
        assertThat(handlerMethodNames(AssignmentCampaignLaunchController.class))
            .contains("launchCampaign")
            .doesNotContain("createAssignmentCampaign", "updateAssignmentCampaign", "patchAssignmentCampaign", "deleteAssignmentCampaign");

        assertThat(handlerMethodNames(AssignmentAdministrativeActionController.class))
            .containsExactlyInAnyOrder("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain("patchAssignment", "updateAssignment", "changeAssignee", "markCompleted", "editAssignmentTests");

        assertThat(hasAnyForbiddenHttpMutationMapping(AssignmentCampaignLaunchController.class)).isFalse();
        assertThat(hasAnyForbiddenHttpMutationMapping(AssignmentAdministrativeActionController.class)).isFalse();
    }

    @Test
    void commandAndAdministrativeParametersCarryScenarioValidationAnnotations() throws NoSuchMethodException {
        Method launchMethod = AssignmentCampaignLaunchController.class.getDeclaredMethod(
            "launchCampaign",
            LaunchAssignmentCampaignRequest.class
        );
        assertThat(launchMethod.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(launchMethod.getParameters()[0].isAnnotationPresent(Valid.class)).isTrue();
        assertThat(launchMethod.isAnnotationPresent(PostMapping.class)).isTrue();

        Method cancelMethod = AssignmentAdministrativeActionController.class.getDeclaredMethod(
            "cancelAssignment",
            Long.class,
            CancelAssignmentRequest.class
        );
        assertThat(cancelMethod.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
        assertThat(cancelMethod.getParameters()[0].isAnnotationPresent(Positive.class)).isTrue();
        assertThat(cancelMethod.getParameters()[1].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(cancelMethod.getParameters()[1].isAnnotationPresent(Valid.class)).isTrue();

        Method extendMethod = AssignmentAdministrativeActionController.class.getDeclaredMethod(
            "extendAssignmentDeadline",
            Long.class,
            ExtendAssignmentDeadlineRequest.class
        );
        assertThat(extendMethod.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
        assertThat(extendMethod.getParameters()[0].isAnnotationPresent(Positive.class)).isTrue();
        assertThat(extendMethod.getParameters()[1].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(extendMethod.getParameters()[1].isAnnotationPresent(Valid.class)).isTrue();

        Method replaceMethod = AssignmentAdministrativeActionController.class.getDeclaredMethod(
            "replaceWithNewAssignment",
            Long.class,
            ReplaceAssignmentWithNewRequest.class
        );
        assertThat(replaceMethod.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
        assertThat(replaceMethod.getParameters()[0].isAnnotationPresent(Positive.class)).isTrue();
        assertThat(replaceMethod.getParameters()[1].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(replaceMethod.getParameters()[1].isAnnotationPresent(Valid.class)).isTrue();
    }

    private void assertControllerBoundaryAnnotations(Class<?> controllerClass, String expectedRootPath) {
        assertThat(controllerClass.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(controllerClass.isAnnotationPresent(Validated.class)).isTrue();
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly(expectedRootPath);
    }

    private String[] handlerMethodNames(Class<?> controllerClass) {
        return Stream.of(controllerClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toArray(String[]::new);
    }

    private boolean hasAnyForbiddenHttpMutationMapping(Class<?> controllerClass) {
        return Arrays.stream(controllerClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .anyMatch(method -> method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class));
    }
}
