package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import java.util.List;

/**
 * Контракт сервиса чтения {@code AuditQueryService}.
 */
public interface AuditQueryService {

    AuditEvent findAuditEventById(Long auditEventId);

    List<AuditEvent> findAllAuditEvents();

    List<AuditEvent> findAuditEventsByEventType(AuditEventType eventType);

    List<AuditEvent> findAuditEventsByEntity(String entityType, String entityId);

    List<AuditEvent> findAuditEventsByActorUserId(Long actorUserId);
}
