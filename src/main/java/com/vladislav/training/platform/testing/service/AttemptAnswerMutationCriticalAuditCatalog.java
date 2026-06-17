package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.audit.domain.AuditEventType;

/**
 * Перечисление {@code AttemptAnswerMutationCriticalAuditCatalog}.
 */
enum AttemptAnswerMutationCriticalAuditCatalog {

    ASSIGNED_ANSWER_MUTATED(
        CapabilityOperationCode.TESTING_ASSIGNED_ANSWER_MUTATION,
        "test_attempt",
        "TESTING_ASSIGNED_ANSWER_MUTATED"
    ),
    SELF_ANSWER_MUTATED(
        CapabilityOperationCode.TESTING_SELF_ANSWER_MUTATION,
        "test_attempt",
        "TESTING_SELF_ANSWER_MUTATED"
    );

    private final CapabilityOperationCode operationCode;
    private final String auditEntityType;
    private final String auditEventTypeCode;

    AttemptAnswerMutationCriticalAuditCatalog(
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
