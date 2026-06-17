package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;

/**
 * Класс {@code AssignmentCriticalAuditPlannerImpl}.
 */
public final class AssignmentCriticalAuditPlannerImpl implements AssignmentCriticalAuditPlanner {

    @Override
    public LaunchAuditPlan planLaunchAudit(Long assignmentCampaignId) {
        return new LaunchAuditPlan(
            assignmentCampaignId,
            AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH
        );
    }

    @Override
    public AdministrativeAuditPlan planAdministrativeAudit(
        CapabilityOperationCode operationCode,
        Long assignmentId
    ) {
        return new AdministrativeAuditPlan(
            assignmentId,
            AssignmentCriticalAuditCatalog.forOperation(operationCode)
        );
    }
}
