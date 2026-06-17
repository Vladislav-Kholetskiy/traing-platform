package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.repository.TemporaryRoleAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code TemporaryRoleAssignmentReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
class TemporaryRoleAssignmentReadServiceImpl implements TemporaryRoleAssignmentReadService {

    private final TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository;

    TemporaryRoleAssignmentReadServiceImpl(TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository) {
        this.temporaryRoleAssignmentRepository = temporaryRoleAssignmentRepository;
    }

    @Override
    public List<Long> findActiveTemporaryRoleIdsByUserId(Long userId, Instant activeAt) {
        return temporaryRoleAssignmentRepository.findActiveTemporaryRoleAssignmentsByUserId(userId, activeAt)
            .stream()
            .map(TemporaryRoleAssignment::roleId)
            .toList();
    }
}
