package com.vladislav.training.platform.access.repository;

import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code TemporaryRoleAssignmentRepository}.
 */
public interface TemporaryRoleAssignmentRepository {

    TemporaryRoleAssignment findTemporaryRoleAssignmentById(Long temporaryRoleAssignmentId);

    List<TemporaryRoleAssignment> findTemporaryRoleAssignmentsByUserId(Long userId);

    List<TemporaryRoleAssignment> findActiveTemporaryRoleAssignmentsByUserId(Long userId, Instant activeAt);

    TemporaryRoleAssignment saveTemporaryRoleAssignment(TemporaryRoleAssignment temporaryRoleAssignment);

    void endTemporaryRoleAssignment(Long temporaryRoleAssignmentId, Instant validTo);
}
