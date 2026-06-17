package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import java.util.Objects;

/**
 * Интерфейс {@code AssignmentCriticalAuditPlanner}.
 */
public interface AssignmentCriticalAuditPlanner {

    /*
     * Contract markers: mandatory synchronous audit companion, launch-side and typed administrative companion embedding,
     * does not reinterpret assignment_administrative_action, same command boundary.
     */

    LaunchAuditPlan planLaunchAudit(Long assignmentCampaignId);

    AdministrativeAuditPlan planAdministrativeAudit(CapabilityOperationCode operationCode, Long assignmentId);

    /**
     * Общий контракт плана аудита для критичной команды.
     */
    interface AuditPlan {

        AssignmentCriticalAuditCatalog auditCatalog();

        String auditEntityType();

        Long auditEntityId();

        default boolean requiresSynchronousCommandBoundary() {
            return true;
        }

        default boolean allowsAsyncBestEffortTail() {
            return false;
        }

        default boolean runtimeEmbedded() {
            return false;
        }

        default boolean substitutesOwnerHistory() {
            return false;
        }

        default String auditFoundationType() {
            return CriticalCommandAuditSupport.class.getSimpleName();
        }
    }

    /**
     * План аудита для запуска кампании.
     */
    record LaunchAuditPlan(
        Long assignmentCampaignId,
        AssignmentCriticalAuditCatalog auditCatalog
    ) implements AuditPlan {

        public LaunchAuditPlan {
            Objects.requireNonNull(assignmentCampaignId, "assignmentCampaignId must not be null");
            Objects.requireNonNull(auditCatalog, "auditCatalog must not be null");
            if (auditCatalog != AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH) {
                throw new IllegalArgumentException("Для запуска кампании нужен тип аудита CAMPAIGN_LAUNCH");
            }
        }

        @Override
        public String auditEntityType() {
            return "assignment_campaign";
        }

        @Override
        public Long auditEntityId() {
            return assignmentCampaignId;
        }
    }

    /**
     * План аудита для административных действий над назначением.
     */
    record AdministrativeAuditPlan(
        Long assignmentId,
        AssignmentCriticalAuditCatalog auditCatalog
    ) implements AuditPlan {

        public AdministrativeAuditPlan {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
            Objects.requireNonNull(auditCatalog, "auditCatalog must not be null");
            if (!auditCatalog.anchorsAssignmentRoot()) {
                throw new IllegalArgumentException("Административный аудит должен быть привязан к назначению");
            }
            if (auditCatalog == AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH) {
                throw new IllegalArgumentException("Administrative audit companion cannot use launch audit catalog");
            }
        }

        @Override
        public String auditEntityType() {
            return "assignment";
        }

        @Override
        public Long auditEntityId() {
            return assignmentId;
        }
    }
}
