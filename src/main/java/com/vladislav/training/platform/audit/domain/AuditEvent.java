package com.vladislav.training.platform.audit.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AuditEvent}.
 */
public record AuditEvent(
    Long id,
    AuditEventType eventType,
    String entityType,
    String entityId,
    Long actorUserId,
    Instant occurredAt,
    AuditPayload payloadBefore,
    AuditPayload payloadAfter,
    AuditContext contextPayload,
    String correlationId,
    String requestId,
    Instant createdAt
) {

    public AuditEvent {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be blank");
        }
        if (entityId.isBlank()) {
            throw new IllegalArgumentException("entityId must not be blank");
        }
    }
}
