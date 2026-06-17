package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code UserCommandServiceImpl}.
 */
@Service
@Transactional
public class UserCommandServiceImpl implements UserCommandService {

    private final AppUserRepository appUserRepository;
    private final UtcClock utcClock;

    public UserCommandServiceImpl(
            AppUserRepository appUserRepository,
            UtcClock utcClock
    ) {
        this.appUserRepository = appUserRepository;
        this.utcClock = utcClock;
    }

    @Override
    public AppUser createUser(AppUser user) {
        if (user.id() != null) {
            throw new ValidationException("appUser.id must be null for create command");
        }
        String employeeNumber = normalizeRequired(user.employeeNumber(), "employeeNumber must not be blank");
        String externalId = normalizeOptionalExternalId(user.externalId());
        String lastName = normalizeRequired(user.lastName(), "lastName must not be blank");
        String firstName = normalizeRequired(user.firstName(), "firstName must not be blank");
        String middleName = normalizeOptional(user.middleName());
        String positionTitle = normalizeOptional(user.positionTitle());
        if (user.status() == null) {
            throw new ValidationException("status must not be null");
        }

        ensureIdentifiersUniqueForCreate(employeeNumber, externalId);

        Instant now = utcClock.now();
        return appUserRepository.saveUser(new AppUser(
                null,
                employeeNumber,
                externalId,
                lastName,
                firstName,
                middleName,
                positionTitle,
                user.status(),
                now,
                now
        ));
    }

    @Override
    public AppUser updateUser(AppUser user) {
        if (user.id() == null) {
            throw new ValidationException("appUser.id must not be null for update command");
        }
        AppUser currentUser = appUserRepository.findUserById(user.id());
        String employeeNumber = normalizeRequired(user.employeeNumber(), "employeeNumber must not be blank");
        String externalId = normalizeOptionalExternalId(user.externalId());
        String lastName = normalizeRequired(user.lastName(), "lastName must not be blank");
        String firstName = normalizeRequired(user.firstName(), "firstName must not be blank");
        String middleName = normalizeOptional(user.middleName());
        String positionTitle = user.positionTitle() == null
                ? currentUser.positionTitle()
                : normalizeOptional(user.positionTitle());
        if (user.status() == null) {
            throw new ValidationException("status must not be null");
        }

        ensureIdentifiersUnchanged(currentUser, employeeNumber, externalId);

        return appUserRepository.saveUser(new AppUser(
                currentUser.id(),
                currentUser.employeeNumber(),
                currentUser.externalId(),
                lastName,
                firstName,
                middleName,
                positionTitle,
                user.status(),
                currentUser.createdAt(),
                utcClock.now()
        ));
    }

    @Override
    public AppUser deactivateUserAfterAdmission(Long userId, Instant effectiveAt) {
        if (effectiveAt == null) {
            throw new ValidationException("effectiveAt must not be null for deactivate flow");
        }
        AppUser currentUser = appUserRepository.findUserById(userId);
        return appUserRepository.saveUser(new AppUser(
                currentUser.id(),
                currentUser.employeeNumber(),
                currentUser.externalId(),
                currentUser.lastName(),
                currentUser.firstName(),
                currentUser.middleName(),
                currentUser.positionTitle(),
                UserStatus.INACTIVE,
                currentUser.createdAt(),
                effectiveAt
        ));
    }

    private void ensureIdentifiersUniqueForCreate(String employeeNumber, String externalId) {
        if (appUserRepository.existsUserByEmployeeNumber(employeeNumber)) {
            throw new ConflictException("User employeeNumber cannot be used: " + employeeNumber);
        }
        if (externalId != null && appUserRepository.existsUserByExternalId(externalId)) {
            throw new ConflictException("User externalId cannot be used: " + externalId);
        }
    }

    private void ensureIdentifiersUnchanged(AppUser currentUser, String employeeNumber, String externalId) {
        if (!Objects.equals(currentUser.employeeNumber(), employeeNumber)) {
            throw new ValidationException("employeeNumber is create-only for SCN-16 update flow");
        }
        if (!Objects.equals(currentUser.externalId(), externalId)) {
            throw new ValidationException("externalId is create-only for SCN-16 update flow");
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalizedValue = normalizeOptional(value);
        if (normalizedValue == null) {
            throw new ValidationException(message);
        }
        return normalizedValue;
    }

    private String normalizeOptionalExternalId(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.isBlank()) {
            throw new ValidationException("externalId must not be blank when provided");
        }
        return normalizedValue;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
    }
}
