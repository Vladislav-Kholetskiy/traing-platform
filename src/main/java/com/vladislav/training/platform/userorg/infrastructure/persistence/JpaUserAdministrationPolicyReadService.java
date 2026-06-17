package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.service.UserAdministrationPolicyReadService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code JpaUserAdministrationPolicyReadService}.
 */
@Service
@Transactional(readOnly = true)
public class JpaUserAdministrationPolicyReadService implements UserAdministrationPolicyReadService {

    private final PolicyScopedAppUserReadRepository appUserReadRepository;
    private final PolicyScopedUserRoleAssignmentReadRepository userRoleAssignmentReadRepository;
    private final PolicyScopedUserOrganizationAssignmentReadRepository userOrganizationAssignmentReadRepository;

    public JpaUserAdministrationPolicyReadService(
        PolicyScopedAppUserReadRepository appUserReadRepository,
        PolicyScopedUserRoleAssignmentReadRepository userRoleAssignmentReadRepository,
        PolicyScopedUserOrganizationAssignmentReadRepository userOrganizationAssignmentReadRepository
    ) {
        this.appUserReadRepository = appUserReadRepository;
        this.userRoleAssignmentReadRepository = userRoleAssignmentReadRepository;
        this.userOrganizationAssignmentReadRepository = userOrganizationAssignmentReadRepository;
    }

    @Override
    public List<AppUser> findUsersWithinScope(AccessReadScope scope, UserStatus status, Instant effectiveAt) {
        return appUserReadRepository.findUsersWithinScope(scope, status, effectiveAt);
    }

    @Override
    public Optional<AppUser> findUserByIdWithinScope(AccessReadScope scope, Instant effectiveAt, Long userId) {
        return appUserReadRepository.findUserByIdWithinScope(scope, effectiveAt, userId);
    }

    @Override
    public List<UserRoleAssignment> findRoleAssignmentsByUserIdWithinScope(AccessReadScope scope, Instant effectiveAt, Long userId) {
        return userRoleAssignmentReadRepository.findRoleAssignmentsByUserIdWithinScope(scope, effectiveAt, userId);
    }

    @Override
    public List<UserRoleAssignment> findActiveRoleAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    ) {
        return userRoleAssignmentReadRepository.findActiveRoleAssignmentsByUserIdWithinScope(scope, effectiveAt, userId);
    }

    @Override
    public List<UserOrganizationAssignment> findOrganizationAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    ) {
        return userOrganizationAssignmentReadRepository.findOrganizationAssignmentsByUserIdWithinScope(scope, effectiveAt, userId);
    }

    @Override
    public List<UserOrganizationAssignment> findActiveOrganizationAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    ) {
        return userOrganizationAssignmentReadRepository.findActiveOrganizationAssignmentsByUserIdWithinScope(
            scope,
            effectiveAt,
            userId
        );
    }
}
