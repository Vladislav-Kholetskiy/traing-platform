package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code UserRoleAssignmentService}.
 */
public interface UserRoleAssignmentService {

    UserRoleAssignment assignRoleAssignment(UserRoleAssignment assignment);

    UserRoleAssignment findRoleAssignmentById(Long assignmentId);

    List<UserRoleAssignment> findRoleAssignmentsByUserId(Long userId);

    List<UserRoleAssignment> findRoleAssignmentsByRoleId(Long roleId);

    List<UserRoleAssignment> findActiveRoleAssignmentsByUserId(Long userId, Instant activeAt);

    UserRoleAssignment closeRoleAssignment(Long assignmentId, Instant validTo);

        List<UserRoleAssignment> closeActiveRoleAssignmentsByUserId(Long userId, Instant effectiveAt);
}
