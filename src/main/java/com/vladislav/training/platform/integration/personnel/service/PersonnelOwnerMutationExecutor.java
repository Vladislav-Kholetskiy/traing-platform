package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.ManagementRelationCommandService;
import com.vladislav.training.platform.access.service.ManagementRelationTypeQueryService;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaCommandService;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationCommandService;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentCommandService;
import com.vladislav.training.platform.access.service.UserAccessAreaCommandService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserAdministrationCommandService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import com.vladislav.training.platform.userorg.service.UserRoleAssignmentService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.lang.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
/**
 * Класс {@code PersonnelOwnerMutationExecutor}.
 */

@Service
public class PersonnelOwnerMutationExecutor {

    private static final String DEFAULT_MANAGEMENT_RELATION_TYPE_CODE = "SUPERVISOR";
    private static final Set<PersonnelPlannedMutationType> TEMPORARY_CLOSE_MUTATIONS = EnumSet.of(
        PersonnelPlannedMutationType.CLOSE_TEMPORARY_ROLE_ASSIGNMENT,
        PersonnelPlannedMutationType.CLOSE_TEMPORARY_ACCESS_AREA,
        PersonnelPlannedMutationType.CLOSE_TEMPORARY_MANAGEMENT_DELEGATION
    );

    private final UserAdministrationCommandService userAdministrationCommandService;
    private final AccessAdministrationCommandService accessAdministrationCommandService;
    private final UserQueryService userQueryService;
    private final OrganizationQueryService organizationQueryService;
    private final ManagementRelationTypeQueryService managementRelationTypeQueryService;
    @Nullable
    private final UserRoleAssignmentService userRoleAssignmentService;
    @Nullable
    private final UserOrganizationAssignmentService userOrganizationAssignmentService;
    @Nullable
    private final UserAccessAreaCommandService userAccessAreaCommandService;
    @Nullable
    private final ManagementRelationCommandService managementRelationCommandService;
    @Nullable
    private final TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService;
    @Nullable
    private final TemporaryAccessAreaCommandService temporaryAccessAreaCommandService;
    @Nullable
    private final TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService;
    private final UtcClock utcClock;

    @Autowired
    public PersonnelOwnerMutationExecutor(
        UserAdministrationCommandService userAdministrationCommandService,
        AccessAdministrationCommandService accessAdministrationCommandService,
        UserQueryService userQueryService,
        OrganizationQueryService organizationQueryService,
        ManagementRelationTypeQueryService managementRelationTypeQueryService,
        UserRoleAssignmentService userRoleAssignmentService,
        UserOrganizationAssignmentService userOrganizationAssignmentService,
        UserAccessAreaCommandService userAccessAreaCommandService,
        ManagementRelationCommandService managementRelationCommandService,
        TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService,
        TemporaryAccessAreaCommandService temporaryAccessAreaCommandService,
        TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService,
        UtcClock utcClock
    ) {
        this(
            userAdministrationCommandService,
            accessAdministrationCommandService,
            userQueryService,
            organizationQueryService,
            managementRelationTypeQueryService,
            utcClock,
            userRoleAssignmentService,
            userOrganizationAssignmentService,
            userAccessAreaCommandService,
            managementRelationCommandService,
            temporaryRoleAssignmentCommandService,
            temporaryAccessAreaCommandService,
            temporaryManagementDelegationCommandService
        );
    }

    public PersonnelOwnerMutationExecutor(
        UserAdministrationCommandService userAdministrationCommandService,
        AccessAdministrationCommandService accessAdministrationCommandService,
        UserQueryService userQueryService,
        OrganizationQueryService organizationQueryService,
        ManagementRelationTypeQueryService managementRelationTypeQueryService,
        UtcClock utcClock
    ) {
        this(
            userAdministrationCommandService,
            accessAdministrationCommandService,
            userQueryService,
            organizationQueryService,
            managementRelationTypeQueryService,
            utcClock,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private PersonnelOwnerMutationExecutor(
        UserAdministrationCommandService userAdministrationCommandService,
        AccessAdministrationCommandService accessAdministrationCommandService,
        UserQueryService userQueryService,
        OrganizationQueryService organizationQueryService,
        ManagementRelationTypeQueryService managementRelationTypeQueryService,
        UtcClock utcClock,
        @Nullable UserRoleAssignmentService userRoleAssignmentService,
        @Nullable UserOrganizationAssignmentService userOrganizationAssignmentService,
        @Nullable UserAccessAreaCommandService userAccessAreaCommandService,
        @Nullable ManagementRelationCommandService managementRelationCommandService,
        @Nullable TemporaryRoleAssignmentCommandService temporaryRoleAssignmentCommandService,
        @Nullable TemporaryAccessAreaCommandService temporaryAccessAreaCommandService,
        @Nullable TemporaryManagementDelegationCommandService temporaryManagementDelegationCommandService
    ) {
        this.userAdministrationCommandService = Objects.requireNonNull(
            userAdministrationCommandService,
            "userAdministrationCommandService must not be null"
        );
        this.accessAdministrationCommandService = Objects.requireNonNull(
            accessAdministrationCommandService,
            "accessAdministrationCommandService must not be null"
        );
        this.userQueryService = Objects.requireNonNull(userQueryService, "userQueryService must not be null");
        this.organizationQueryService = Objects.requireNonNull(
            organizationQueryService,
            "organizationQueryService must not be null"
        );
        this.managementRelationTypeQueryService = Objects.requireNonNull(
            managementRelationTypeQueryService,
            "managementRelationTypeQueryService must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.userOrganizationAssignmentService = userOrganizationAssignmentService;
        this.userAccessAreaCommandService = userAccessAreaCommandService;
        this.managementRelationCommandService = managementRelationCommandService;
        this.temporaryRoleAssignmentCommandService = temporaryRoleAssignmentCommandService;
        this.temporaryAccessAreaCommandService = temporaryAccessAreaCommandService;
        this.temporaryManagementDelegationCommandService = temporaryManagementDelegationCommandService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long execute(PersonnelBusinessIntent intent, PersonnelPlan plan) {
        Objects.requireNonNull(intent, "intent must not be null");
        Objects.requireNonNull(plan, "plan must not be null");

        Instant effectiveAt = utcClock.now();
        boolean createPlan = plan.outcomeCode() == com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED;
        Long targetUserId = createPlan
            ? createUser(intent, effectiveAt).id()
            : requireResolvedUserId(plan);

        if (!createPlan) {
            synchronizePositionTitle(targetUserId, intent);
        }

        if (plan.plannedMutations().stream()
            .anyMatch(mutation -> mutation.mutationType() == PersonnelPlannedMutationType.DEACTIVATE_USER_TO_INACTIVE)) {
            userAdministrationCommandService.deactivateUser(targetUserId);
            return targetUserId;
        }

        rejectUnsupportedTemporaryReplacement(plan);
        if (!createPlan) {
            applyPrimaryHomeMutationIfPlanned(targetUserId, plan.plannedMutations(), effectiveAt);
        }

        for (PersonnelPlannedMutation mutation : plan.plannedMutations()) {
            if (mutation.mutationType() == PersonnelPlannedMutationType.CREATE_USER
                || mutation.mutationType() == PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT
                || (!createPlan && mutation.mutationType() == PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT)) {
                continue;
            }
            applyMutation(targetUserId, intent, mutation, effectiveAt);
        }
        return targetUserId;
    }

    private Long requireResolvedUserId(PersonnelPlan plan) {
        if (plan.resolvedUserId() == null) {
            throw new IllegalStateException(
                "Personnel apply mutation requires resolvedUserId; mutation path is fail-closed without identity propagation"
            );
        }
        return plan.resolvedUserId();
    }

    private void rejectUnsupportedTemporaryReplacement(PersonnelPlan plan) {
        boolean hasTemporaryClose = plan.plannedMutations().stream()
            .map(PersonnelPlannedMutation::mutationType)
            .anyMatch(TEMPORARY_CLOSE_MUTATIONS::contains);
        if (hasTemporaryClose) {
            throw new IllegalArgumentException("Unsupported temporary replacement semantics; v1 supports only additive temporary apply");
        }
    }

    private void applyPrimaryHomeMutationIfPlanned(
        Long userId,
        List<PersonnelPlannedMutation> mutations,
        Instant effectiveAt
    ) {
        boolean closesPrimary = mutations.stream()
            .anyMatch(mutation -> mutation.mutationType() == PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT);
        Optional<PersonnelPlannedMutation> opensPrimary = mutations.stream()
            .filter(mutation -> mutation.mutationType() == PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT)
            .findFirst();
        if (!closesPrimary && opensPrimary.isPresent()) {
            OrganizationalUnit targetUnit = resolveOrganizationalUnit(opensPrimary.get().targetRef());
            userAdministrationCommandService.assignOrganizationAssignment(
                userId,
                targetUnit.id(),
                OrganizationAssignmentType.PRIMARY,
                effectiveAt
            );
            return;
        }
        if (closesPrimary && opensPrimary.isPresent()) {
            OrganizationalUnit replacementUnit = resolveOrganizationalUnit(opensPrimary.get().targetRef());
            userAdministrationCommandService.replacePrimaryHomeUnit(userId, replacementUnit.id(), effectiveAt);
        }
    }

    private AppUser createUser(PersonnelBusinessIntent intent, Instant effectiveAt) {
        return userAdministrationCommandService.createUser(new AppUser(
            null,
            intent.employeeNumber(),
            intent.externalIdConsistencyGuard(),
            intent.lastName(),
            intent.firstName(),
            intent.middleName(),
            resolvePositionTitle(intent),
            com.vladislav.training.platform.userorg.domain.UserStatus.ACTIVE,
            effectiveAt,
            effectiveAt
        ));
    }

    private void synchronizePositionTitle(Long userId, PersonnelBusinessIntent intent) {
        if (intent.employmentAction() != com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction.ENSURE_ACTIVE) {
            return;
        }
        String resolvedPositionTitle = resolvePositionTitle(intent);
        userAdministrationCommandService.updateUser(
            userId,
            intent.lastName(),
            intent.firstName(),
            intent.middleName(),
            resolvedPositionTitle
        );
    }

    private String resolvePositionTitle(PersonnelBusinessIntent intent) {
        String positionCode = intent.basePositionMapping().positionCode();
        return switch (positionCode) {
            case "OPS" -> "Оператор технологической установки";
            case "DEV" -> resolveSpecialistPositionTitle(intent.homeOrgUnitCode());
            case "HEAD" -> resolveHeadPositionTitle(intent.homeOrgUnitCode());
            default -> null;
        };
    }

    private String resolveSpecialistPositionTitle(String homeOrgUnitCode) {
        return switch (homeOrgUnitCode) {
            case "EDU" -> "Специалист по учебным материалам";
            case "HSE" -> "Специалист по охране труда и промышленной безопасности";
            default -> "Специалист";
        };
    }

    private String resolveHeadPositionTitle(String homeOrgUnitCode) {
        return switch (homeOrgUnitCode) {
            case "HQ" -> "Генеральный директор";
            case "PROD" -> "Начальник производства";
            default -> deriveHeadTitleFromUnitName(resolveOrganizationalUnit(homeOrgUnitCode).name());
        };
    }

    private String deriveHeadTitleFromUnitName(String unitName) {
        if (unitName == null || unitName.isBlank()) {
            return "Руководитель подразделения";
        }
        if (unitName.startsWith("Комплекс ")) {
            return "Начальник комплекса " + unitName.substring("Комплекс ".length());
        }
        if (unitName.startsWith("Технологическая установка ")) {
            return "Начальник установки " + unitName.substring("Технологическая установка ".length());
        }
        if (unitName.startsWith("Отдел ")) {
            return "Начальник отдела " + unitName.substring("Отдел ".length());
        }
        return "Руководитель подразделения";
    }

    private void applyMutation(Long userId, PersonnelBusinessIntent intent, PersonnelPlannedMutation mutation, Instant effectiveAt) {
        switch (mutation.mutationType()) {
            case OPEN_PRIMARY_ORG_ASSIGNMENT -> {
                OrganizationalUnit unit = resolveOrganizationalUnit(mutation.targetRef());
                userAdministrationCommandService.assignOrganizationAssignment(
                    userId,
                    unit.id(),
                    OrganizationAssignmentType.PRIMARY,
                    effectiveAt
                );
            }
            case OPEN_ROLE_ASSIGNMENT -> {
                AppRole role = userQueryService.findRoleByCode(mutation.targetRef());
                userAdministrationCommandService.assignRole(userId, role.id(), effectiveAt);
            }
            case CLOSE_ROLE_ASSIGNMENT -> closeRole(userId, mutation.targetRef(), effectiveAt);
            case OPEN_MANAGEMENT_RELATION -> {
                OrganizationalUnit unit = resolveOrganizationalUnit(mutation.targetRef());
                ManagementRelationType relationType = managementRelationTypeQueryService.findManagementRelationTypeByCode(
                    DEFAULT_MANAGEMENT_RELATION_TYPE_CODE
                );
                accessAdministrationCommandService.assignManagementRelation(
                    userId,
                    unit.id(),
                    relationType.id(),
                    effectiveAt
                );
            }
            case CLOSE_MANAGEMENT_RELATION -> closeManagementRelation(userId, effectiveAt);
            case OPEN_USER_ACCESS_AREA -> {
                AccessScopeType scopeType = toAccessScopeType(mutation.targetRef());
                OrganizationalUnit unit = scopeType == AccessScopeType.GLOBAL
                    ? null
                    : resolveOrganizationalUnit(intent.homeOrgUnitCode());
                accessAdministrationCommandService.assignUserAccessArea(
                    userId,
                    unit == null ? null : unit.id(),
                    scopeType,
                    effectiveAt
                );
            }
            case CLOSE_USER_ACCESS_AREA -> closeUserAccessArea(userId, mutation.targetRef(), intent.homeOrgUnitCode(), effectiveAt);
            case OPEN_TEMPORARY_ROLE_ASSIGNMENT -> {
                AppRole role = userQueryService.findRoleByCode(mutation.targetRef());
                accessAdministrationCommandService.assignTemporaryRoleAssignment(userId, role.id(), effectiveAt);
            }
            case OPEN_TEMPORARY_ACCESS_AREA -> {
                AccessScopeType scopeType = toAccessScopeType(mutation.targetRef());
                OrganizationalUnit unit = scopeType == AccessScopeType.GLOBAL
                    ? null
                    : resolveOrganizationalUnit(intent.temporaryAppointmentIntent().orgUnitCode());
                accessAdministrationCommandService.assignTemporaryAccessArea(
                    userId,
                    unit == null ? null : unit.id(),
                    scopeType,
                    effectiveAt
                );
            }
            case OPEN_TEMPORARY_MANAGEMENT_DELEGATION -> {
                OrganizationalUnit unit = resolveOrganizationalUnit(mutation.targetRef());
                ManagementRelationType relationType = managementRelationTypeQueryService.findManagementRelationTypeByCode(
                    DEFAULT_MANAGEMENT_RELATION_TYPE_CODE
                );
                accessAdministrationCommandService.assignTemporaryManagementDelegation(
                    userId,
                    unit.id(),
                    relationType.id(),
                    effectiveAt
                );
            }
            case CLOSE_TEMPORARY_ROLE_ASSIGNMENT -> closeTemporaryRole(userId, mutation.targetRef(), effectiveAt);
            case CLOSE_TEMPORARY_ACCESS_AREA -> closeTemporaryAccessArea(userId, mutation.targetRef(), intent, effectiveAt);
            case CLOSE_TEMPORARY_MANAGEMENT_DELEGATION -> closeTemporaryManagementDelegation(userId, effectiveAt);
            case DEACTIVATE_USER_TO_INACTIVE, CLOSE_PRIMARY_ORG_ASSIGNMENT -> {
                return;
            }
        }
    }

    private void closeRole(Long userId, String roleCode, Instant effectiveAt) {
        requireReadService(userRoleAssignmentService, "role close requires UserRoleAssignmentService");
        AppRole role = userQueryService.findRoleByCode(roleCode);
        UserRoleAssignment assignment = userRoleAssignmentService.findActiveRoleAssignmentsByUserId(userId, effectiveAt).stream()
            .filter(candidate -> Objects.equals(candidate.roleId(), role.id()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Active role assignment not found for roleCode: " + roleCode));
        userAdministrationCommandService.closeRole(userId, assignment.id(), effectiveAt);
    }

    private void closeManagementRelation(Long userId, Instant effectiveAt) {
        requireReadService(managementRelationCommandService, "management relation close requires ManagementRelationCommandService");
        managementRelationCommandService.findActiveManagementRelationsByUserId(userId, effectiveAt).stream()
            .findFirst()
            .ifPresent(relation -> accessAdministrationCommandService.closeManagementRelation(relation.id(), effectiveAt));
    }

    private void closeUserAccessArea(Long userId, String accessScopeCode, String homeOrgUnitCode, Instant effectiveAt) {
        requireReadService(userAccessAreaCommandService, "access area close requires UserAccessAreaCommandService");
        Long unitId = resolveOrganizationalUnit(homeOrgUnitCode).id();
        AccessScopeType scopeType = toAccessScopeType(accessScopeCode);
        userAccessAreaCommandService.findActiveUserAccessAreasByUserId(userId, effectiveAt).stream()
            .filter(area -> Objects.equals(area.organizationalUnitId(), unitId) || scopeType == AccessScopeType.GLOBAL)
            .filter(area -> area.accessScopeType() == scopeType)
            .findFirst()
            .ifPresent(area -> accessAdministrationCommandService.closeUserAccessArea(area.id(), effectiveAt));
    }

    private void closeTemporaryRole(Long userId, String roleCode, Instant effectiveAt) {
        requireReadService(temporaryRoleAssignmentCommandService, "temporary role close requires TemporaryRoleAssignmentCommandService");
        AppRole role = userQueryService.findRoleByCode(roleCode);
        temporaryRoleAssignmentCommandService.findActiveTemporaryRoleAssignmentsByUserId(userId, effectiveAt).stream()
            .filter(candidate -> Objects.equals(candidate.roleId(), role.id()))
            .findFirst()
            .ifPresent(assignment -> accessAdministrationCommandService.closeTemporaryRoleAssignment(assignment.id(), effectiveAt));
    }

    private void closeTemporaryAccessArea(
        Long userId,
        String accessScopeCode,
        PersonnelBusinessIntent intent,
        Instant effectiveAt
    ) {
        requireReadService(temporaryAccessAreaCommandService, "temporary access area close requires TemporaryAccessAreaCommandService");
        Long unitId = resolveOrganizationalUnit(intent.temporaryAppointmentIntent().orgUnitCode()).id();
        AccessScopeType scopeType = toAccessScopeType(accessScopeCode);
        temporaryAccessAreaCommandService.findActiveTemporaryAccessAreasByUserId(userId, effectiveAt).stream()
            .filter(area -> Objects.equals(area.organizationalUnitId(), unitId) || scopeType == AccessScopeType.GLOBAL)
            .filter(area -> area.accessScopeType() == scopeType)
            .findFirst()
            .ifPresent(area -> accessAdministrationCommandService.closeTemporaryAccessArea(area.id(), effectiveAt));
    }

    private void closeTemporaryManagementDelegation(Long userId, Instant effectiveAt) {
        requireReadService(
            temporaryManagementDelegationCommandService,
            "temporary management close requires TemporaryManagementDelegationCommandService"
        );
        temporaryManagementDelegationCommandService.findActiveTemporaryManagementDelegationsByUserId(userId, effectiveAt).stream()
            .findFirst()
            .ifPresent(delegation -> accessAdministrationCommandService.closeTemporaryManagementDelegation(delegation.id(), effectiveAt));
    }

    private OrganizationalUnit resolveOrganizationalUnit(String unitCodeOrPath) {
        return organizationQueryService.findOptionalOrganizationalUnitByExternalId(unitCodeOrPath)
            .or(() -> organizationQueryService.findOptionalOrganizationalUnitByPath(unitCodeOrPath))
            .orElseThrow(() -> new IllegalArgumentException("Organizational unit not found by code or path: " + unitCodeOrPath));
    }

    private AccessScopeType toAccessScopeType(String accessScopeCode) {
        if ("UNIT".equals(accessScopeCode)) {
            return AccessScopeType.UNIT_ONLY;
        }
        if ("SELF".equals(accessScopeCode)) {
            return AccessScopeType.GLOBAL;
        }
        return AccessScopeType.valueOf(accessScopeCode);
    }

    private <T> T requireReadService(@Nullable T readService, String message) {
        if (readService == null) {
            throw new IllegalStateException(message);
        }
        return readService;
    }
}
