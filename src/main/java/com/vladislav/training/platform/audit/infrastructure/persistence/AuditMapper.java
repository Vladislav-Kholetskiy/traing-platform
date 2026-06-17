package com.vladislav.training.platform.audit.infrastructure.persistence;

import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.domain.AuditPayload;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Преобразователь {@code AuditMapper}.
 */
@Component
public class AuditMapper {

    public AuditEvent toDomain(AuditEventEntity entity) {
        return new AuditEvent(
            entity.getId(),
            new AuditEventType(entity.getEventType()),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getActorUserId(),
            entity.getOccurredAt(),
            entity.getPayloadBefore() == null ? null : new AuditPayload(entity.getPayloadBefore()),
            entity.getPayloadAfter() == null ? null : new AuditPayload(entity.getPayloadAfter()),
            entity.getContextPayload() == null ? null : new AuditContext(entity.getContextPayload()),
            entity.getCorrelationId(),
            entity.getRequestId(),
            entity.getCreatedAt()
        );
    }

    public AuditEventEntity toEntity(AuditEvent domain) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setId(domain.id());
        entity.setEventType(domain.eventType().value());
        entity.setEntityType(domain.entityType());
        entity.setEntityId(domain.entityId());
        entity.setActorUserId(domain.actorUserId());
        entity.setOccurredAt(domain.occurredAt());
        entity.setPayloadBefore(domain.payloadBefore() == null ? null : domain.payloadBefore().json());
        entity.setPayloadAfter(domain.payloadAfter() == null ? null : domain.payloadAfter().json());
        entity.setContextPayload(domain.contextPayload() == null ? null : domain.contextPayload().json());
        entity.setCorrelationId(domain.correlationId());
        entity.setRequestId(domain.requestId());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public List<AuditEvent> toDomainEvents(List<AuditEventEntity> entities) {
        return entities.stream().map(this::toDomain).toList();
    }
}
