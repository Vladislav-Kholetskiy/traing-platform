package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.repository.UserAccessAreaRepository;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code UserAccessAreaCommandServiceImpl}.
 */
@Service
@Transactional
public class UserAccessAreaCommandServiceImpl implements UserAccessAreaCommandService {

    private final UserAccessAreaRepository userAccessAreaRepository;
    private final AccessCommandValidationSupport accessCommandValidationSupport;
    private final UtcClock utcClock;

    public UserAccessAreaCommandServiceImpl(
            UserAccessAreaRepository userAccessAreaRepository,
            AccessCommandValidationSupport accessCommandValidationSupport,
            UtcClock utcClock
    ) {
        this.userAccessAreaRepository = userAccessAreaRepository;
        this.accessCommandValidationSupport = accessCommandValidationSupport;
        this.utcClock = utcClock;
    }

    @Override
    public UserAccessArea findUserAccessAreaById(Long userAccessAreaId) {
        return userAccessAreaRepository.findUserAccessAreaById(userAccessAreaId);
    }

    @Override
    public List<UserAccessArea> findUserAccessAreasByUserId(Long userId) {
        return userAccessAreaRepository.findUserAccessAreasByUserId(userId);
    }

    @Override
    public List<UserAccessArea> findActiveUserAccessAreasByUserId(Long userId, Instant activeAt) {
        return userAccessAreaRepository.findActiveUserAccessAreasByUserId(userId, activeAt);
    }

    @Override
    public UserAccessArea saveUserAccessArea(UserAccessArea userAccessArea) {
        if (userAccessArea.id() != null) {
            throw new ValidationException("userAccessArea.id must be null for assign command");
        }

        accessCommandValidationSupport.ensureUserAccessAreaAssignable(userAccessArea);

        Instant now = utcClock.now();
        UserAccessArea toSave = new UserAccessArea(
                null,
                userAccessArea.userId(),
                userAccessArea.organizationalUnitId(),
                userAccessArea.accessScopeType(),
                userAccessArea.validFrom(),
                null,
                now,
                now
        );
        return userAccessAreaRepository.saveUserAccessArea(toSave);
    }

    @Override
    public void revokeUserAccessArea(Long userAccessAreaId, Instant validTo) {
        accessCommandValidationSupport.ensureUserAccessAreaClosable(userAccessAreaId, validTo);
        userAccessAreaRepository.revokeUserAccessArea(userAccessAreaId, validTo);
    }

    @Override
    public List<UserAccessArea> closeActiveUserAccessAreasByUserId(Long userId, Instant effectiveAt) {
        return userAccessAreaRepository.findActiveUserAccessAreasByUserId(userId, effectiveAt).stream()
                .map(activeArea -> {
                    revokeUserAccessArea(activeArea.id(), effectiveAt);
                    return userAccessAreaRepository.findUserAccessAreaById(activeArea.id());
                })
                .toList();
    }
}
