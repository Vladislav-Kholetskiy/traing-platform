package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentReadRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@ConditionalOnBean(AssignmentReadRepository.class)
class AssignmentQueryServiceImpl implements AssignmentQueryService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private final AssignmentReadRepository assignmentReadRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    AssignmentQueryServiceImpl(
        AssignmentReadRepository assignmentReadRepository,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.assignmentReadRepository = assignmentReadRepository;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.contextResolver = contextResolver;
    }

    @Override
    public Assignment findAssignmentById(Long assignmentId) {
        ensureAssignmentReadAllowed(AccessReadType.DETAIL, "assignment");
        Assignment assignment = assignmentReadRepository.findAssignmentById(assignmentId);
        if (assignment == null) {
            throw new NotFoundException("Assignment not found by id: " + assignmentId);
        }
        return assignment;
    }

    @Override
    public List<Assignment> findAllAssignments() {
        ensureAssignmentReadAllowed(AccessReadType.LIST, "assignment");
        return assignmentReadRepository.findAllAssignments();
    }

    @Override
    public List<Assignment> findAssignmentsByCampaignId(Long campaignId) {
        ensureAssignmentReadAllowed(AccessReadType.LIST, "assignment");
        return assignmentReadRepository.findAssignmentsByCampaignId(campaignId);
    }

    @Override
    public List<Assignment> findAssignmentsByUserId(Long userId) {
        ensureAssignmentReadAllowed(AccessReadType.LIST, "assignment");
        return assignmentReadRepository.findAssignmentsByUserId(userId);
    }

    @Override
    public List<Assignment> findAssignmentsByUserIdAndStatus(Long userId, AssignmentStatus status) {
        ensureAssignmentReadAllowed(AccessReadType.LIST, "assignment");
        return assignmentReadRepository.findAssignmentsByUserIdAndStatus(userId, status);
    }

    @Override
    public Assignment findActiveAssignmentByUserIdAndCourseId(Long userId, Long courseId) {
        ensureAssignmentReadAllowed(AccessReadType.DETAIL, "assignment");
        return assignmentReadRepository.findActiveAssignmentByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public AssignmentTest findAssignmentTestById(Long assignmentTestId) {
        ensureAssignmentReadAllowed(AccessReadType.DETAIL, "assignment");
        return assignmentReadRepository.findAssignmentTestById(assignmentTestId);
    }

    @Override
    public List<AssignmentTest> findAssignmentTestsByAssignmentId(Long assignmentId) {
        ensureAssignmentReadAllowed(AccessReadType.LIST, "assignment");
        return assignmentReadRepository.findAssignmentTestsByAssignmentId(assignmentId);
    }

    @Override
    public AssignmentTest findAssignmentTestByCountedResultId(Long countedResultId) {
        ensureAssignmentReadAllowed(AccessReadType.DETAIL, "assignment");
        return assignmentReadRepository.findAssignmentTestByCountedResultId(countedResultId);
    }

    @Override
    public AssignmentAdministrativeAction findAssignmentAdministrativeActionById(Long assignmentAdministrativeActionId) {
        ensureAssignmentReadAllowed(AccessReadType.DETAIL, "assignment");
        return assignmentReadRepository.findAssignmentAdministrativeActionById(assignmentAdministrativeActionId);
    }

    @Override
    public List<AssignmentAdministrativeAction> findAssignmentAdministrativeActionsByAssignmentId(Long assignmentId) {
        ensureAssignmentReadAllowed(AccessReadType.LIST, "assignment");
        return assignmentReadRepository.findAssignmentAdministrativeActionsByAssignmentId(assignmentId);
    }

    private void ensureAssignmentReadAllowed(AccessReadType readType, String targetEntityFamily) {
        if (!accessSpecificationPolicy.canRead(
            contextResolver.resolve(AccessReadArea.ASSIGNMENT, readType, targetEntityFamily)
        )) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                "Actor is not authorized to read assignment detail data"
            );
        }
    }
}

