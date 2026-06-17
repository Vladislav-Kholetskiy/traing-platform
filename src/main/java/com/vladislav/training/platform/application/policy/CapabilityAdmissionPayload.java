package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Интерфейс {@code CapabilityAdmissionPayload}.
 */
public sealed interface CapabilityAdmissionPayload permits
    CapabilityAdmissionPayload.Empty,
    CapabilityAdmissionPayload.UserMutation,
    CapabilityAdmissionPayload.RoleAssignment,
    CapabilityAdmissionPayload.OrganizationAssignment,
    CapabilityAdmissionPayload.AccessArea,
    CapabilityAdmissionPayload.ManagementRelation,
    CapabilityAdmissionPayload.OrganizationalUnitTypeMutation,
    CapabilityAdmissionPayload.OrganizationalUnitMutation,
    CapabilityAdmissionPayload.ContentMutation,
    CapabilityAdmissionPayload.TopicFinalControlMutation,
    CapabilityAdmissionPayload.AssignmentCampaignLaunch,
    CapabilityAdmissionPayload.AssignmentCancel,
    CapabilityAdmissionPayload.AssignmentDeadlineExtend,
    CapabilityAdmissionPayload.AssignmentReplaceWithNew,
    CapabilityAdmissionPayload.AssignedExecution,
    CapabilityAdmissionPayload.SelfExecution {

    record Empty() implements CapabilityAdmissionPayload {
        public static final Empty INSTANCE = new Empty();
    }

    record UserMutation(Long userId, String employeeNumber, String externalId, UserStatus status)
        implements CapabilityAdmissionPayload {}

    record RoleAssignment(Long userId, Long roleId, Long assignmentId, Instant validFrom, Instant validTo, boolean temporary)
        implements CapabilityAdmissionPayload {}

    record OrganizationAssignment(
        Long userId,
        Long organizationalUnitId,
        Long assignmentId,
        OrganizationAssignmentType assignmentType,
        Instant validFrom,
        Instant validTo,
        Instant effectiveAt
    ) implements CapabilityAdmissionPayload {}

    record AccessArea(
        Long userId,
        Long organizationalUnitId,
        Long accessAreaId,
        AccessScopeType accessScopeType,
        Instant validFrom,
        Instant validTo,
        boolean temporary
    ) implements CapabilityAdmissionPayload {}

    record ManagementRelation(
        Long userId,
        Long organizationalUnitId,
        Long managementRelationTypeId,
        Long relationId,
        Instant validFrom,
        Instant validTo,
        boolean temporary
    ) implements CapabilityAdmissionPayload {}

    record OrganizationalUnitTypeMutation(Long organizationalUnitTypeId, String code) implements CapabilityAdmissionPayload {}

    record OrganizationalUnitMutation(
        Long organizationalUnitId,
        Long parentUnitId,
        Long newParentUnitId,
        Long organizationalUnitTypeId,
        String externalId,
        OrganizationalUnitStatus status
    ) implements CapabilityAdmissionPayload {}

    record ContentMutation(Long parentEntityId, CapabilityTargetEntityType parentEntityType) implements CapabilityAdmissionPayload {}

    record TopicFinalControlMutation(Long topicId, Long testId) implements CapabilityAdmissionPayload {}

    /**
     * Контекст запуска кампании назначений из внешнего источника или подготовленного набора данных.
     */
    record AssignmentCampaignLaunch(String sourceType, String sourceRef) implements CapabilityAdmissionPayload {
        public AssignmentCampaignLaunch {
            if (sourceType == null || sourceType.isBlank()) {
                throw new IllegalArgumentException("sourceType must not be blank");
            }
        }
    }

    /**
     * Маркер операции отмены назначения без дополнительных данных.
     */
    record AssignmentCancel() implements CapabilityAdmissionPayload {
        public static final AssignmentCancel INSTANCE = new AssignmentCancel();
    }

    /**
     * Контекст переноса срока назначения.
     */
    record AssignmentDeadlineExtend(Instant newDeadlineAt) implements CapabilityAdmissionPayload {
        public AssignmentDeadlineExtend {
            Objects.requireNonNull(newDeadlineAt, "newDeadlineAt must not be null");
        }
    }

    /**
     * Контекст замены назначения новым экземпляром в рамках кампании.
     */
    record AssignmentReplaceWithNew(Long campaignId) implements CapabilityAdmissionPayload {
        public AssignmentReplaceWithNew {
            Objects.requireNonNull(campaignId, "campaignId must not be null");
        }
    }

    /**
     * Контекст выполнения теста, привязанного к назначению.
     */
    record AssignedExecution(Long assignmentId) implements CapabilityAdmissionPayload {
        public AssignedExecution {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        }
    }

    /**
     * Маркер самостоятельного прохождения теста без привязки к назначению.
     */
    record SelfExecution() implements CapabilityAdmissionPayload {
        public static final SelfExecution INSTANCE = new SelfExecution();
    }
}
