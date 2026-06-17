package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Собирает набор регрессионных проверок вокруг {@code AssignmentCountedResultHandoff}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class AssignmentCountedResultHandoffRegressionPackTest {

    @Test
    void countedResultHandoffDoesNotDriftIntoAdministrativeActionVocabulary() {
        assertThat(methodNames(AssignmentCountedResultHandoffService.class))
            .containsExactly("acceptValidCountedAssignmentResult")
            .doesNotContain(
                "cancelAssignment",
                "extendAssignmentDeadline",
                "replaceWithNewAssignment",
                "launchAssignmentCampaign",
                "patchAssignmentResult"
            );
    }

    @Test
    void countedResultHandoffDoesNotDependOnAssignmentAdministrativeActionServices() {
        assertThat(fieldTypes(AssignmentCountedResultHandoffServiceImpl.class))
            .doesNotContain(
                AssignmentAdministrativeActionService.class,
                AssignmentAdministrativeActionServiceImpl.class,
                CriticalCommandAuditSupport.class
            );
    }

    @Test
    void countedResultHandoffDoesNotDependOnCampaignCommandServices() {
        assertThat(fieldTypes(AssignmentCountedResultHandoffServiceImpl.class))
            .doesNotContain(
                AssignmentCampaignCommandService.class,
                AssignmentCampaignCommandServiceImpl.class,
                AssignmentCampaignPreviewService.class,
                AssignmentCampaignQueryService.class
            );
    }

    @Test
    void countedResultHandoffDoesNotDirectlyOwnAssignmentStatusTruth() throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCountedResultHandoffServiceImpl.java");

        assertThat(source)
            .contains("closeAssignmentTestWithCountedResult")
            .doesNotContain(
                "saveAssignment(",
                "AssignmentStatus.COMPLETED",
                "AssignmentStatus.OVERDUE",
                "recalculateAssignmentStatus(",
                "setStatus"
            );
    }

    @Test
    void countedResultHandoffDelegatesStatusRefreshInsteadOfComputingStatusInline() throws IOException {
        String handoffSource = read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCountedResultHandoffServiceImpl.java");
        String statusSource = read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationServiceImpl.java");

        assertThat(handoffSource)
            .contains("assignmentCommandService.closeAssignmentTestWithCountedResult")
            .doesNotContain(
                "deadlineAt().isBefore",
                "cancelledAt()",
                "counted-result proof for COMPLETED"
            );
        assertThat(statusSource)
            .contains("counted-result proof for COMPLETED")
            .contains("refreshAssignmentStatusCache")
            .doesNotContain("AssignmentCountedResultHandoffService");
    }

    @Test
    void countedResultHandoffDoesNotPullAuditPolicyOrControllerContours() {
        assertThat(fieldTypes(AssignmentCountedResultHandoffServiceImpl.class))
            .doesNotContain(AccessSpecificationPolicy.class);
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
