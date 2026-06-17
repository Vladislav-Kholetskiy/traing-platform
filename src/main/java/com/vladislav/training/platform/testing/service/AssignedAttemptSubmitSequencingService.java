package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code AssignedAttemptSubmitSequencingService}.
 */
@Service
@Transactional
public class AssignedAttemptSubmitSequencingService {

    private final AssignedAttemptSubmitTerminalService assignedAttemptSubmitTerminalService;
    private final ResultRecordingService resultRecordingService;

    public AssignedAttemptSubmitSequencingService(
        AssignedAttemptSubmitTerminalService assignedAttemptSubmitTerminalService,
        ResultRecordingService resultRecordingService
    ) {
        this.assignedAttemptSubmitTerminalService = Objects.requireNonNull(
            assignedAttemptSubmitTerminalService,
            "assignedAttemptSubmitTerminalService must not be null"
        );
        this.resultRecordingService = Objects.requireNonNull(
            resultRecordingService,
            "resultRecordingService must not be null"
        );
    }

    public AssignedAttemptSubmitOutcome submitAssignedAttempt(Long actorUserId, Long assignmentTestId, Long testAttemptId) {
        AttemptTerminalizationOutcome terminalizationOutcome = assignedAttemptSubmitTerminalService.submitAssignedAttempt(
            actorUserId,
            assignmentTestId,
            testAttemptId
        );
        if (!terminalizationOutcome.resultRecordable()) {
            if (terminalizationOutcome.terminalStatus() == TestAttemptStatus.EXPIRED) {
                return AssignedAttemptSubmitOutcome.expired(terminalizationOutcome.attemptId());
            }
            throw new ConflictException(
                "Assigned submit sequencing encountered non-recordable terminalization outcome: attemptId="
                    + terminalizationOutcome.attemptId()
                    + ", status="
                    + terminalizationOutcome.terminalStatus()
                    + ", reason="
                    + terminalizationOutcome.reason()
            );
        }
        if (terminalizationOutcome.terminalStatus() != TestAttemptStatus.COMPLETED) {
            throw new ConflictException(
                "Assigned submit sequencing encountered unsupported terminal status: attemptId="
                    + terminalizationOutcome.attemptId()
                    + ", status="
                    + terminalizationOutcome.terminalStatus()
            );
        }
        Long recordedResult = resultRecordingService.recordResult(terminalizationOutcome.attemptId());
        return AssignedAttemptSubmitOutcome.completed(terminalizationOutcome.attemptId(), recordedResult);
    }

    public record AssignedAttemptSubmitOutcome(
        Long attemptId,
        TestAttemptStatus terminalStatus,
        Long recordedResult
    ) {

        public AssignedAttemptSubmitOutcome {
            Objects.requireNonNull(attemptId, "attemptId must not be null");
            Objects.requireNonNull(terminalStatus, "terminalStatus must not be null");
            if (terminalStatus == TestAttemptStatus.COMPLETED) {
                if (recordedResult == null) {
                    throw new IllegalArgumentException("completed submit outcome requires recordedResult");
                }
            } else if (terminalStatus == TestAttemptStatus.EXPIRED) {
                if (recordedResult != null) {
                    throw new IllegalArgumentException("expired submit outcome must not carry recordedResult");
                }
            } else {
                throw new IllegalArgumentException(
                    "assigned submit outcome supports only COMPLETED with recordedResult or EXPIRED without recordedResult"
                );
            }
        }

        public static AssignedAttemptSubmitOutcome completed(Long attemptId, Long recordedResult) {
            return new AssignedAttemptSubmitOutcome(attemptId, TestAttemptStatus.COMPLETED, recordedResult);
        }

        public static AssignedAttemptSubmitOutcome expired(Long attemptId) {
            return new AssignedAttemptSubmitOutcome(attemptId, TestAttemptStatus.EXPIRED, null);
        }

        public boolean expiredWithoutResult() {
            return terminalStatus == TestAttemptStatus.EXPIRED && recordedResult == null;
        }

        public boolean completedWithResult() {
            return terminalStatus == TestAttemptStatus.COMPLETED && recordedResult != null;
        }
    }
}
