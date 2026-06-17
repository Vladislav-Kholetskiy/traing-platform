package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Контракт сервиса {@code FallbackInMemoryAuditService}.
 */
public class FallbackInMemoryAuditService implements AuditService {

    private final AtomicLong idSequence = new AtomicLong();
    private final List<AuditEvent> recordedEvents = new CopyOnWriteArrayList<>();

    @Override
    public AuditEvent recordAuditEvent(AuditEvent auditEvent) {
        Objects.requireNonNull(auditEvent, "auditEvent must not be null");
        if (auditEvent.id() != null) {
            throw new IllegalArgumentException("Audit event id must be null for synchronous write-side insert");
        }
        AuditEvent persistedEvent = new AuditEvent(
            idSequence.incrementAndGet(),
            auditEvent.eventType(),
            auditEvent.entityType(),
            auditEvent.entityId(),
            auditEvent.actorUserId(),
            auditEvent.occurredAt(),
            auditEvent.payloadBefore(),
            auditEvent.payloadAfter(),
            auditEvent.contextPayload(),
            auditEvent.correlationId(),
            auditEvent.requestId(),
            auditEvent.createdAt()
        );
        recordedEvents.add(persistedEvent);
        return persistedEvent;
    }

    List<AuditEvent> recordedEventsSnapshot() {
        return List.copyOf(recordedEvents);
    }
}

