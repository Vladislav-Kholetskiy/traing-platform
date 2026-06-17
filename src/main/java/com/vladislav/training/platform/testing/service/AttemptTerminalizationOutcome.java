package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AttemptTerminalizationOutcome}.
 */
public record AttemptTerminalizationOutcome(
    Long attemptId,
    Long actorUserId,
    AttemptMode attemptMode,
    TestAttemptStatus terminalStatus,
    Instant terminalizedAt,
    AttemptTerminalizationReason reason,
    boolean resultRecordable,
    AuditEventType auditEventType,
    TestAttempt terminalizedAttempt
) {

    public AttemptTerminalizationOutcome {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(attemptMode, "attemptMode must not be null");
        Objects.requireNonNull(terminalStatus, "terminalStatus must not be null");
        Objects.requireNonNull(terminalizedAt, "terminalizedAt must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(auditEventType, "auditEventType must not be null");
        Objects.requireNonNull(terminalizedAttempt, "terminalizedAttempt must not be null");
    }

    public static AttemptTerminalizationOutcome assignedNormalSubmit(Long actorUserId, TestAttempt terminalizedAttempt) {
        return new AttemptTerminalizationOutcome(
            terminalizedAttempt.id(),
            actorUserId,
            terminalizedAttempt.attemptMode(),
            terminalizedAttempt.status(),
            terminalizedAttempt.completedAt(),
            AttemptTerminalizationReason.NORMAL_SUBMIT,
            true,
            AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_SUBMITTED.auditEventType(),
            terminalizedAttempt
        );
    }

    public static AttemptTerminalizationOutcome selfNormalSubmit(Long actorUserId, TestAttempt terminalizedAttempt) {
        return new AttemptTerminalizationOutcome(
            terminalizedAttempt.id(),
            actorUserId,
            terminalizedAttempt.attemptMode(),
            terminalizedAttempt.status(),
            terminalizedAttempt.completedAt(),
            AttemptTerminalizationReason.NORMAL_SUBMIT,
            true,
            AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_SUBMITTED.auditEventType(),
            terminalizedAttempt
        );
    }

    public static AttemptTerminalizationOutcome expiredByRefresh(Long actorUserId, TestAttempt terminalizedAttempt) {
        return new AttemptTerminalizationOutcome(
            terminalizedAttempt.id(),
            actorUserId,
            terminalizedAttempt.attemptMode(),
            terminalizedAttempt.status(),
            terminalizedAttempt.expiredAt(),
            AttemptTerminalizationReason.EXPIRED_BY_REFRESH,
            false,
            AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType(),
            terminalizedAttempt
        );
    }

    public static AttemptTerminalizationOutcome assignedExplicitExpiry(Long actorUserId, TestAttempt terminalizedAttempt) {
        return new AttemptTerminalizationOutcome(
            terminalizedAttempt.id(),
            actorUserId,
            terminalizedAttempt.attemptMode(),
            terminalizedAttempt.status(),
            terminalizedAttempt.expiredAt(),
            AttemptTerminalizationReason.ASSIGNED_EXPLICIT_EXPIRY,
            false,
            AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType(),
            terminalizedAttempt
        );
    }

    public static AttemptTerminalizationOutcome selfAbandon(Long actorUserId, TestAttempt terminalizedAttempt) {
        return new AttemptTerminalizationOutcome(
            terminalizedAttempt.id(),
            actorUserId,
            terminalizedAttempt.attemptMode(),
            terminalizedAttempt.status(),
            terminalizedAttempt.abandonedAt(),
            AttemptTerminalizationReason.SELF_ABANDON,
            false,
            AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_ABANDONED.auditEventType(),
            terminalizedAttempt
        );
    }
}
