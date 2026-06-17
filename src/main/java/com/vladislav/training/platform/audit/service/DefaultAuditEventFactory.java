package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.domain.AuditPayload;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Фабрика {@code DefaultAuditEventFactory}.
 */
@Component("canonicalAuditEventFactory")
public class DefaultAuditEventFactory implements AuditEventFactory {

    @Override
    public AuditEvent createAuditEvent(
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
        return new AuditEvent(
            null,
            eventType,
            entityType,
            entityId,
            actorUserId,
            occurredAt,
            payloadBefore,
            payloadAfter,
            contextPayload,
            correlationId,
            requestId,
            createdAt
        );
    }
}
