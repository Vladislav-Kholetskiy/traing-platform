package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.repository.TemporaryAccessAreaRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaTemporaryAccessAreaRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaTemporaryAccessAreaRepositoryAdapter implements TemporaryAccessAreaRepository {

    private final SpringDataTemporaryAccessAreaJpaRepository repository;
    private final AccessMapper mapper;

    public JpaTemporaryAccessAreaRepositoryAdapter(
        SpringDataTemporaryAccessAreaJpaRepository repository,
        AccessMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TemporaryAccessArea findTemporaryAccessAreaById(Long temporaryAccessAreaId) {
        return repository.findById(temporaryAccessAreaId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Temporary access area not found by id: " + temporaryAccessAreaId));
    }

    @Override
    public List<TemporaryAccessArea> findTemporaryAccessAreasByUserId(Long userId) {
        return mapper.toTemporaryAccessAreas(repository.findAllByUserIdOrderByValidFromDescIdDesc(userId));
    }

    @Override
    public List<TemporaryAccessArea> findTemporaryAccessAreasByOrganizationalUnitId(Long organizationalUnitId) {
        return mapper.toTemporaryAccessAreas(
            repository.findAllByOrganizationalUnitIdOrderByValidFromDescIdDesc(organizationalUnitId)
        );
    }

    @Override
    public List<TemporaryAccessArea> findActiveTemporaryAccessAreasByUserId(Long userId, Instant activeAt) {
        return mapper.toTemporaryAccessAreas(repository.findActiveByUserId(userId, activeAt));
    }

    @Override
    @Transactional
    public TemporaryAccessArea saveTemporaryAccessArea(TemporaryAccessArea temporaryAccessArea) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(temporaryAccessArea)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist temporary_access_area", exception);
        }
    }

    @Override
    @Transactional
    public void endTemporaryAccessArea(Long temporaryAccessAreaId, Instant validTo) {
        TemporaryAccessAreaEntity entity = repository.findById(temporaryAccessAreaId)
            .orElseThrow(() -> new NotFoundException("Temporary access area not found by id: " + temporaryAccessAreaId));
        entity.setValidTo(validTo);
        entity.setUpdatedAt(validTo);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to close temporary_access_area", exception);
        }
    }
}
