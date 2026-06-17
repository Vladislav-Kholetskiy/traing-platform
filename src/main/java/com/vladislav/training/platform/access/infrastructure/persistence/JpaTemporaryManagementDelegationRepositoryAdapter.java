package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaTemporaryManagementDelegationRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaTemporaryManagementDelegationRepositoryAdapter implements TemporaryManagementDelegationRepository {

    private static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("validFrom"),
        Sort.Order.desc("id")
    );

    private final SpringDataTemporaryManagementDelegationJpaRepository repository;
    private final AccessMapper mapper;

    public JpaTemporaryManagementDelegationRepositoryAdapter(
        SpringDataTemporaryManagementDelegationJpaRepository repository,
        AccessMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TemporaryManagementDelegation findTemporaryManagementDelegationById(Long temporaryManagementDelegationId) {
        return repository.findById(temporaryManagementDelegationId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Temporary management delegation not found by id: " + temporaryManagementDelegationId
            ));
    }

    @Override
    public List<TemporaryManagementDelegation> findTemporaryManagementDelegationsByUserId(Long userId) {
        return mapper.toTemporaryManagementDelegations(repository.findAllByUserIdOrderByValidFromDescIdDesc(userId));
    }

    @Override
    public List<TemporaryManagementDelegation> findTemporaryManagementDelegationsByOrganizationalUnitId(Long organizationalUnitId) {
        return mapper.toTemporaryManagementDelegations(repository.findAll(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("organizationalUnitId"), organizationalUnitId),
            HISTORY_SORT
        ));
    }

    @Override
    public List<TemporaryManagementDelegation> findActiveTemporaryManagementDelegationsByUserId(Long userId, Instant activeAt) {
        return mapper.toTemporaryManagementDelegations(repository.findActiveByUserId(userId, activeAt));
    }

    @Override
    @Transactional
    public TemporaryManagementDelegation saveTemporaryManagementDelegation(
        TemporaryManagementDelegation temporaryManagementDelegation
    ) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(temporaryManagementDelegation)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to persist temporary_management_delegation",
                exception
            );
        }
    }

    @Override
    @Transactional
    public void endTemporaryManagementDelegation(Long temporaryManagementDelegationId, Instant validTo) {
        TemporaryManagementDelegationEntity entity = repository.findById(temporaryManagementDelegationId)
            .orElseThrow(() -> new NotFoundException(
                "Temporary management delegation not found by id: " + temporaryManagementDelegationId
            ));
        entity.setValidTo(validTo);
        entity.setUpdatedAt(validTo);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to close temporary_management_delegation",
                exception
            );
        }
    }
}
