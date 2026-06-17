package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.repository.AppRoleRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaAppRoleRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaAppRoleRepositoryAdapter implements AppRoleRepository {

    private final SpringDataAppRoleJpaRepository repository;
    private final UserOrgMapper mapper;

    public JpaAppRoleRepositoryAdapter(SpringDataAppRoleJpaRepository repository, UserOrgMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AppRole findRoleById(Long roleId) {
        return repository.findById(roleId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Role not found by id: " + roleId));
    }

    @Override
    public AppRole findRoleByCode(String roleCode) {
        return repository.findByCode(roleCode)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Role not found by code: " + roleCode));
    }

    @Override
    public List<AppRole> findAllRoles() {
        return mapper.toAppRoles(repository.findAllByOrderByIdAsc());
    }

    @Override
    @Transactional
    public AppRole saveRole(AppRole role) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(role)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist app_role", exception);
        }
    }
}
