package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.audit.domain.AuditEvent;

/**
 * Контракт сервиса {@code AuditService}.
 */
public interface AuditService {

    AuditEvent recordAuditEvent(AuditEvent auditEvent);
}
