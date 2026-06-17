package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Фабрика {@code AttemptTerminalCriticalAuditPayloadFactory}.
 */
final class AttemptTerminalCriticalAuditPayloadFactory {

    Map<String, Object> createAssignedSubmitDetails(Long assignmentTestId, Long testId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "assigned_attempt_submit");
        details.put("terminalType", "completed");
        details.put("assignmentTestId", assignmentTestId);
        details.put("testId", testId);
        details.put("actorSource", "interactive");
        return details;
    }

    Map<String, Object> createAssignedExpiryDetails(Long assignmentTestId, Long testId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "assigned_attempt_expire");
        details.put("terminalType", "expired");
        details.put("assignmentTestId", assignmentTestId);
        details.put("testId", testId);
        details.put("actorSource", "system");
        return details;
    }

    Map<String, Object> createAssignedSubmitExpiredDetails(Long assignmentTestId, Long testId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "assigned_attempt_submit");
        details.put("terminalType", "expired");
        details.put("assignmentTestId", assignmentTestId);
        details.put("testId", testId);
        details.put("actorSource", "interactive");
        return details;
    }

    Map<String, Object> createSelfSubmitDetails(Long testId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "self_attempt_submit");
        details.put("terminalType", "completed");
        details.put("testId", testId);
        details.put("actorSource", "interactive");
        return details;
    }

    Map<String, Object> createSelfAbandonDetails(Long testId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "self_attempt_abandon");
        details.put("terminalType", "abandoned");
        details.put("testId", testId);
        details.put("actorSource", "interactive");
        return details;
    }

    Map<String, Object> payloadBefore(TestAttempt attempt) {
        return attemptPayload(attempt);
    }

    Map<String, Object> payloadAfter(TestAttempt attempt) {
        return attemptPayload(attempt);
    }

    private Map<String, Object> attemptPayload(TestAttempt attempt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", attempt.id());
        payload.put("userId", attempt.userId());
        payload.put("testId", attempt.testId());
        payload.put("assignmentTestId", attempt.assignmentTestId());
        payload.put("attemptMode", attempt.attemptMode());
        payload.put("status", attempt.status());
        payload.put("startedAt", attempt.startedAt());
        payload.put("completedAt", attempt.completedAt());
        payload.put("expiredAt", attempt.expiredAt());
        payload.put("abandonedAt", attempt.abandonedAt());
        payload.put("lastActivityAt", attempt.lastActivityAt());
        payload.put("createdAt", attempt.createdAt());
        payload.put("updatedAt", attempt.updatedAt());
        return payload;
    }
}
