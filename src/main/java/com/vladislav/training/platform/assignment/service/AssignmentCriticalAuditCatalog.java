package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Перечисление {@code AssignmentCriticalAuditCatalog}.
 */
public enum AssignmentCriticalAuditCatalog {

    CAMPAIGN_LAUNCH(
        CapabilityOperationCode.ASSIGNMENT_CAMPAIGN_LAUNCH,
        "assignment_campaign",
        "ASSIGNMENT_CAMPAIGN_LAUNCHED"
    ),
    ASSIGNMENT_CANCEL(
        CapabilityOperationCode.ASSIGNMENT_CANCEL,
        "assignment",
        "ASSIGNMENT_CANCELLED"
    ),
    ASSIGNMENT_DEADLINE_EXTEND(
        CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND,
        "assignment",
        "ASSIGNMENT_DEADLINE_EXTENDED"
    ),
    ASSIGNMENT_REPLACE_WITH_NEW(
        CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW,
        "assignment",
        "ASSIGNMENT_REPLACED_WITH_NEW"
    );

    private static final Map<CapabilityOperationCode, AssignmentCriticalAuditCatalog> BY_OPERATION =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
            AssignmentCriticalAuditCatalog::operationCode,
            Function.identity()
        ));

    private final CapabilityOperationCode operationCode;
    private final String auditEntityType;
    private final String auditEventTypeCode;

    AssignmentCriticalAuditCatalog(
        CapabilityOperationCode operationCode,
        String auditEntityType,
        String auditEventTypeCode
    ) {
        this.operationCode = operationCode;
        this.auditEntityType = auditEntityType;
        this.auditEventTypeCode = auditEventTypeCode;
    }

    public CapabilityOperationCode operationCode() {
        return operationCode;
    }

    public String auditEntityType() {
        return auditEntityType;
    }

    public AuditEventType auditEventType() {
        return new AuditEventType(auditEventTypeCode);
    }

    public boolean requiresSynchronousCompanion() {
        return true;
    }

    public boolean anchorsCampaignRoot() {
        return "assignment_campaign".equals(auditEntityType);
    }

    public boolean anchorsAssignmentRoot() {
        return "assignment".equals(auditEntityType);
    }

    public static boolean isTrackedCriticalOperation(CapabilityOperationCode operationCode) {
        return BY_OPERATION.containsKey(operationCode);
    }

    public static AssignmentCriticalAuditCatalog forOperation(CapabilityOperationCode operationCode) {
        AssignmentCriticalAuditCatalog catalogEntry = BY_OPERATION.get(operationCode);
        if (catalogEntry == null) {
            throw new IllegalArgumentException("Unsupported assignment contour critical audit operation: " + operationCode);
        }
        return catalogEntry;
    }
}
