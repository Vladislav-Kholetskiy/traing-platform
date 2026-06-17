package com.vladislav.training.platform.userorg.repository;

import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.util.List;
import java.util.Optional;

/**
 * Контракт репозитория {@code AppUserRepository}.
 */
public interface AppUserRepository {

    AppUser findUserById(Long userId);

    AppUser findUserByEmployeeNumber(String employeeNumber);

    Optional<AppUser> findOptionalUserByEmployeeNumber(String employeeNumber);

    List<AppUser> findAllUsers();

    List<AppUser> findUsersByStatus(UserStatus status);

    boolean existsUserByEmployeeNumber(String employeeNumber);

    boolean existsUserByExternalId(String externalId);

    AppUser saveUser(AppUser user);
}
