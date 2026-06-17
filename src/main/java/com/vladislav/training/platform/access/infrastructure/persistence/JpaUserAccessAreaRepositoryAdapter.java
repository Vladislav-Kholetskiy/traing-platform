package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.repository.UserAccessAreaRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaUserAccessAreaRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaUserAccessAreaRepositoryAdapter implements UserAccessAreaRepository {

    private final SpringDataUserAccessAreaJpaRepository repository;
    private final AccessMapper mapper;

    public JpaUserAccessAreaRepositoryAdapter(SpringDataUserAccessAreaJpaRepository repository, AccessMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserAccessArea findUserAccessAreaById(Long userAccessAreaId) {
        return repository.findById(userAccessAreaId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("User access area not found by id: " + userAccessAreaId));
    }

    @Override
    public List<UserAccessArea> findUserAccessAreasByUserId(Long userId) {
        return mapper.toUserAccessAreas(repository.findAllByUserIdOrderByValidFromDescIdDesc(userId));
    }

    @Override
    public List<UserAccessArea> findUserAccessAreasByOrganizationalUnitId(Long organizationalUnitId) {
        return mapper.toUserAccessAreas(repository.findAllByOrganizationalUnitIdOrderByValidFromDescIdDesc(organizationalUnitId));
    }

    @Override
    public List<UserAccessArea> findActiveUserAccessAreasByUserId(Long userId, Instant activeAt) {
        return mapper.toUserAccessAreas(repository.findActiveByUserId(userId, activeAt));
    }

    @Override
    @Transactional
    public UserAccessArea saveUserAccessArea(UserAccessArea userAccessArea) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(userAccessArea)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist user_access_area", exception);
        }
    }

    @Override
    @Transactional
    public void revokeUserAccessArea(Long userAccessAreaId, Instant validTo) {
        UserAccessAreaEntity entity = repository.findById(userAccessAreaId)
            .orElseThrow(() -> new NotFoundException("User access area not found by id: " + userAccessAreaId));
        entity.setValidTo(validTo);
        entity.setUpdatedAt(validTo);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to close user_access_area", exception);
        }
    }
}
