package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.repository.AuditEventRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code PersistentAuditService}.
 */
@Service("canonicalAuditService")
@Transactional(propagation = Propagation.MANDATORY)
public class PersistentAuditService implements AuditService {

    private final AuditEventRepository auditEventRepository;

    public PersistentAuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    public AuditEvent recordAuditEvent(AuditEvent auditEvent) {
        Objects.requireNonNull(auditEvent, "auditEvent must not be null");
        if (auditEvent.id() != null) {
            throw new IllegalArgumentException("Audit event id must be null for synchronous write-side insert");
        }
        return auditEventRepository.saveAuditEvent(auditEvent);
    }
}
