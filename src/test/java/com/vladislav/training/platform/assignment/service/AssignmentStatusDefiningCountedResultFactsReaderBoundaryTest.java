package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.repository.ResultRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code AssignmentStatusDefiningCountedResultFactsReader}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class AssignmentStatusDefiningCountedResultFactsReaderBoundaryTest {

    @Test
    void countedResultFactsReaderContractRemainsNarrowAndOwnerScoped() {
        assertThat(Stream.of(AssignmentStatusDefiningCountedResultFactsReader.class.getDeclaredMethods())
            .map(Method::getName))
            .containsExactlyInAnyOrder("findStatusDefiningFactsByCountedResultId", "findCountedAssignmentResultHandoffFactsByResultId")
            .doesNotContain("findResultById", "findResultsByAssignmentId", "findResultQuestionSnapshotsByResultId", "reportResults");

        assertThat(Arrays.stream(AssignmentStatusDefiningCountedResultFactsReader.StatusDefiningCountedResultFacts.class.getRecordComponents())
            .map(component -> component.getName()))
            .containsExactly("passed", "withinDeadline", "countedInAssignment");
        assertThat(Arrays.stream(AssignmentStatusDefiningCountedResultFactsReader.CountedAssignmentResultHandoffFacts.class.getRecordComponents())
            .map(component -> component.getName()))
            .containsExactly(
                "resultId",
                "assignmentId",
                "assignmentTestId",
                "attemptMode",
                "passed",
                "withinDeadline",
                "countedInAssignment",
                "snapshotFinalTopicControlFlag",
                "recordedAt"
            );
        assertThat(AttemptMode.valueOf("ASSIGNED")).isEqualTo(AttemptMode.ASSIGNED);
    }

    @Test
    void statusRecalculationImplementationIsNotYetWidenedIntoResultFacadeOrAttemptStatusShortcut() {
        assertThat(Stream.of(AssignmentStatusRecalculationServiceImpl.class.getDeclaredFields())
            .map(Field::getType))
            .doesNotContain(ResultRepository.class);

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationServiceImpl.java"))
            .contains("counted-result proof for COMPLETED")
            .doesNotContain("TestAttempt.status")
            .doesNotContain("AssignmentCountedResultHandoffService");
    }

    private String read(String relativePath) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(relativePath));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read file: " + relativePath, exception);
        }
    }
}
