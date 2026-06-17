package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.repository.AuditEventRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code AuditQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
class AuditQueryServiceImpl implements AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    AuditQueryServiceImpl(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = Objects.requireNonNull(auditEventRepository, "auditEventRepository must not be null");
    }

    @Override
    public AuditEvent findAuditEventById(Long auditEventId) {
        return auditEventRepository.findAuditEventById(auditEventId);
    }

    @Override
    public List<AuditEvent> findAllAuditEvents() {
        return auditEventRepository.findAllAuditEvents();
    }

    @Override
    public List<AuditEvent> findAuditEventsByEventType(AuditEventType eventType) {
        return auditEventRepository.findAuditEventsByEventType(eventType);
    }

    @Override
    public List<AuditEvent> findAuditEventsByEntity(String entityType, String entityId) {
        return auditEventRepository.findAuditEventsByEntity(entityType, entityId);
    }

    @Override
    public List<AuditEvent> findAuditEventsByActorUserId(Long actorUserId) {
        return auditEventRepository.findAuditEventsByActorUserId(actorUserId);
    }
}
