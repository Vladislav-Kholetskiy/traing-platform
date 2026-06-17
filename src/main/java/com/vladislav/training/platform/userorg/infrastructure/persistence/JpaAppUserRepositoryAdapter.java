package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaAppUserRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaAppUserRepositoryAdapter implements AppUserRepository {

    private final SpringDataAppUserJpaRepository repository;
    private final UserOrgMapper mapper;

    public JpaAppUserRepositoryAdapter(SpringDataAppUserJpaRepository repository, UserOrgMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AppUser findUserById(Long userId) {
        return repository.findById(userId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("User not found by id: " + userId));
    }

    @Override
    public AppUser findUserByEmployeeNumber(String employeeNumber) {
        return repository.findByEmployeeNumber(employeeNumber)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("User not found by employeeNumber: " + employeeNumber));
    }

    @Override
    public Optional<AppUser> findOptionalUserByEmployeeNumber(String employeeNumber) {
        return repository.findByEmployeeNumber(employeeNumber).map(mapper::toDomain);
    }

    @Override
    public List<AppUser> findAllUsers() {
        return mapper.toAppUsers(repository.findAllByOrderByIdAsc());
    }

    @Override
    public List<AppUser> findUsersByStatus(UserStatus status) {
        return mapper.toAppUsers(repository.findAllByStatusOrderByIdAsc(status));
    }

    @Override
    public boolean existsUserByEmployeeNumber(String employeeNumber) {
        return repository.existsByEmployeeNumber(employeeNumber);
    }

    @Override
    public boolean existsUserByExternalId(String externalId) {
        if (externalId == null) {
            return false;
        }
        return repository.existsByExternalId(externalId);
    }

    @Override
    @Transactional
    public AppUser saveUser(AppUser user) {
        try {
            AppUserEntity entity = mapper.toEntity(user);
            AppUserEntity savedEntity = repository.save(entity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist app_user", exception);
        }
    }
}
