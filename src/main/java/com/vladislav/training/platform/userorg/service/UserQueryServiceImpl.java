package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppRoleRepository;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code UserQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;

    public UserQueryServiceImpl(AppUserRepository appUserRepository, AppRoleRepository appRoleRepository) {
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
    }

    @Override
    public AppUser findUserById(Long userId) {
        return appUserRepository.findUserById(userId);
    }

    @Override
    public AppUser findUserByEmployeeNumber(String employeeNumber) {
        return appUserRepository.findUserByEmployeeNumber(employeeNumber);
    }

    @Override
    public Optional<AppUser> findOptionalUserByEmployeeNumber(String employeeNumber) {
        return appUserRepository.findOptionalUserByEmployeeNumber(employeeNumber);
    }

    @Override
    public List<AppUser> findAllUsers() {
        return appUserRepository.findAllUsers();
    }

    @Override
    public List<AppUser> findUsersByStatus(UserStatus status) {
        return appUserRepository.findUsersByStatus(status);
    }

    @Override
    public AppRole findRoleById(Long roleId) {
        return appRoleRepository.findRoleById(roleId);
    }

    @Override
    public AppRole findRoleByCode(String roleCode) {
        return appRoleRepository.findRoleByCode(roleCode);
    }

    @Override
    public List<AppRole> findAllRoles() {
        return appRoleRepository.findAllRoles();
    }
}
