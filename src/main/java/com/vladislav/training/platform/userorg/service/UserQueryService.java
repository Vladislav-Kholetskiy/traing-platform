package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.util.List;
import java.util.Optional;

/**
 * Контракт сервиса чтения {@code UserQueryService}.
 */
public interface UserQueryService {

    AppUser findUserById(Long userId);

    AppUser findUserByEmployeeNumber(String employeeNumber);

    Optional<AppUser> findOptionalUserByEmployeeNumber(String employeeNumber);

    List<AppUser> findAllUsers();

    List<AppUser> findUsersByStatus(UserStatus status);

    AppRole findRoleById(Long roleId);

    AppRole findRoleByCode(String code);

    List<AppRole> findAllRoles();
}
