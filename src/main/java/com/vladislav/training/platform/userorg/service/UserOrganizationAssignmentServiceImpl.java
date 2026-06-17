package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code UserOrganizationAssignmentServiceImpl}.
 */
@Service
@Transactional
public class UserOrganizationAssignmentServiceImpl implements UserOrganizationAssignmentService {

    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    private final UserOrganizationAssignmentValidationSupport userOrganizationAssignmentValidationSupport;
    private final UtcClock utcClock;

    public UserOrganizationAssignmentServiceImpl(
            UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
            UserOrganizationAssignmentValidationSupport userOrganizationAssignmentValidationSupport,
            UtcClock utcClock
    ) {
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
        this.userOrganizationAssignmentValidationSupport = userOrganizationAssignmentValidationSupport;
        this.utcClock = utcClock;
    }

    @Override
    public UserOrganizationAssignment assignOrganizationAssignment(UserOrganizationAssignment assignment) {
        if (assignment.id() != null) {
            throw new ValidationException("userOrganizationAssignment.id must be null for assign command");
        }

        userOrganizationAssignmentValidationSupport.ensureAssignable(assignment);

        Instant now = utcClock.now();
        UserOrganizationAssignment toSave = new UserOrganizationAssignment(
                null,
                assignment.userId(),
                assignment.organizationalUnitId(),
                assignment.assignmentType(),
                assignment.validFrom(),
                null,
                now,
                now
        );
        return userOrganizationAssignmentRepository.saveOrganizationAssignment(toSave);
    }

    @Override
    public UserOrganizationAssignment findOrganizationAssignmentById(Long assignmentId) {
        return userOrganizationAssignmentRepository.findOrganizationAssignmentById(assignmentId);
    }

    @Override
    public List<UserOrganizationAssignment> findOrganizationAssignmentsByUserId(Long userId) {
        return userOrganizationAssignmentRepository.findOrganizationAssignmentsByUserId(userId);
    }

    @Override
    public List<UserOrganizationAssignment> findOrganizationAssignmentsByUnitId(Long organizationalUnitId) {
        return userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(organizationalUnitId);
    }

    @Override
    public List<UserOrganizationAssignment> findOrganizationAssignmentsByType(OrganizationAssignmentType assignmentType) {
        return userOrganizationAssignmentRepository.findOrganizationAssignmentsByType(assignmentType);
    }

    @Override
    public List<UserOrganizationAssignment> findActiveOrganizationAssignmentsByUserId(Long userId, Instant activeAt) {
        return userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(userId, activeAt);
    }

    @Override
    public UserOrganizationAssignment closeOrganizationAssignment(Long assignmentId, Instant validTo) {
        userOrganizationAssignmentValidationSupport.ensureClosable(assignmentId, validTo);
        userOrganizationAssignmentRepository.endOrganizationAssignment(assignmentId, validTo);
        return userOrganizationAssignmentRepository.findOrganizationAssignmentById(assignmentId);
    }

    @Override
    public List<UserOrganizationAssignment> closeActiveOrganizationAssignmentsByUserId(Long userId, Instant effectiveAt) {
        return userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(userId, effectiveAt).stream()
                .map(assignment -> closeOrganizationAssignment(assignment.id(), effectiveAt))
                .toList();
    }

    @Override
    public UserOrganizationAssignment replacePrimaryHomeUnit(Long userId, Long newOrganizationalUnitId, Instant effectiveAt) {
        UserOrganizationAssignment currentPrimaryAssignment =
                userOrganizationAssignmentValidationSupport.ensurePrimaryHomeReplacementAllowed(
                        userId,
                        newOrganizationalUnitId,
                        effectiveAt
                );

        Instant now = utcClock.now();
        UserOrganizationAssignment replacementAssignment = new UserOrganizationAssignment(
                null,
                userId,
                newOrganizationalUnitId,
                OrganizationAssignmentType.PRIMARY,
                effectiveAt,
                null,
                now,
                now
        );

        userOrganizationAssignmentRepository.endOrganizationAssignment(currentPrimaryAssignment.id(), effectiveAt);
        return userOrganizationAssignmentRepository.saveOrganizationAssignment(replacementAssignment);
    }
}