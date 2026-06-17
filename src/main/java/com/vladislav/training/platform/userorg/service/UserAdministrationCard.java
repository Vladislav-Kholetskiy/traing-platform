package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.AppUser;
import java.util.List;

/**
 * Запись данных {@code UserAdministrationCard}.
 */
public record UserAdministrationCard(
        AppUser user,
        List<UserAdministrationRoleAssignmentView> activeRoleAssignments,
        List<UserAdministrationOrganizationAssignmentView> activeOrganizationAssignments
) {
}