package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.util.List;

/**
 * Контракт сервиса чтения {@code UserAdministrationQueryService}.
 */
public interface UserAdministrationQueryService {

    List<AppUser> listUsers(UserStatus status);

    UserAdministrationCard getUserCard(Long userId);

    List<UserAdministrationRoleAssignmentView> getRoleHistory(Long userId);

    List<UserAdministrationOrganizationAssignmentView> getOrganizationAssignmentHistory(Long userId);

    List<AppRole> listRoles();

    UserAdministrationRoleAssignmentView toRoleAssignmentView(UserRoleAssignment assignment);

    UserAdministrationOrganizationAssignmentView toOrganizationAssignmentView(UserOrganizationAssignment assignment);
}