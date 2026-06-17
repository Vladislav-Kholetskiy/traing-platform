package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AssignmentAdministrativeActionService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignmentAdministrativeActionServiceBoundaryTest {

    @Test
    void administrativeContourRemainsTypedAndDoesNotDegradeIntoGenericPatchSurface() {
        assertThat(Stream.of(AssignmentAdministrativeActionService.class.getDeclaredMethods())
            .map(Method::getName))
            .contains("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain("patchAssignment", "updateAssignment", "changeAssignee", "editAssignmentTests");
        assertThat(Arrays.stream(AssignmentAdministrativeActionService.class.getDeclaredClasses())
            .map(Class::getSimpleName))
            .contains("CancelAssignmentCommand", "ExtendAssignmentDeadlineCommand", "ReplaceWithNewAssignmentCommand");
        assertThat(Arrays.stream(AssignmentAdministrativeActionService.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("replaceWithNewAssignment"))
            .findFirst()
            .orElseThrow()
            .getParameterTypes())
            .containsExactly(AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand.class);
        assertThat(Arrays.stream(
            AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand.class.getRecordComponents()
        ).map(component -> component.getName()))
            .containsExactly("assignmentId", "campaignId", "newCycleDeadlineAt", "note")
            .doesNotContain("userId", "courseId", "status", "assignmentTests", "replacementAssignment");
    }

    @Test
    void cancelRuntimeDependsOnOwnerMutationCarriersRatherThanReadSideOrAssignmentTestEditing() {
        assertThat(Stream.of(AssignmentAdministrativeActionServiceImpl.class.getDeclaredFields())
            .map(Field::getType))
            .contains(AssignmentRepository.class, AssignmentAdministrativeActionRepository.class)
            .doesNotContain(AssignmentQueryService.class, AssignmentCampaignQueryService.class);
    }

    @Test
    void statusRecalculationIsWiredOnlyIntoAssignmentOwnerCommandContours() {
        assertThat(Stream.of(AssignmentCampaignCommandServiceImpl.class.getDeclaredFields())
            .map(Field::getType))
            .contains(AssignmentStatusRecalculationService.class);
        assertThat(Stream.of(AssignmentAdministrativeActionServiceImpl.class.getDeclaredFields())
            .map(Field::getType))
            .contains(AssignmentStatusRecalculationService.class);
        assertThat(Stream.of(AssignmentQueryServiceImpl.class.getDeclaredFields())
            .map(Field::getType))
            .doesNotContain(AssignmentStatusRecalculationService.class);
    }

    @Test
    void criticalAssignmentCommandsUseSingleInteractiveActorResolutionDiscipline() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandServiceImpl.java"))
            .contains("resolveInteractiveActorUserId()")
            .doesNotContain("admissionRequest.actorUserId()");
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentAdministrativeActionServiceImpl.java"))
            .contains("resolveInteractiveActorUserId()")
            .doesNotContain("admissionRequest.actorUserId()");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
