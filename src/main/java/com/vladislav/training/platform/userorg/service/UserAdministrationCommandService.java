package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import java.time.Instant;

/**
 * Контракт командного сервиса {@code UserAdministrationCommandService}.
 */
public interface UserAdministrationCommandService {

    AppUser createUser(AppUser user);

    default AppUser updateUser(
            Long userId,
            String lastName,
            String firstName,
            String middleName
    ) {
        return updateUser(userId, lastName, firstName, middleName, null);
    }

    AppUser updateUser(
            Long userId,
            String lastName,
            String firstName,
            String middleName,
            String positionTitle
    );

    AppUser deactivateUser(Long userId);

    UserRoleAssignment assignRole(Long userId, Long roleId, Instant validFrom);

    UserRoleAssignment closeRole(Long userId, Long assignmentId, Instant validTo);

    UserOrganizationAssignment assignOrganizationAssignment(
        Long userId,
        Long organizationalUnitId,
        OrganizationAssignmentType assignmentType,
        Instant validFrom
    );

    UserOrganizationAssignment closeOrganizationAssignment(Long userId, Long assignmentId, Instant validTo);

    UserOrganizationAssignment replacePrimaryHomeUnit(Long userId, Long organizationalUnitId, Instant effectiveAt);
}

