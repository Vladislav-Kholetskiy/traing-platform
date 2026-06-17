package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AssignmentCountedResultHandoffService}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignmentCountedResultHandoffServiceBoundaryTest {

    @Test
    void countedResultHandoffServiceKeepsNarrowVocabularyWithoutGenericAssignmentMutationDrift() {
        assertThat(Arrays.stream(AssignmentCountedResultHandoffService.class.getDeclaredMethods())
            .map(Method::getName)
            .toList())
            .containsExactly("acceptValidCountedAssignmentResult")
            .doesNotContain(
                "overrideAssignmentResult",
                "patchAssignmentResult",
                "rebuildAssignmentState",
                "resyncAllAssignmentResults"
            );
    }

    @Test
    void narrowHandoffRemainsTheOnlyAllowedAssignmentSideWriteBridgeForResultPath() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCountedResultHandoffServiceImpl.java"
        ));

        assertThat(source)
            .contains("closeAssignmentTestWithCountedResult")
            .doesNotContain(
                "saveAssignmentTest",
                "saveAssignment(",
                "saveAssignmentCampaign",
                "AssignmentCampaignRepository",
                "AssignmentCampaignCourseRepository",
                "AssignmentCampaignRecipientSnapshotRepository"
            );
    }

    @Test
    void countedResultHandoffImplementationDependsOnlyOnCountedFactsReaderAssignmentTestRepositoryAndAssignmentOwnerClosure() {
        assertThat(fieldTypes(AssignmentCountedResultHandoffServiceImpl.class))
            .containsExactlyInAnyOrder(
                AssignmentStatusDefiningCountedResultFactsReader.class,
                AssignmentTestRepository.class,
                AssignmentCommandService.class
            )
            .doesNotContain(
                AssignmentQueryService.class,
                AssignmentCampaignQueryService.class,
                AssignmentAdministrativeActionService.class,
                AssignmentCampaignCommandService.class,
                CriticalCommandAuditSupport.class,
                AccessSpecificationPolicy.class
            );
    }

    @Test
    void countedResultHandoffDoesNotDependOnAuditPolicyOrApiContours() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCountedResultHandoffServiceImpl.java"
        ));

        assertThat(source).doesNotContain(
            "CriticalCommandAuditSupport",
            "AssignmentCriticalAuditPlanner",
            "AccessSpecificationPolicy",
            "Controller",
            "ResponseEntity",
            "refreshAssignmentStatusCache"
        );
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }
}

