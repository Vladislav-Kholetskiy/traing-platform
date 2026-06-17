package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.audit.domain.AuditEventType;

/**
 * Перечисление {@code AttemptTerminalCriticalAuditCatalog}.
 */
enum AttemptTerminalCriticalAuditCatalog {

    ASSIGNED_ATTEMPT_SUBMITTED(
        "TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL",
        "test_attempt",
        "TESTING_ASSIGNED_ATTEMPT_SUBMITTED"
    ),
    ASSIGNED_ATTEMPT_EXPIRED(
        "TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL",
        "test_attempt",
        "TESTING_ASSIGNED_ATTEMPT_EXPIRED"
    ),
    SELF_ATTEMPT_SUBMITTED(
        "TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL",
        "test_attempt",
        "TESTING_SELF_ATTEMPT_SUBMITTED"
    ),
    SELF_ATTEMPT_ABANDONED(
        "TESTING_SELF_ATTEMPT_ABANDON_TERMINAL",
        "test_attempt",
        "TESTING_SELF_ATTEMPT_ABANDONED"
    );

    private final String operationCode;
    private final String auditEntityType;
    private final String auditEventTypeCode;

    AttemptTerminalCriticalAuditCatalog(String operationCode, String auditEntityType, String auditEventTypeCode) {
        this.operationCode = operationCode;
        this.auditEntityType = auditEntityType;
        this.auditEventTypeCode = auditEventTypeCode;
    }

    String operationCode() {
        return operationCode;
    }

    String auditEntityType() {
        return auditEntityType;
    }

    AuditEventType auditEventType() {
        return new AuditEventType(auditEventTypeCode);
    }
}
