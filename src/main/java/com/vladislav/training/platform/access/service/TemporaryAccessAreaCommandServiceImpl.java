package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.repository.TemporaryAccessAreaRepository;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code TemporaryAccessAreaCommandServiceImpl}.
 */
@Service
@Transactional
public class TemporaryAccessAreaCommandServiceImpl implements TemporaryAccessAreaCommandService {

    private final TemporaryAccessAreaRepository temporaryAccessAreaRepository;
    private final AccessCommandValidationSupport accessCommandValidationSupport;
    private final UtcClock utcClock;

    public TemporaryAccessAreaCommandServiceImpl(
            TemporaryAccessAreaRepository temporaryAccessAreaRepository,
            AccessCommandValidationSupport accessCommandValidationSupport,
            UtcClock utcClock
    ) {
        this.temporaryAccessAreaRepository = temporaryAccessAreaRepository;
        this.accessCommandValidationSupport = accessCommandValidationSupport;
        this.utcClock = utcClock;
    }

    @Override
    public TemporaryAccessArea findTemporaryAccessAreaById(Long temporaryAccessAreaId) {
        return temporaryAccessAreaRepository.findTemporaryAccessAreaById(temporaryAccessAreaId);
    }

    @Override
    public List<TemporaryAccessArea> findTemporaryAccessAreasByUserId(Long userId) {
        return temporaryAccessAreaRepository.findTemporaryAccessAreasByUserId(userId);
    }

    @Override
    public List<TemporaryAccessArea> findActiveTemporaryAccessAreasByUserId(Long userId, Instant activeAt) {
        return temporaryAccessAreaRepository.findActiveTemporaryAccessAreasByUserId(userId, activeAt);
    }

    @Override
    public TemporaryAccessArea saveTemporaryAccessArea(TemporaryAccessArea temporaryAccessArea) {
        if (temporaryAccessArea.id() != null) {
            throw new ValidationException("temporaryAccessArea.id must be null for assign command");
        }

        accessCommandValidationSupport.ensureTemporaryAccessAreaAssignable(temporaryAccessArea);

        Instant now = utcClock.now();
        TemporaryAccessArea toSave = new TemporaryAccessArea(
                null,
                temporaryAccessArea.userId(),
                temporaryAccessArea.organizationalUnitId(),
                temporaryAccessArea.accessScopeType(),
                temporaryAccessArea.validFrom(),
                null,
                now,
                now
        );
        return temporaryAccessAreaRepository.saveTemporaryAccessArea(toSave);
    }

    @Override
    public void endTemporaryAccessArea(Long temporaryAccessAreaId, Instant validTo) {
        accessCommandValidationSupport.ensureTemporaryAccessAreaClosable(temporaryAccessAreaId, validTo);
        temporaryAccessAreaRepository.endTemporaryAccessArea(temporaryAccessAreaId, validTo);
    }

    @Override
    public List<TemporaryAccessArea> closeActiveTemporaryAccessAreasByUserId(Long userId, Instant effectiveAt) {
        return temporaryAccessAreaRepository.findActiveTemporaryAccessAreasByUserId(userId, effectiveAt).stream()
                .map(activeArea -> {
                    endTemporaryAccessArea(activeArea.id(), effectiveAt);
                    return temporaryAccessAreaRepository.findTemporaryAccessAreaById(activeArea.id());
                })
                .toList();
    }
}
