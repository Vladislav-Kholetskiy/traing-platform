package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Контракт сервиса {@code UserAdministrationPolicyReadService}.
 */
public interface UserAdministrationPolicyReadService {

    List<AppUser> findUsersWithinScope(AccessReadScope scope, UserStatus status, Instant effectiveAt);

    Optional<AppUser> findUserByIdWithinScope(AccessReadScope scope, Instant effectiveAt, Long userId);

    List<UserRoleAssignment> findRoleAssignmentsByUserIdWithinScope(AccessReadScope scope, Instant effectiveAt, Long userId);

    List<UserRoleAssignment> findActiveRoleAssignmentsByUserIdWithinScope(AccessReadScope scope, Instant effectiveAt, Long userId);

    List<UserOrganizationAssignment> findOrganizationAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    );

    List<UserOrganizationAssignment> findActiveOrganizationAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    );
}
