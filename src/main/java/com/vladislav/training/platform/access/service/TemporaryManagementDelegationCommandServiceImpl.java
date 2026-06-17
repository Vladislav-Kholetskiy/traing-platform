package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code TemporaryManagementDelegationCommandServiceImpl}.
 */
@Service
@Transactional
public class TemporaryManagementDelegationCommandServiceImpl implements TemporaryManagementDelegationCommandService {

    private final TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;
    private final AccessCommandValidationSupport accessCommandValidationSupport;
    private final UtcClock utcClock;

    public TemporaryManagementDelegationCommandServiceImpl(
            TemporaryManagementDelegationRepository temporaryManagementDelegationRepository,
            AccessCommandValidationSupport accessCommandValidationSupport,
            UtcClock utcClock
    ) {
        this.temporaryManagementDelegationRepository = temporaryManagementDelegationRepository;
        this.accessCommandValidationSupport = accessCommandValidationSupport;
        this.utcClock = utcClock;
    }

    @Override
    public TemporaryManagementDelegation findTemporaryManagementDelegationById(Long temporaryManagementDelegationId) {
        return temporaryManagementDelegationRepository.findTemporaryManagementDelegationById(temporaryManagementDelegationId);
    }

    @Override
    public List<TemporaryManagementDelegation> findTemporaryManagementDelegationsByUserId(Long userId) {
        return temporaryManagementDelegationRepository.findTemporaryManagementDelegationsByUserId(userId);
    }

    @Override
    public List<TemporaryManagementDelegation> findActiveTemporaryManagementDelegationsByUserId(Long userId, Instant activeAt) {
        return temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(userId, activeAt);
    }

    @Override
    public TemporaryManagementDelegation saveTemporaryManagementDelegation(
            TemporaryManagementDelegation temporaryManagementDelegation
    ) {
        if (temporaryManagementDelegation.id() != null) {
            throw new ValidationException("temporaryManagementDelegation.id must be null for assign command");
        }

        accessCommandValidationSupport.ensureTemporaryManagementAssignable(temporaryManagementDelegation);

        Instant now = utcClock.now();
        TemporaryManagementDelegation toSave = new TemporaryManagementDelegation(
                null,
                temporaryManagementDelegation.userId(),
                temporaryManagementDelegation.organizationalUnitId(),
                temporaryManagementDelegation.managementRelationTypeId(),
                temporaryManagementDelegation.validFrom(),
                null,
                now,
                now
        );
        return temporaryManagementDelegationRepository.saveTemporaryManagementDelegation(toSave);
    }

    @Override
    public void endTemporaryManagementDelegation(Long temporaryManagementDelegationId, Instant validTo) {
        accessCommandValidationSupport.ensureTemporaryManagementClosable(temporaryManagementDelegationId, validTo);
        temporaryManagementDelegationRepository.endTemporaryManagementDelegation(temporaryManagementDelegationId, validTo);
    }

    @Override
    public List<TemporaryManagementDelegation> closeActiveTemporaryManagementDelegationsByUserId(
            Long userId,
            Instant effectiveAt
    ) {
        return temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(userId, effectiveAt)
                .stream()
                .map(activeDelegation -> {
                    endTemporaryManagementDelegation(activeDelegation.id(), effectiveAt);
                    return temporaryManagementDelegationRepository.findTemporaryManagementDelegationById(activeDelegation.id());
                })
                .toList();
    }
}
