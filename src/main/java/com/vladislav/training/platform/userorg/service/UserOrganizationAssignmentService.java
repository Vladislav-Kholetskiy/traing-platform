package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code UserOrganizationAssignmentService}.
 */
public interface UserOrganizationAssignmentService {

    UserOrganizationAssignment assignOrganizationAssignment(UserOrganizationAssignment assignment);

    UserOrganizationAssignment findOrganizationAssignmentById(Long assignmentId);

    List<UserOrganizationAssignment> findOrganizationAssignmentsByUserId(Long userId);

    List<UserOrganizationAssignment> findOrganizationAssignmentsByUnitId(Long organizationalUnitId);

    List<UserOrganizationAssignment> findOrganizationAssignmentsByType(OrganizationAssignmentType assignmentType);

    List<UserOrganizationAssignment> findActiveOrganizationAssignmentsByUserId(Long userId, Instant activeAt);

    UserOrganizationAssignment closeOrganizationAssignment(Long assignmentId, Instant validTo);

        List<UserOrganizationAssignment> closeActiveOrganizationAssignmentsByUserId(Long userId, Instant effectiveAt);

    UserOrganizationAssignment replacePrimaryHomeUnit(Long userId, Long newOrganizationalUnitId, Instant effectiveAt);
}
