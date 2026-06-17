package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.repository.TemporaryRoleAssignmentRepository;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code TemporaryRoleAssignmentCommandServiceImpl}.
 */
@Service
@Transactional
public class TemporaryRoleAssignmentCommandServiceImpl implements TemporaryRoleAssignmentCommandService {

    private final TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository;
    private final AccessCommandValidationSupport accessCommandValidationSupport;
    private final UtcClock utcClock;

    public TemporaryRoleAssignmentCommandServiceImpl(
            TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository,
            AccessCommandValidationSupport accessCommandValidationSupport,
            UtcClock utcClock
    ) {
        this.temporaryRoleAssignmentRepository = temporaryRoleAssignmentRepository;
        this.accessCommandValidationSupport = accessCommandValidationSupport;
        this.utcClock = utcClock;
    }

    @Override
    public TemporaryRoleAssignment findTemporaryRoleAssignmentById(Long temporaryRoleAssignmentId) {
        return temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentById(temporaryRoleAssignmentId);
    }

    @Override
    public List<TemporaryRoleAssignment> findTemporaryRoleAssignmentsByUserId(Long userId) {
        return temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentsByUserId(userId);
    }

    @Override
    public List<TemporaryRoleAssignment> findActiveTemporaryRoleAssignmentsByUserId(Long userId, Instant activeAt) {
        return temporaryRoleAssignmentRepository.findActiveTemporaryRoleAssignmentsByUserId(userId, activeAt);
    }

    @Override
    public TemporaryRoleAssignment saveTemporaryRoleAssignment(TemporaryRoleAssignment temporaryRoleAssignment) {
        if (temporaryRoleAssignment.id() != null) {
            throw new ValidationException("temporaryRoleAssignment.id must be null for assign command");
        }

        accessCommandValidationSupport.ensureTemporaryRoleAssignable(temporaryRoleAssignment);

        Instant now = utcClock.now();
        TemporaryRoleAssignment toSave = new TemporaryRoleAssignment(
                null,
                temporaryRoleAssignment.userId(),
                temporaryRoleAssignment.roleId(),
                temporaryRoleAssignment.validFrom(),
                null,
                now,
                now
        );
        return temporaryRoleAssignmentRepository.saveTemporaryRoleAssignment(toSave);
    }

    @Override
    public void endTemporaryRoleAssignment(Long temporaryRoleAssignmentId, Instant validTo) {
        accessCommandValidationSupport.ensureTemporaryRoleClosable(temporaryRoleAssignmentId, validTo);
        temporaryRoleAssignmentRepository.endTemporaryRoleAssignment(temporaryRoleAssignmentId, validTo);
    }

    @Override
    public List<TemporaryRoleAssignment> closeActiveTemporaryRoleAssignmentsByUserId(Long userId, Instant effectiveAt) {
        return temporaryRoleAssignmentRepository.findActiveTemporaryRoleAssignmentsByUserId(userId, effectiveAt).stream()
                .map(activeAssignment -> {
                    endTemporaryRoleAssignment(activeAssignment.id(), effectiveAt);
                    return temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentById(activeAssignment.id());
                })
                .toList();
    }
}
