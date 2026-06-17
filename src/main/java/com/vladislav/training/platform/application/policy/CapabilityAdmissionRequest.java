package com.vladislav.training.platform.application.policy;

import java.time.Instant;

/**
 * Запрос {@code CapabilityAdmissionRequest}.
 */
public record CapabilityAdmissionRequest(
    Long actorUserId,
    String operationCode,
    CapabilityTargetEntityType targetEntityType,
    Long targetEntityId,
    CapabilityAdmissionPayload payloadContext,
    Instant requestedAt
) {

    public CapabilityAdmissionRequest {
        if (actorUserId == null) {
            throw new IllegalArgumentException("actorUserId must not be null");
        }
        if (operationCode == null || operationCode.isBlank()) {
            throw new IllegalArgumentException("operationCode must not be blank");
        }
        if (!CapabilityOperationCode.isKnown(operationCode)) {
            throw new IllegalArgumentException("operationCode must be one of canonical CapabilityOperationCode values");
        }
        if (targetEntityType == null) {
            throw new IllegalArgumentException("targetEntityType must not be null");
        }
        if (payloadContext == null) {
            throw new IllegalArgumentException("payloadContext must not be null");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null");
        }
    }
}
