package com.vladislav.training.platform.userorg.repository;

import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code UserRoleAssignmentRepository}.
 */
public interface UserRoleAssignmentRepository {

    UserRoleAssignment findRoleAssignmentById(Long assignmentId);

    List<UserRoleAssignment> findRoleAssignmentsByUserId(Long userId);

    List<UserRoleAssignment> findRoleAssignmentsByRoleId(Long roleId);

    List<UserRoleAssignment> findActiveRoleAssignmentsByUserId(Long userId, Instant activeAt);

    UserRoleAssignment saveRoleAssignment(UserRoleAssignment assignment);

    void endRoleAssignment(Long assignmentId, Instant validTo);
}
