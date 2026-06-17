package com.vladislav.training.platform.audit.repository;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import java.util.List;

/**
 * Контракт репозитория {@code AuditEventRepository}.
 */
public interface AuditEventRepository {

    AuditEvent findAuditEventById(Long auditEventId);

    List<AuditEvent> findAllAuditEvents();

    List<AuditEvent> findAuditEventsByEventType(AuditEventType eventType);

    List<AuditEvent> findAuditEventsByEntity(String entityType, String entityId);

    List<AuditEvent> findAuditEventsByActorUserId(Long actorUserId);

    AuditEvent saveAuditEvent(AuditEvent auditEvent);
}
