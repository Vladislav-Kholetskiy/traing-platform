package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Фабрика {@code ResultRecordingCriticalAuditPayloadFactory}.
 */
final class ResultRecordingCriticalAuditPayloadFactory {

    Map<String, Object> createRecordingDetails(Result result, TestAttemptStatus terminalStatus, String actorSource) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "result_record");
        details.put("recordingKind", "immutable_result_materialization");
        details.put("testAttemptId", result.testAttemptId());
        details.put("attemptMode", result.attemptMode());
        details.put("terminalStatus", terminalStatus);
        details.put("actorSource", actorSource);
        if (result.assignmentId() != null) {
            details.put("assignmentId", result.assignmentId());
        }
        if (result.assignmentTestId() != null) {
            details.put("assignmentTestId", result.assignmentTestId());
        }
        return details;
    }

    Map<String, Object> payloadAfter(Result result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", result.id());
        payload.put("testAttemptId", result.testAttemptId());
        payload.put("attemptMode", result.attemptMode());
        payload.put("assignmentId", result.assignmentId());
        payload.put("assignmentTestId", result.assignmentTestId());
        payload.put("scoringSnapshot", result.scoringSnapshot());
        payload.put("withinDeadline", result.withinDeadline());
        payload.put("countedInAssignment", result.countedInAssignment());
        payload.put("completedAt", result.completedAt());
        payload.put("orgContextSnapshot", result.orgContextSnapshot());
        payload.put("snapshotFinalTopicControlFlag", result.snapshotFinalTopicControlFlag());
        payload.put("createdAt", result.createdAt());
        return payload;
    }
}
