package com.vladislav.training.platform.assignment.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationBatchService;
import com.vladislav.training.platform.common.time.UtcClock;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
/**
 * Проверяет поведение {@code AssignmentStatusRecalculationScheduler}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentStatusRecalculationSchedulerTest {

    @Test
    void describeSchedulerReportsRuntimeMaterializedOptInScheduler() {
        AssignmentStatusRecalculationBatchService batchService = mock(AssignmentStatusRecalculationBatchService.class);
        UtcClock utcClock = mock(UtcClock.class);
        AssignmentStatusRecalculationScheduler scheduler = new AssignmentStatusRecalculationScheduler(
            batchService,
            utcClock,
            true
        );

        AssignmentStatusRecalculationScheduler.SchedulerState state = scheduler.describeScheduler();

        assertThat(state.runtimeMaterialized()).isTrue();
        assertThat(state.activeSchedulingEnabled()).isTrue();
        assertThat(state.ownerMechanicServicePresent()).isTrue();
        assertThat(state.executionPerformed()).isFalse();
        assertThat(state.description()).isNotBlank();
        assertThat(Stream.of(AssignmentStatusRecalculationScheduler.class.getDeclaredMethods())
            .anyMatch(this::hasScheduledAnnotation)).isTrue();
    }

    @Test
    void triggerRecalculationDelegatesToBoundedBatchPass() {
        AssignmentStatusRecalculationBatchService batchService = mock(AssignmentStatusRecalculationBatchService.class);
        UtcClock utcClock = mock(UtcClock.class);
        Instant fixedNow = Instant.parse("2026-06-07T10:15:00Z");
        when(utcClock.now()).thenReturn(fixedNow);
        when(batchService.recalculateDueAssignments(fixedNow))
            .thenReturn(new AssignmentStatusRecalculationBatchService.BatchResult(
                fixedNow.minusSeconds(86400),
                fixedNow,
                3,
                3
            ));
        AssignmentStatusRecalculationScheduler scheduler = new AssignmentStatusRecalculationScheduler(
            batchService,
            utcClock,
            true
        );

        AssignmentStatusRecalculationScheduler.SchedulerState state = scheduler.triggerRecalculation();

        assertThat(state.executionPerformed()).isTrue();
        assertThat(state.candidateCount()).isEqualTo(3);
        assertThat(state.refreshedCount()).isEqualTo(3);
        verify(batchService).recalculateDueAssignments(fixedNow);
    }

    private boolean hasScheduledAnnotation(Method method) {
        return method.isAnnotationPresent(Scheduled.class);
    }
}
