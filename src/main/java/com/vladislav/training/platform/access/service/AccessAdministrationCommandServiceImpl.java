package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code AccessAdministrationCommandServiceImpl}.
 */
@Service
@Transactional
public class AccessAdministrationCommandServiceImpl implements AccessAdministrationCommandService {

    private static final String TARGET_MODULE = "access";

    private static final String ENTITY_TYPE_USER_ACCESS_AREA = "user_access_area";
    private static final String ENTITY_TYPE_MANAGEMENT_RELATION = "management_relation";
    private static final String ENTITY_TYPE_TEMPORARY_ROLE_ASSIGNMENT = "temporary_role_assignment";
    private static final String ENTITY_TYPE_TEMPORARY_ACCESS_AREA = "temporary_access_area";
    private static final String ENTITY_TYPE_TEMPORARY_MANAGEMENT_DELEGATION = "temporary_management_delegation";

    private static final String OPERATION_ASSIGN_ACCESS_AREA = "ACCESS_USER_ACCESS_AREA_ASSIGN";
    private static final String OPERATION_CLOSE_ACCESS_AREA = "ACCESS_USER_ACCESS_AREA_CLOSE";
    private static final String OPERATION_ASSIGN_MANAGEMENT_RELATION = "ACCESS_MANAGEMENT_RELATION_ASSIGN";
    private static final String OPERATION_CLOSE_MANAGEMENT_RELATION = "ACCESS_MANAGEMENT_RELATION_CLOSE";
    private static final String OPERATION_ASSIGN_TEMPORARY_ROLE = "ACCESS_TEMPORARY_ROLE_ASSIGN";
    private static final String OPERATION_CLOSE_TEMPORARY_ROLE = "ACCESS_TEMPORARY_ROLE_CLOSE";
    private static final String OPERATION_ASSIGN_TEMPORARY_ACCESS = "ACCESS_TEMPORARY_ACCESS_ASSIGN";
    private static final String OPERATION_CLOSE_TEMPORARY_ACCESS = "ACCESS_TEMPORARY_ACCESS_CLOSE";
    private static final String OPERATION_ASSIGN_TEMPORARY_MANAGEMENT = "ACCESS_TEMPORARY_MANAGEMENT_ASSIGN";
    private static final String OPERATION_CLOSE_TEMPORARY_MANAGEMENT = "ACCESS_TEMPORARY_MANAGEMENT_CLOSE";

    private final UserAccessAreaCommandService userAccessAreaCommandService;
    private final ManagementRelationCommandService managementRelationCommandService;
    private final TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService;
    private final TemporaryAccessAreaCommandService temporaryAccessAreaCommandService;
    private final TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final UtcClock utcClock;

    public AccessAdministrationCommandServiceImpl(
            UserAccessAreaCommandService userAccessAreaCommandService,
            ManagementRelationCommandService managementRelationCommandService,
            TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService,
            TemporaryAccessAreaCommandService temporaryAccessAreaCommandService,
            TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService,
            CapabilityAdmissionPolicy capabilityAdmissionPolicy,
            CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
            CriticalCommandAuditSupport criticalCommandAuditSupport,
            UtcClock utcClock
    ) {
        this.userAccessAreaCommandService = userAccessAreaCommandService;
        this.managementRelationCommandService = managementRelationCommandService;
        this.temporaryRoleAssignmentCommandService = temporaryRoleAssignmentCommandService;
        this.temporaryAccessAreaCommandService = temporaryAccessAreaCommandService;
        this.temporaryManagementDelegationCommandService = temporaryManagementDelegationCommandService;
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
        this.criticalCommandAuditSupport = criticalCommandAuditSupport;
        this.utcClock = utcClock;
    }

    @Override
    public UserAccessArea assignUserAccessArea(
            Long userId,
            Long organizationalUnitId,
            AccessScopeType accessScopeType,
            Instant validFrom
    ) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_USER_ACCESS_AREA_ASSIGN,
                CapabilityTargetEntityType.USER_ACCESS_AREA,
                null,
                new CapabilityAdmissionPayload.AccessArea(
                        userId,
                        organizationalUnitId,
                        null,
                        accessScopeType,
                        validFrom,
                        null,
                        false
                )
        ));
        Instant now = utcClock.now();
        UserAccessArea createdArea = userAccessAreaCommandService.saveUserAccessArea(new UserAccessArea(
                null,
                userId,
                organizationalUnitId,
                accessScopeType,
                validFrom,
                null,
                now,
                now
        ));
        recordAudit(
                actorUserId,
                new AuditEventType("access.user_access_area.assigned"),
                ENTITY_TYPE_USER_ACCESS_AREA,
                createdArea.id(),
                null,
                createdArea,
                auditContextData(OPERATION_ASSIGN_ACCESS_AREA, details(
                        "userId", userId,
                        "organizationalUnitId", organizationalUnitId,
                        "accessScopeType", accessScopeType,
                        "validFrom", validFrom
                ))
        );
        return createdArea;
    }

    @Override
    public UserAccessArea closeUserAccessArea(Long userAccessAreaId, Instant validTo) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_USER_ACCESS_AREA_CLOSE,
                CapabilityTargetEntityType.USER_ACCESS_AREA,
                userAccessAreaId,
                new CapabilityAdmissionPayload.AccessArea(
                        null,
                        null,
                        userAccessAreaId,
                        null,
                        null,
                        validTo,
                        false
                )
        ));
        UserAccessArea currentArea = userAccessAreaCommandService.findUserAccessAreaById(userAccessAreaId);
        userAccessAreaCommandService.revokeUserAccessArea(userAccessAreaId, validTo);
        UserAccessArea closedArea = userAccessAreaCommandService.findUserAccessAreaById(userAccessAreaId);
        recordAudit(
                actorUserId,
                new AuditEventType("access.user_access_area.closed"),
                ENTITY_TYPE_USER_ACCESS_AREA,
                closedArea.id(),
                currentArea,
                closedArea,
                auditContextData(OPERATION_CLOSE_ACCESS_AREA, details("validTo", validTo))
        );
        return closedArea;
    }

    @Override
    public ManagementRelation assignManagementRelation(
            Long userId,
            Long organizationalUnitId,
            Long managementRelationTypeId,
            Instant validFrom
    ) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_MANAGEMENT_RELATION_ASSIGN,
                CapabilityTargetEntityType.MANAGEMENT_RELATION,
                null,
                new CapabilityAdmissionPayload.ManagementRelation(
                        userId,
                        organizationalUnitId,
                        managementRelationTypeId,
                        null,
                        validFrom,
                        null,
                        false
                )
        ));
        Instant now = utcClock.now();
        ManagementRelation createdRelation = managementRelationCommandService.saveManagementRelation(new ManagementRelation(
                null,
                userId,
                organizationalUnitId,
                managementRelationTypeId,
                validFrom,
                null,
                now,
                now
        ));
        recordAudit(
                actorUserId,
                new AuditEventType("access.management_relation.assigned"),
                ENTITY_TYPE_MANAGEMENT_RELATION,
                createdRelation.id(),
                null,
                createdRelation,
                auditContextData(OPERATION_ASSIGN_MANAGEMENT_RELATION, details(
                        "userId", userId,
                        "organizationalUnitId", organizationalUnitId,
                        "managementRelationTypeId", managementRelationTypeId,
                        "validFrom", validFrom
                ))
        );
        return createdRelation;
    }

    @Override
    public ManagementRelation closeManagementRelation(Long managementRelationId, Instant validTo) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_MANAGEMENT_RELATION_CLOSE,
                CapabilityTargetEntityType.MANAGEMENT_RELATION,
                managementRelationId,
                new CapabilityAdmissionPayload.ManagementRelation(
                        null,
                        null,
                        null,
                        managementRelationId,
                        null,
                        validTo,
                        false
                )
        ));
        ManagementRelation currentRelation = managementRelationCommandService.findManagementRelationById(managementRelationId);
        managementRelationCommandService.endManagementRelation(managementRelationId, validTo);
        ManagementRelation closedRelation = managementRelationCommandService.findManagementRelationById(managementRelationId);
        recordAudit(
                actorUserId,
                new AuditEventType("access.management_relation.closed"),
                ENTITY_TYPE_MANAGEMENT_RELATION,
                closedRelation.id(),
                currentRelation,
                closedRelation,
                auditContextData(OPERATION_CLOSE_MANAGEMENT_RELATION, details("validTo", validTo))
        );
        return closedRelation;
    }

    @Override
    public TemporaryRoleAssignment assignTemporaryRoleAssignment(Long userId, Long roleId, Instant validFrom) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_TEMPORARY_ROLE_ASSIGN,
                CapabilityTargetEntityType.TEMPORARY_ROLE_ASSIGNMENT,
                null,
                new CapabilityAdmissionPayload.RoleAssignment(
                        userId,
                        roleId,
                        null,
                        validFrom,
                        null,
                        true
                )
        ));
        Instant now = utcClock.now();
        TemporaryRoleAssignment createdAssignment = temporaryRoleAssignmentCommandService.saveTemporaryRoleAssignment(
                new TemporaryRoleAssignment(null, userId, roleId, validFrom, null, now, now)
        );
        recordAudit(
                actorUserId,
                new AuditEventType("access.temporary_role_assignment.assigned"),
                ENTITY_TYPE_TEMPORARY_ROLE_ASSIGNMENT,
                createdAssignment.id(),
                null,
                createdAssignment,
                auditContextData(OPERATION_ASSIGN_TEMPORARY_ROLE, details(
                        "userId", userId,
                        "roleId", roleId,
                        "validFrom", validFrom
                ))
        );
        return createdAssignment;
    }

    @Override
    public TemporaryRoleAssignment closeTemporaryRoleAssignment(Long temporaryRoleAssignmentId, Instant validTo) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_TEMPORARY_ROLE_CLOSE,
                CapabilityTargetEntityType.TEMPORARY_ROLE_ASSIGNMENT,
                temporaryRoleAssignmentId,
                new CapabilityAdmissionPayload.RoleAssignment(
                        null,
                        null,
                        temporaryRoleAssignmentId,
                        null,
                        validTo,
                        true
                )
        ));
        TemporaryRoleAssignment currentAssignment = temporaryRoleAssignmentCommandService.findTemporaryRoleAssignmentById(
                temporaryRoleAssignmentId
        );
        temporaryRoleAssignmentCommandService.endTemporaryRoleAssignment(temporaryRoleAssignmentId, validTo);
        TemporaryRoleAssignment closedAssignment = temporaryRoleAssignmentCommandService.findTemporaryRoleAssignmentById(
                temporaryRoleAssignmentId
        );
        recordAudit(
                actorUserId,
                new AuditEventType("access.temporary_role_assignment.closed"),
                ENTITY_TYPE_TEMPORARY_ROLE_ASSIGNMENT,
                closedAssignment.id(),
                currentAssignment,
                closedAssignment,
                auditContextData(OPERATION_CLOSE_TEMPORARY_ROLE, details("validTo", validTo))
        );
        return closedAssignment;
    }

    @Override
    public TemporaryAccessArea assignTemporaryAccessArea(
            Long userId,
            Long organizationalUnitId,
            AccessScopeType accessScopeType,
            Instant validFrom
    ) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_TEMPORARY_ACCESS_ASSIGN,
                CapabilityTargetEntityType.TEMPORARY_ACCESS_AREA,
                null,
                new CapabilityAdmissionPayload.AccessArea(
                        userId,
                        organizationalUnitId,
                        null,
                        accessScopeType,
                        validFrom,
                        null,
                        true
                )
        ));
        Instant now = utcClock.now();
        TemporaryAccessArea createdArea = temporaryAccessAreaCommandService.saveTemporaryAccessArea(new TemporaryAccessArea(
                null,
                userId,
                organizationalUnitId,
                accessScopeType,
                validFrom,
                null,
                now,
                now
        ));
        recordAudit(
                actorUserId,
                new AuditEventType("access.temporary_access_area.assigned"),
                ENTITY_TYPE_TEMPORARY_ACCESS_AREA,
                createdArea.id(),
                null,
                createdArea,
                auditContextData(OPERATION_ASSIGN_TEMPORARY_ACCESS, details(
                        "userId", userId,
                        "organizationalUnitId", organizationalUnitId,
                        "accessScopeType", accessScopeType,
                        "validFrom", validFrom
                ))
        );
        return createdArea;
    }

    @Override
    public TemporaryAccessArea closeTemporaryAccessArea(Long temporaryAccessAreaId, Instant validTo) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_TEMPORARY_ACCESS_CLOSE,
                CapabilityTargetEntityType.TEMPORARY_ACCESS_AREA,
                temporaryAccessAreaId,
                new CapabilityAdmissionPayload.AccessArea(
                        null,
                        null,
                        temporaryAccessAreaId,
                        null,
                        null,
                        validTo,
                        true
                )
        ));
        TemporaryAccessArea currentArea = temporaryAccessAreaCommandService.findTemporaryAccessAreaById(
                temporaryAccessAreaId
        );
        temporaryAccessAreaCommandService.endTemporaryAccessArea(temporaryAccessAreaId, validTo);
        TemporaryAccessArea closedArea = temporaryAccessAreaCommandService.findTemporaryAccessAreaById(temporaryAccessAreaId);
        recordAudit(
                actorUserId,
                new AuditEventType("access.temporary_access_area.closed"),
                ENTITY_TYPE_TEMPORARY_ACCESS_AREA,
                closedArea.id(),
                currentArea,
                closedArea,
                auditContextData(OPERATION_CLOSE_TEMPORARY_ACCESS, details("validTo", validTo))
        );
        return closedArea;
    }

    @Override
    public TemporaryManagementDelegation assignTemporaryManagementDelegation(
            Long userId,
            Long organizationalUnitId,
            Long managementRelationTypeId,
            Instant validFrom
    ) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_TEMPORARY_MANAGEMENT_ASSIGN,
                CapabilityTargetEntityType.TEMPORARY_MANAGEMENT_DELEGATION,
                null,
                new CapabilityAdmissionPayload.ManagementRelation(
                        userId,
                        organizationalUnitId,
                        managementRelationTypeId,
                        null,
                        validFrom,
                        null,
                        true
                )
        ));
        Instant now = utcClock.now();
        TemporaryManagementDelegation createdDelegation = temporaryManagementDelegationCommandService
                .saveTemporaryManagementDelegation(new TemporaryManagementDelegation(
                        null,
                        userId,
                        organizationalUnitId,
                        managementRelationTypeId,
                        validFrom,
                        null,
                        now,
                        now
                ));
        recordAudit(
                actorUserId,
                new AuditEventType("access.temporary_management_delegation.assigned"),
                ENTITY_TYPE_TEMPORARY_MANAGEMENT_DELEGATION,
                createdDelegation.id(),
                null,
                createdDelegation,
                auditContextData(OPERATION_ASSIGN_TEMPORARY_MANAGEMENT, details(
                        "userId", userId,
                        "organizationalUnitId", organizationalUnitId,
                        "managementRelationTypeId", managementRelationTypeId,
                        "validFrom", validFrom
                ))
        );
        return createdDelegation;
    }

    @Override
    public TemporaryManagementDelegation closeTemporaryManagementDelegation(
            Long temporaryManagementDelegationId,
            Instant validTo
    ) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.ACCESS_TEMPORARY_MANAGEMENT_CLOSE,
                CapabilityTargetEntityType.TEMPORARY_MANAGEMENT_DELEGATION,
                temporaryManagementDelegationId,
                new CapabilityAdmissionPayload.ManagementRelation(
                        null,
                        null,
                        null,
                        temporaryManagementDelegationId,
                        null,
                        validTo,
                        true
                )
        ));
        TemporaryManagementDelegation currentDelegation = temporaryManagementDelegationCommandService
                .findTemporaryManagementDelegationById(temporaryManagementDelegationId);
        temporaryManagementDelegationCommandService.endTemporaryManagementDelegation(
                temporaryManagementDelegationId,
                validTo
        );
        TemporaryManagementDelegation closedDelegation = temporaryManagementDelegationCommandService
                .findTemporaryManagementDelegationById(temporaryManagementDelegationId);
        recordAudit(
                actorUserId,
                new AuditEventType("access.temporary_management_delegation.closed"),
                ENTITY_TYPE_TEMPORARY_MANAGEMENT_DELEGATION,
                closedDelegation.id(),
                currentDelegation,
                closedDelegation,
                auditContextData(OPERATION_CLOSE_TEMPORARY_MANAGEMENT, details("validTo", validTo))
        );
        return closedDelegation;
    }

    private Map<String, Object> details(Object... entries) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            details.put((String) entries[index], entries[index + 1]);
        }
        return details;
    }

    private Long resolveActorUserId() {
        return criticalCommandAuditSupport.resolveInteractiveActorUserId();
    }

    private void recordAudit(
            Long actorUserId,
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Object payloadBefore,
            Object payloadAfter,
            AuditContext contextPayload
    ) {
        criticalCommandAuditSupport.recordAudit(
                actorUserId,
                eventType,
                entityType,
                entityId,
                payloadBefore,
                payloadAfter,
                contextPayload
        );
    }

    private AuditContext auditContextData(String operationCode, Map<String, Object> details) {
        return criticalCommandAuditSupport.buildAuditContext(TARGET_MODULE, operationCode, details);
    }
}
