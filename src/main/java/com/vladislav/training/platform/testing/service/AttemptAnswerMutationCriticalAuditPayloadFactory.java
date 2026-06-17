package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Фабрика {@code AttemptAnswerMutationCriticalAuditPayloadFactory}.
 */
final class AttemptAnswerMutationCriticalAuditPayloadFactory {

    Map<String, Object> createAssignedDetails(
        String mutationAction,
        Long assignmentId,
        Long assignmentTestId,
        Long testId,
        Long questionId,
        Long attemptId,
        com.vladislav.training.platform.common.model.AttemptMode attemptMode,
        int answerItemCount
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "assigned_answer_mutation");
        details.put("mutationAction", mutationAction);
        details.put("assignmentId", assignmentId);
        details.put("assignmentTestId", assignmentTestId);
        details.put("testId", testId);
        details.put("questionId", questionId);
        details.put("attemptId", attemptId);
        details.put("attemptMode", attemptMode);
        details.put("answerItemCount", answerItemCount);
        return details;
    }

    Map<String, Object> createSelfDetails(
        String mutationAction,
        Long testId,
        Long questionId,
        Long attemptId,
        com.vladislav.training.platform.common.model.AttemptMode attemptMode,
        int answerItemCount
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", "self_answer_mutation");
        details.put("mutationAction", mutationAction);
        details.put("testId", testId);
        details.put("questionId", questionId);
        details.put("attemptId", attemptId);
        details.put("attemptMode", attemptMode);
        details.put("answerItemCount", answerItemCount);
        return details;
    }

    Map<String, Object> payloadAfter(
        TestAttempt attempt,
        String mutationAction,
        Long questionId,
        Integer answerItemCount,
        Long assignmentId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mutationAction", mutationAction);
        payload.put("questionId", questionId);
        payload.put("answerItemCount", answerItemCount);
        if (assignmentId != null) {
            payload.put("assignmentId", assignmentId);
        }
        payload.put("attempt", attemptPayload(attempt));
        return payload;
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
