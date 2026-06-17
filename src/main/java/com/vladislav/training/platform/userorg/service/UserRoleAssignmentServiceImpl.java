package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code UserRoleAssignmentServiceImpl}.
 */
@Service
@Transactional
public class UserRoleAssignmentServiceImpl implements UserRoleAssignmentService {

    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final UserRoleAssignmentValidationSupport userRoleAssignmentValidationSupport;
    private final UtcClock utcClock;

    public UserRoleAssignmentServiceImpl(
            UserRoleAssignmentRepository userRoleAssignmentRepository,
            UserRoleAssignmentValidationSupport userRoleAssignmentValidationSupport,
            UtcClock utcClock
    ) {
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.userRoleAssignmentValidationSupport = userRoleAssignmentValidationSupport;
        this.utcClock = utcClock;
    }

    @Override
    public UserRoleAssignment assignRoleAssignment(UserRoleAssignment assignment) {
        if (assignment.id() != null) {
            throw new ValidationException("userRoleAssignment.id must be null for assign command");
        }

        userRoleAssignmentValidationSupport.ensureAssignable(assignment);

        Instant now = utcClock.now();
        UserRoleAssignment toSave = new UserRoleAssignment(
                null,
                assignment.userId(),
                assignment.roleId(),
                assignment.validFrom(),
                null,
                now,
                now
        );
        return userRoleAssignmentRepository.saveRoleAssignment(toSave);
    }

    @Override
    public UserRoleAssignment findRoleAssignmentById(Long assignmentId) {
        return userRoleAssignmentRepository.findRoleAssignmentById(assignmentId);
    }

    @Override
    public List<UserRoleAssignment> findRoleAssignmentsByUserId(Long userId) {
        return userRoleAssignmentRepository.findRoleAssignmentsByUserId(userId);
    }

    @Override
    public List<UserRoleAssignment> findRoleAssignmentsByRoleId(Long roleId) {
        return userRoleAssignmentRepository.findRoleAssignmentsByRoleId(roleId);
    }

    @Override
    public List<UserRoleAssignment> findActiveRoleAssignmentsByUserId(Long userId, Instant activeAt) {
        return userRoleAssignmentRepository.findActiveRoleAssignmentsByUserId(userId, activeAt);
    }

    @Override
    public UserRoleAssignment closeRoleAssignment(Long assignmentId, Instant validTo) {
        userRoleAssignmentValidationSupport.ensureClosable(assignmentId, validTo);
        userRoleAssignmentRepository.endRoleAssignment(assignmentId, validTo);
        return userRoleAssignmentRepository.findRoleAssignmentById(assignmentId);
    }

    @Override
    public List<UserRoleAssignment> closeActiveRoleAssignmentsByUserId(Long userId, Instant effectiveAt) {
        return userRoleAssignmentRepository.findActiveRoleAssignmentsByUserId(userId, effectiveAt).stream()
                .map(assignment -> closeRoleAssignment(assignment.id(), effectiveAt))
                .toList();
    }
}