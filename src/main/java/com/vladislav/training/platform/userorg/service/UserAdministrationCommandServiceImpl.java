package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.ManagementRelationCommandService;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaCommandService;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationCommandService;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentCommandService;
import com.vladislav.training.platform.access.service.UserAccessAreaCommandService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code UserAdministrationCommandServiceImpl}.
 */
@Service
@Transactional
public class UserAdministrationCommandServiceImpl implements UserAdministrationCommandService {

    private static final String TARGET_MODULE = "userorg";
    private static final String ENTITY_TYPE_APP_USER = "app_user";
    private static final String ENTITY_TYPE_USER_ROLE_ASSIGNMENT = "user_role_assignment";
    private static final String ENTITY_TYPE_USER_ORGANIZATION_ASSIGNMENT = "user_organization_assignment";

    private static final String OPERATION_CREATE_USER = "USERORG_USER_CREATE";
    private static final String OPERATION_UPDATE_USER = "USERORG_USER_UPDATE";
    private static final String OPERATION_DEACTIVATE_USER = "USERORG_USER_DEACTIVATE";
    private static final String OPERATION_ASSIGN_ROLE = "USERORG_USER_ROLE_ASSIGN";
    private static final String OPERATION_CLOSE_ROLE = "USERORG_USER_ROLE_CLOSE";
    private static final String OPERATION_ASSIGN_ORGANIZATION = "USERORG_USER_ORGANIZATION_ASSIGN";
    private static final String OPERATION_CLOSE_ORGANIZATION = "USERORG_USER_ORGANIZATION_CLOSE";
    private static final String OPERATION_REPLACE_PRIMARY_HOME = "USERORG_USER_PRIMARY_HOME_REPLACE";

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final UserOrganizationAssignmentService userOrganizationAssignmentService;
    private final UserAccessAreaCommandService userAccessAreaCommandService;
    private final ManagementRelationCommandService managementRelationCommandService;
    private final TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService;
    private final TemporaryAccessAreaCommandService temporaryAccessAreaCommandService;
    private final TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final UtcClock utcClock;

    public UserAdministrationCommandServiceImpl(
            UserCommandService userCommandService,
            UserQueryService userQueryService,
            CapabilityAdmissionPolicy capabilityAdmissionPolicy,
            CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
            UserRoleAssignmentService userRoleAssignmentService,
            UserOrganizationAssignmentService userOrganizationAssignmentService,
            UserAccessAreaCommandService userAccessAreaCommandService,
            ManagementRelationCommandService managementRelationCommandService,
            TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService,
            TemporaryAccessAreaCommandService temporaryAccessAreaCommandService,
            TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService,
            CriticalCommandAuditSupport criticalCommandAuditSupport,
            UtcClock utcClock
    ) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.userOrganizationAssignmentService = userOrganizationAssignmentService;
        this.userAccessAreaCommandService = userAccessAreaCommandService;
        this.managementRelationCommandService = managementRelationCommandService;
        this.temporaryRoleAssignmentCommandService = temporaryRoleAssignmentCommandService;
        this.temporaryAccessAreaCommandService = temporaryAccessAreaCommandService;
        this.temporaryManagementDelegationCommandService = temporaryManagementDelegationCommandService;
        this.criticalCommandAuditSupport = criticalCommandAuditSupport;
        this.utcClock = utcClock;
    }

    @Override
    public AppUser createUser(AppUser user) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_CREATE,
                CapabilityTargetEntityType.APP_USER,
                null,
                new CapabilityAdmissionPayload.UserMutation(
                        null,
                        user.employeeNumber(),
                        user.externalId(),
                        user.status()
                )
        ));
        AppUser createdUser = userCommandService.createUser(user);
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.app_user.created"),
                ENTITY_TYPE_APP_USER,
                createdUser.id(),
                null,
                createdUser,
                auditContextData(OPERATION_CREATE_USER, Map.of("employeeNumber", createdUser.employeeNumber()))
        );
        return createdUser;
    }

    @Override
    public AppUser updateUser(
            Long userId,
            String lastName,
            String firstName,
            String middleName,
            String positionTitle
    ) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_UPDATE,
                CapabilityTargetEntityType.APP_USER,
                userId,
                new CapabilityAdmissionPayload.UserMutation(
                        userId,
                        null,
                        null,
                        null
                )
        ));
        AppUser currentUser = userQueryService.findUserById(userId);
        AppUser updatedUser = userCommandService.updateUser(new AppUser(
                userId,
                currentUser.employeeNumber(),
                currentUser.externalId(),
                lastName,
                firstName,
                middleName,
                positionTitle == null ? currentUser.positionTitle() : positionTitle,
                currentUser.status(),
                currentUser.createdAt(),
                currentUser.updatedAt()
        ));
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.app_user.updated"),
                ENTITY_TYPE_APP_USER,
                updatedUser.id(),
                currentUser,
                updatedUser,
                auditContextData(OPERATION_UPDATE_USER, Map.of("employeeNumber", updatedUser.employeeNumber()))
        );
        return updatedUser;
    }

    @Override
    public AppUser deactivateUser(Long userId) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_DEACTIVATE,
                CapabilityTargetEntityType.APP_USER,
                userId
        ));
        AppUser currentUser = userQueryService.findUserById(userId);
        Instant effectiveAt = utcClock.now();

        List<UserRoleAssignment> closedRoleAssignments = userRoleAssignmentService.closeActiveRoleAssignmentsByUserId(
                userId,
                effectiveAt
        );
        List<UserOrganizationAssignment> closedOrganizationAssignments = userOrganizationAssignmentService
                .closeActiveOrganizationAssignmentsByUserId(userId, effectiveAt);
        List<UserAccessArea> closedUserAccessAreas = userAccessAreaCommandService.closeActiveUserAccessAreasByUserId(
                userId,
                effectiveAt
        );
        List<ManagementRelation> closedManagementRelations = managementRelationCommandService.closeActiveManagementRelationsByUserId(
                userId,
                effectiveAt
        );
        List<TemporaryRoleAssignment> closedTemporaryRoleAssignments = temporaryRoleAssignmentCommandService
                .closeActiveTemporaryRoleAssignmentsByUserId(userId, effectiveAt);
        List<TemporaryAccessArea> closedTemporaryAccessAreas = temporaryAccessAreaCommandService.closeActiveTemporaryAccessAreasByUserId(
                userId,
                effectiveAt
        );
        List<TemporaryManagementDelegation> closedTemporaryManagementDelegations = temporaryManagementDelegationCommandService
                .closeActiveTemporaryManagementDelegationsByUserId(userId, effectiveAt);

        AppUser deactivatedUser = userCommandService.deactivateUserAfterAdmission(userId, effectiveAt);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("effectiveAt", effectiveAt);
        details.put("closedRoleAssignments", closedRoleAssignments.size());
        details.put("closedOrganizationAssignments", closedOrganizationAssignments.size());
        details.put("closedUserAccessAreas", closedUserAccessAreas.size());
        details.put("closedManagementRelations", closedManagementRelations.size());
        details.put("closedTemporaryRoleAssignments", closedTemporaryRoleAssignments.size());
        details.put("closedTemporaryAccessAreas", closedTemporaryAccessAreas.size());
        details.put("closedTemporaryManagementDelegations", closedTemporaryManagementDelegations.size());
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.app_user.deactivated"),
                ENTITY_TYPE_APP_USER,
                deactivatedUser.id(),
                currentUser,
                deactivatedUser,
                auditContextData(OPERATION_DEACTIVATE_USER, details)
        );
        return deactivatedUser;
    }

    @Override
    public UserRoleAssignment assignRole(Long userId, Long roleId, Instant validFrom) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_ROLE_ASSIGN,
                CapabilityTargetEntityType.USER_ROLE_ASSIGNMENT,
                null,
                new CapabilityAdmissionPayload.RoleAssignment(
                        userId,
                        roleId,
                        null,
                        validFrom,
                        null,
                        false
                )
        ));
        Instant now = utcClock.now();
        UserRoleAssignment createdAssignment = userRoleAssignmentService.assignRoleAssignment(new UserRoleAssignment(
                null,
                userId,
                roleId,
                validFrom,
                null,
                now,
                now
        ));
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.user_role_assignment.assigned"),
                ENTITY_TYPE_USER_ROLE_ASSIGNMENT,
                createdAssignment.id(),
                null,
                createdAssignment,
                auditContextData(OPERATION_ASSIGN_ROLE, Map.of("userId", userId, "roleId", roleId))
        );
        return createdAssignment;
    }

    @Override
    public UserRoleAssignment closeRole(Long userId, Long assignmentId, Instant validTo) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_ROLE_CLOSE,
                CapabilityTargetEntityType.USER_ROLE_ASSIGNMENT,
                assignmentId,
                new CapabilityAdmissionPayload.RoleAssignment(
                        null,
                        null,
                        assignmentId,
                        null,
                        validTo,
                        false
                )
        ));
        UserRoleAssignment currentAssignment = requireRoleAssignmentForUser(userId, assignmentId);
        UserRoleAssignment closedAssignment = userRoleAssignmentService.closeRoleAssignment(assignmentId, validTo);
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.user_role_assignment.closed"),
                ENTITY_TYPE_USER_ROLE_ASSIGNMENT,
                closedAssignment.id(),
                currentAssignment,
                closedAssignment,
                auditContextData(OPERATION_CLOSE_ROLE, Map.of("userId", userId, "validTo", validTo))
        );
        return closedAssignment;
    }

    @Override
    public UserOrganizationAssignment assignOrganizationAssignment(
            Long userId,
            Long organizationalUnitId,
            OrganizationAssignmentType assignmentType,
            Instant validFrom
    ) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_ORGANIZATION_ASSIGN,
                CapabilityTargetEntityType.USER_ORGANIZATION_ASSIGNMENT,
                null,
                new CapabilityAdmissionPayload.OrganizationAssignment(
                        userId,
                        organizationalUnitId,
                        null,
                        assignmentType,
                        validFrom,
                        null,
                        null
                )
        ));
        Instant now = utcClock.now();
        UserOrganizationAssignment createdAssignment = userOrganizationAssignmentService.assignOrganizationAssignment(
                new UserOrganizationAssignment(
                        null,
                        userId,
                        organizationalUnitId,
                        assignmentType,
                        validFrom,
                        null,
                        now,
                        now
                )
        );
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.user_organization_assignment.assigned"),
                ENTITY_TYPE_USER_ORGANIZATION_ASSIGNMENT,
                createdAssignment.id(),
                null,
                createdAssignment,
                auditContextData(
                        OPERATION_ASSIGN_ORGANIZATION,
                        Map.of("userId", userId, "organizationalUnitId", organizationalUnitId, "assignmentType", assignmentType)
                )
        );
        return createdAssignment;
    }

    @Override
    public UserOrganizationAssignment closeOrganizationAssignment(Long userId, Long assignmentId, Instant validTo) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_ORGANIZATION_CLOSE,
                CapabilityTargetEntityType.USER_ORGANIZATION_ASSIGNMENT,
                assignmentId,
                new CapabilityAdmissionPayload.OrganizationAssignment(
                        null,
                        null,
                        assignmentId,
                        null,
                        null,
                        validTo,
                        null
                )
        ));
        UserOrganizationAssignment currentAssignment = requireOrganizationAssignmentForUser(userId, assignmentId);
        UserOrganizationAssignment closedAssignment = userOrganizationAssignmentService.closeOrganizationAssignment(
                assignmentId,
                validTo
        );
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.user_organization_assignment.closed"),
                ENTITY_TYPE_USER_ORGANIZATION_ASSIGNMENT,
                closedAssignment.id(),
                currentAssignment,
                closedAssignment,
                auditContextData(OPERATION_CLOSE_ORGANIZATION, Map.of("userId", userId, "validTo", validTo))
        );
        return closedAssignment;
    }

    @Override
    public UserOrganizationAssignment replacePrimaryHomeUnit(Long userId, Long organizationalUnitId, Instant effectiveAt) {
        Long actorUserId = resolveActorUserId();
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.create(
                actorUserId,
                CapabilityOperationCodes.USERORG_USER_PRIMARY_HOME_REPLACE,
                CapabilityTargetEntityType.APP_USER,
                userId,
                new CapabilityAdmissionPayload.OrganizationAssignment(
                        userId,
                        organizationalUnitId,
                        null,
                        OrganizationAssignmentType.PRIMARY,
                        null,
                        null,
                        effectiveAt
                )
        ));
        UserOrganizationAssignment currentPrimaryAssignment = userOrganizationAssignmentService
                .findActiveOrganizationAssignmentsByUserId(userId, effectiveAt)
                .stream()
                .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
                .findFirst()
                .orElse(null);
        UserOrganizationAssignment replacementAssignment = userOrganizationAssignmentService.replacePrimaryHomeUnit(
                userId,
                organizationalUnitId,
                effectiveAt
        );
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousPrimaryAssignmentId", currentPrimaryAssignment == null ? null : currentPrimaryAssignment.id());
        details.put("newPrimaryAssignmentId", replacementAssignment.id());
        details.put("newOrganizationalUnitId", organizationalUnitId);
        details.put("effectiveAt", effectiveAt);
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.app_user.primary_home_unit_replaced"),
                ENTITY_TYPE_APP_USER,
                userId,
                currentPrimaryAssignment,
                replacementAssignment,
                auditContextData(OPERATION_REPLACE_PRIMARY_HOME, details)
        );
        return replacementAssignment;
    }

    private UserRoleAssignment requireRoleAssignmentForUser(Long userId, Long assignmentId) {
        UserRoleAssignment assignment = userRoleAssignmentService.findRoleAssignmentById(assignmentId);
        if (!Objects.equals(assignment.userId(), userId)) {
            throw new NotFoundException("Role assignment not found for userId=" + userId + ", assignmentId=" + assignmentId);
        }
        return assignment;
    }

    private UserOrganizationAssignment requireOrganizationAssignmentForUser(Long userId, Long assignmentId) {
        UserOrganizationAssignment assignment = userOrganizationAssignmentService.findOrganizationAssignmentById(assignmentId);
        if (!Objects.equals(assignment.userId(), userId)) {
            throw new NotFoundException(
                    "Organization assignment not found for userId=" + userId + ", assignmentId=" + assignmentId
            );
        }
        return assignment;
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
