package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.audit.domain.AuditEventType;
/**
 * Перечисление {@code ResultRecordingCriticalAuditCatalog}.
 */
enum ResultRecordingCriticalAuditCatalog {

    RESULT_RECORDED(
        "RESULT_RECORDING",
        "result",
        "RESULT_RECORDED"
    );

    private final String operationCode;
    private final String auditEntityType;
    private final String auditEventTypeCode;

    ResultRecordingCriticalAuditCatalog(String operationCode, String auditEntityType, String auditEventTypeCode) {
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
