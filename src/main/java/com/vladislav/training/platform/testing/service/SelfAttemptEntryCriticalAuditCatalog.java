package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.audit.domain.AuditEventType;

/**
 * Перечисление {@code SelfAttemptEntryCriticalAuditCatalog}.
 */
enum SelfAttemptEntryCriticalAuditCatalog {

    SELF_ATTEMPT_STARTED(
        CapabilityOperationCode.TESTING_SELF_ATTEMPT_START,
        "test_attempt",
        "TESTING_SELF_ATTEMPT_STARTED"
    );

    private final CapabilityOperationCode operationCode;
    private final String auditEntityType;
    private final String auditEventTypeCode;

    SelfAttemptEntryCriticalAuditCatalog(
        CapabilityOperationCode operationCode,
        String auditEntityType,
        String auditEventTypeCode
    ) {
        this.operationCode = operationCode;
        this.auditEntityType = auditEntityType;
        this.auditEventTypeCode = auditEventTypeCode;
    }

    CapabilityOperationCode operationCode() {
        return operationCode;
    }

    String auditEntityType() {
        return auditEntityType;
    }

    AuditEventType auditEventType() {
        return new AuditEventType(auditEventTypeCode);
    }
}
