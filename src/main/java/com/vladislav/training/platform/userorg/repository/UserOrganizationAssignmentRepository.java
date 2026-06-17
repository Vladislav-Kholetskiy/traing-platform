package com.vladislav.training.platform.userorg.repository;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code UserOrganizationAssignmentRepository}.
 */
public interface UserOrganizationAssignmentRepository {

    UserOrganizationAssignment findOrganizationAssignmentById(Long assignmentId);

    List<UserOrganizationAssignment> findOrganizationAssignmentsByUserId(Long userId);

    List<UserOrganizationAssignment> findOrganizationAssignmentsByUnitId(Long organizationalUnitId);

    List<UserOrganizationAssignment> findOrganizationAssignmentsByType(OrganizationAssignmentType assignmentType);

    List<UserOrganizationAssignment> findActiveOrganizationAssignmentsByUserId(Long userId, Instant activeAt);

    UserOrganizationAssignment saveOrganizationAssignment(UserOrganizationAssignment assignment);

    void endOrganizationAssignment(Long assignmentId, Instant validTo);
}
