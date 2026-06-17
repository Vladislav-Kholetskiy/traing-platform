package com.vladislav.training.platform.assignment.infrastructure.scheduler;

import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationBatchService;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик {@code AssignmentStatusRecalculationScheduler}.
 */
@Component
@ConditionalOnProperty(name = "assignment.scheduler.enabled", havingValue = "true")
public class AssignmentStatusRecalculationScheduler {

    private final AssignmentStatusRecalculationBatchService batchService;
    private final UtcClock utcClock;
    private final boolean activeSchedulingEnabled;

    public AssignmentStatusRecalculationScheduler(
        AssignmentStatusRecalculationBatchService batchService,
        UtcClock utcClock,
        @Value("${assignment.scheduler.enabled:false}") boolean activeSchedulingEnabled
    ) {
        this.batchService = Objects.requireNonNull(batchService, "batchService must not be null");
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
        this.activeSchedulingEnabled = activeSchedulingEnabled;
    }

    public SchedulerState describeScheduler() {
        return new SchedulerState(
            true,
            activeSchedulingEnabled,
            true,
            false,
            0,
            0,
            "Assignment scheduler performs bounded owner-local status recalculation without taking over lifecycle ownership"
        );
    }

    public SchedulerState triggerRecalculation() {
        return executePass(utcClock.now());
    }

    @Scheduled(cron = "${assignment.scheduler.status-recalculation-cron:0 */15 * * * *}", zone = "UTC")
    void recalculateAssignmentStatuses() {
        executePass(utcClock.now());
    }

    private SchedulerState executePass(Instant effectiveAt) {
        AssignmentStatusRecalculationBatchService.BatchResult result = batchService.recalculateDueAssignments(effectiveAt);
        return new SchedulerState(
            true,
            activeSchedulingEnabled,
            true,
            result.refreshedCount() > 0,
            result.candidateCount(),
            result.refreshedCount(),
            "Assignment scheduler processed bounded status recalculation window"
        );
    }

    /**
     * Краткое состояние планировщика и последнего запуска пересчёта.
     */
    public record SchedulerState(
        boolean runtimeMaterialized,
        boolean activeSchedulingEnabled,
        boolean ownerMechanicServicePresent,
        boolean executionPerformed,
        int candidateCount,
        int refreshedCount,
        String description
    ) {
    }
}
