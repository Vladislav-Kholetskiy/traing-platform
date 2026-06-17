package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code ManagementRelationCommandServiceImpl}.
 */
@Service
@Transactional
public class ManagementRelationCommandServiceImpl implements ManagementRelationCommandService {

    private final ManagementRelationRepository managementRelationRepository;
    private final AccessCommandValidationSupport accessCommandValidationSupport;
    private final UtcClock utcClock;

    public ManagementRelationCommandServiceImpl(
            ManagementRelationRepository managementRelationRepository,
            AccessCommandValidationSupport accessCommandValidationSupport,
            UtcClock utcClock
    ) {
        this.managementRelationRepository = managementRelationRepository;
        this.accessCommandValidationSupport = accessCommandValidationSupport;
        this.utcClock = utcClock;
    }

    @Override
    public ManagementRelation findManagementRelationById(Long managementRelationId) {
        return managementRelationRepository.findManagementRelationById(managementRelationId);
    }

    @Override
    public List<ManagementRelation> findManagementRelationsByUserId(Long userId) {
        return managementRelationRepository.findManagementRelationsByUserId(userId);
    }

    @Override
    public List<ManagementRelation> findActiveManagementRelationsByUserId(Long userId, Instant activeAt) {
        return managementRelationRepository.findActiveManagementRelationsByUserId(userId, activeAt);
    }

    @Override
    public ManagementRelation saveManagementRelation(ManagementRelation managementRelation) {
        if (managementRelation.id() != null) {
            throw new ValidationException("managementRelation.id must be null for assign command");
        }

        accessCommandValidationSupport.ensureManagementRelationAssignable(managementRelation);

        Instant now = utcClock.now();
        ManagementRelation toSave = new ManagementRelation(
                null,
                managementRelation.userId(),
                managementRelation.organizationalUnitId(),
                managementRelation.managementRelationTypeId(),
                managementRelation.validFrom(),
                null,
                now,
                now
        );
        return managementRelationRepository.saveManagementRelation(toSave);
    }

    @Override
    public void endManagementRelation(Long managementRelationId, Instant validTo) {
        accessCommandValidationSupport.ensureManagementRelationClosable(managementRelationId, validTo);
        managementRelationRepository.endManagementRelation(managementRelationId, validTo);
    }

    @Override
    public List<ManagementRelation> closeActiveManagementRelationsByUserId(Long userId, Instant effectiveAt) {
        return managementRelationRepository.findActiveManagementRelationsByUserId(userId, effectiveAt).stream()
                .map(activeRelation -> {
                    endManagementRelation(activeRelation.id(), effectiveAt);
                    return managementRelationRepository.findManagementRelationById(activeRelation.id());
                })
                .toList();
    }
}
