package com.vladislav.training.platform.assignment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader.CountedAssignmentResultHandoffFacts;
import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader.StatusDefiningCountedResultFacts;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import com.vladislav.training.platform.result.repository.ResultRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет поведение {@code AssignmentStatusDefiningCountedResultFactsReaderAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentStatusDefiningCountedResultFactsReaderAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-11T10:00:00Z");

    @Mock
    private ResultRepository resultRepository;

    @Test
    void adapterReadsOnlyStatusDefiningFactsByCountedResultId() {
        ResultBackedAssignmentStatusDefiningCountedResultFactsReader adapter =
            new ResultBackedAssignmentStatusDefiningCountedResultFactsReader(resultRepository);
        when(resultRepository.findResultById(9001L)).thenReturn(assignedResult(9001L, true, true, true));

        StatusDefiningCountedResultFacts facts = adapter.findStatusDefiningFactsByCountedResultId(9001L);

        assertThat(facts.passed()).isTrue();
        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.countedInAssignment()).isTrue();
        verify(resultRepository).findResultById(9001L);
    }

    @Test
    void adapterReadsOnlyNarrowHandoffFactsByResultId() {
        ResultBackedAssignmentStatusDefiningCountedResultFactsReader adapter =
            new ResultBackedAssignmentStatusDefiningCountedResultFactsReader(resultRepository);
        when(resultRepository.findResultById(9002L)).thenReturn(assignedResult(9002L, true, true, true));

        CountedAssignmentResultHandoffFacts facts = adapter.findCountedAssignmentResultHandoffFactsByResultId(9002L);

        assertThat(facts.resultId()).isEqualTo(9002L);
        assertThat(facts.assignmentId()).isEqualTo(41L);
        assertThat(facts.assignmentTestId()).isEqualTo(51L);
        assertThat(facts.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(facts.passed()).isTrue();
        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.countedInAssignment()).isTrue();
        assertThat(facts.snapshotFinalTopicControlFlag()).isTrue();
        assertThat(facts.recordedAt()).isEqualTo(FIXED_INSTANT);
        verify(resultRepository).findResultById(9002L);
    }

    @Test
    void adapterFailsWhenCountedResultIdDoesNotResolve() {
        ResultBackedAssignmentStatusDefiningCountedResultFactsReader adapter =
            new ResultBackedAssignmentStatusDefiningCountedResultFactsReader(resultRepository);
        when(resultRepository.findResultById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> adapter.findStatusDefiningFactsByCountedResultId(9999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("9999");
    }

    @Test
    void adapterShapeStaysRepositoryScopedConditionalAndDoesNotWidenIntoBroadFacade() {
        assertThat(ResultBackedAssignmentStatusDefiningCountedResultFactsReader.class.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(ResultBackedAssignmentStatusDefiningCountedResultFactsReader.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(ResultBackedAssignmentStatusDefiningCountedResultFactsReader.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(ResultBackedAssignmentStatusDefiningCountedResultFactsReader.class.isAnnotationPresent(ConditionalOnBean.class)).isTrue();
        assertThat(fieldTypes(ResultBackedAssignmentStatusDefiningCountedResultFactsReader.class))
            .containsExactly(ResultRepository.class);
        assertThat(methodNames(ResultBackedAssignmentStatusDefiningCountedResultFactsReader.class))
            .containsExactlyInAnyOrder("findStatusDefiningFactsByCountedResultId", "findCountedAssignmentResultHandoffFactsByResultId")
            .doesNotContain("findResultsByAssignmentId", "findResultQuestionSnapshotsByResultId", "findResultByTestAttemptId");
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods()).map(Method::getName).collect(Collectors.toUnmodifiableSet());
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields())
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Result assignedResult(
        Long resultId,
        boolean passed,
        boolean withinDeadline,
        boolean countedInAssignment
    ) {
        return new Result(
            resultId,
            7001L,
            3001L,
            AttemptMode.ASSIGNED,
            41L,
            51L,
            61L,
            "Final Control",
            new ResultScoringSnapshot(
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(85),
                passed,
                "FINAL_CONTROL",
                "final control snapshot"
            ),
            withinDeadline,
            countedInAssignment,
            FIXED_INSTANT,
            new ResultOrgContextSnapshot(
                301L,
                "/company/ops"
            ),
            true,
            FIXED_INSTANT
        );
    }
}
