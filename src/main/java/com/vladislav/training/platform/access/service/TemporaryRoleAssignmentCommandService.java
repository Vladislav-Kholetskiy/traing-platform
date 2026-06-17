package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import java.time.Instant;
import java.util.List;

/**
 * Контракт командного сервиса {@code TemporaryRoleAssignmentCommandService}.
 */
public interface TemporaryRoleAssignmentCommandService {

    TemporaryRoleAssignment findTemporaryRoleAssignmentById(Long temporaryRoleAssignmentId);

    List<TemporaryRoleAssignment> findTemporaryRoleAssignmentsByUserId(Long userId);

    List<TemporaryRoleAssignment> findActiveTemporaryRoleAssignmentsByUserId(Long userId, Instant activeAt);

    TemporaryRoleAssignment saveTemporaryRoleAssignment(TemporaryRoleAssignment temporaryRoleAssignment);

    void endTemporaryRoleAssignment(Long temporaryRoleAssignmentId, Instant validTo);

        List<TemporaryRoleAssignment> closeActiveTemporaryRoleAssignmentsByUserId(Long userId, Instant effectiveAt);
}
