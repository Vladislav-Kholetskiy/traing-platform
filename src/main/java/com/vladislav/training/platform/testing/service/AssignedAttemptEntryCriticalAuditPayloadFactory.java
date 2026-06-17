package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Фабрика {@code AssignedAttemptEntryCriticalAuditPayloadFactory}.
 */
final class AssignedAttemptEntryCriticalAuditPayloadFactory {

    Map<String, Object> createDetails(Long assignmentId, Long assignmentTestId, Long testId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "assigned_attempt_start");
        details.put("entryMode", "create");
        details.put("assignmentId", assignmentId);
        details.put("assignmentTestId", assignmentTestId);
        details.put("testId", testId);
        return details;
    }

    Map<String, Object> payloadAfter(TestAttempt testAttempt, Long assignmentId) {
        Map<String, Object> payloadAfter = new LinkedHashMap<>();
        payloadAfter.put("assignmentId", assignmentId);
        payloadAfter.put("attempt", attemptPayload(testAttempt));
        return payloadAfter;
    }

    private Map<String, Object> attemptPayload(TestAttempt testAttempt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", testAttempt.id());
        payload.put("userId", testAttempt.userId());
        payload.put("testId", testAttempt.testId());
        payload.put("assignmentTestId", testAttempt.assignmentTestId());
        payload.put("attemptMode", testAttempt.attemptMode());
        payload.put("status", testAttempt.status());
        payload.put("startedAt", testAttempt.startedAt());
        payload.put("lastActivityAt", testAttempt.lastActivityAt());
        payload.put("createdAt", testAttempt.createdAt());
        payload.put("updatedAt", testAttempt.updatedAt());
        return payload;
    }
}
