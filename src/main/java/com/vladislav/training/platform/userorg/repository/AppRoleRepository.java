package com.vladislav.training.platform.userorg.repository;

import com.vladislav.training.platform.userorg.domain.AppRole;
import java.util.List;

/**
 * Контракт репозитория {@code AppRoleRepository}.
 */
public interface AppRoleRepository {

    AppRole findRoleById(Long roleId);

    AppRole findRoleByCode(String code);

    List<AppRole> findAllRoles();

    AppRole saveRole(AppRole role);
}
