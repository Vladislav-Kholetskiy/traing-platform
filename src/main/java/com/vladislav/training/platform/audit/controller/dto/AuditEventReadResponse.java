package com.vladislav.training.platform.audit.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code AuditEventReadResponse}.
 */
public record AuditEventReadResponse(
    Long id,
    String eventType,
    String entityType,
    String entityId,
    Long actorUserId,
    Instant occurredAt,
    String correlationId,
    String requestId,
    Instant createdAt
) {
}
