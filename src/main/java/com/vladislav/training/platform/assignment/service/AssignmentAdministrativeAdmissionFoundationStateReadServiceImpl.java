package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@ConditionalOnBean(AssignmentRepository.class)
class AssignmentAdministrativeAdmissionFoundationStateReadServiceImpl
    implements AssignmentAdministrativeAdmissionFoundationStateReadService {

    private final AssignmentRepository assignmentRepository;

    AssignmentAdministrativeAdmissionFoundationStateReadServiceImpl(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public AssignmentAdministrativeAdmissionFoundationState findAssignmentAdministrativeAdmissionFoundationState(
        Long assignmentId
    ) {
        Assignment assignment = requireExistingAssignment(requireAssignmentId(assignmentId));
        return new AssignmentAdministrativeAdmissionFoundationState(
            assignment.id(),
            assignment.campaignId(),
            assignment.cancelledAt() != null,
            assignment.closedAt() != null
        );
    }

    private Long requireAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("assignmentId must not be null");
        }
        return assignmentId;
    }

    private Assignment requireExistingAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findAssignmentById(assignmentId);
        if (assignment == null) {
            throw new NotFoundException("Assignment not found: " + assignmentId);
        }
        return assignment;
    }
}
