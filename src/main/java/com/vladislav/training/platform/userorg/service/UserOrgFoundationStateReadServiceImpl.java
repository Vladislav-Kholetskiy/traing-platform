package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppRoleRepository;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitTypeRepository;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import com.vladislav.training.platform.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code UserOrgFoundationStateReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
class UserOrgFoundationStateReadServiceImpl implements UserOrgFoundationStateReadService {

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final OrganizationalUnitTypeRepository organizationalUnitTypeRepository;

    UserOrgFoundationStateReadServiceImpl(
        AppUserRepository appUserRepository,
        AppRoleRepository appRoleRepository,
        UserRoleAssignmentRepository userRoleAssignmentRepository,
        OrganizationalUnitRepository organizationalUnitRepository,
        OrganizationalUnitTypeRepository organizationalUnitTypeRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.organizationalUnitTypeRepository = organizationalUnitTypeRepository;
    }

    @Override
    public UserAccessPolicyFoundationState findUserAccessPolicyFoundationState(Long userId, Instant effectiveAt) {
        AppUser user = appUserRepository.findUserById(userId);
        Set<String> activePermanentRoleCodes = new LinkedHashSet<>();
        for (UserRoleAssignment assignment : userRoleAssignmentRepository.findActiveRoleAssignmentsByUserId(userId, effectiveAt)) {
            activePermanentRoleCodes.add(
                RoleCodeNormalizer.normalize(appRoleRepository.findRoleById(assignment.roleId()).code())
            );
        }
        return new UserAccessPolicyFoundationState(
            user.id(),
            user.status() == UserStatus.ACTIVE,
            Set.copyOf(activePermanentRoleCodes)
        );
    }


    @Override
    public ActorCommandFoundationState findActorCommandFoundationState(Long actorUserId, Instant effectiveAt) {
        UserAccessPolicyFoundationState state = findUserAccessPolicyFoundationState(actorUserId, effectiveAt);
        return new ActorCommandFoundationState(state.userId(), state.active(), state.activePermanentRoleCodes());
    }

    @Override
    public TargetUserCommandFoundationState findTargetUserCommandFoundationState(Long userId) {
        AppUser user = appUserRepository.findUserById(userId);
        return new TargetUserCommandFoundationState(user.id(), user.status() == UserStatus.ACTIVE);
    }

    @Override
    public Set<String> findRoleCodesByIds(Collection<Long> roleIds) {
        Set<String> roleCodes = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            AppRole role = appRoleRepository.findRoleById(roleId);
            roleCodes.add(RoleCodeNormalizer.normalize(role.code()));
        }
        return Set.copyOf(roleCodes);
    }

    @Override
    public UserIdentityFoundationState findUserIdentityFoundationStateByEmployeeNumber(String employeeNumber) {
        AppUser user = appUserRepository.findUserByEmployeeNumber(employeeNumber);
        return new UserIdentityFoundationState(user.id(), user.employeeNumber(), user.status() == UserStatus.ACTIVE);
    }

    @Override
    public boolean organizationalUnitTypeExists(Long organizationalUnitTypeId) {
        try {
            organizationalUnitTypeRepository.findOrganizationalUnitTypeById(organizationalUnitTypeId);
            return true;
        } catch (NotFoundException exception) {
            return false;
        }
    }

    @Override
    public OrganizationalUnitFoundationState findOrganizationalUnitFoundationState(Long organizationalUnitId) {
        OrganizationalUnit organizationalUnit = organizationalUnitRepository.findOrganizationalUnitById(organizationalUnitId);
        OrganizationalUnitType organizationalUnitType = organizationalUnitTypeRepository.findOrganizationalUnitTypeById(
            organizationalUnit.organizationalUnitTypeId()
        );
        return new OrganizationalUnitFoundationState(
            organizationalUnit.id(),
            organizationalUnit.status() == OrganizationalUnitStatus.ACTIVE,
            organizationalUnit.path(),
            organizationalUnitType.nodeKind() == OrganizationalNodeKind.LINEAR,
            organizationalUnitType.nodeKind() == OrganizationalNodeKind.FUNCTIONAL,
            organizationalUnitType.canBeOperatorHomeUnit(),
            organizationalUnitType.canHaveManagementRelation(),
            organizationalUnitType.canHaveAccessArea(),
            organizationalUnitType.participatesInSubtreeScope()
        );
    }
}

