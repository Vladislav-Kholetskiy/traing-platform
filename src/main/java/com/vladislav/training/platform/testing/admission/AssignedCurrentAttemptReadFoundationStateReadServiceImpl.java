package com.vladislav.training.platform.testing.admission;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code AssignedCurrentAttemptReadFoundationStateReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnBean(AssignmentSelfScopedReadRepository.class)
class AssignedCurrentAttemptReadFoundationStateReadServiceImpl
    implements AssignedCurrentAttemptReadFoundationStateReadService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String ASSIGNED_CURRENT_ATTEMPT_MESSAGE =
        "Actor is not authorized to read assigned current attempt foundation";

    private final AssignmentSelfScopedReadRepository assignmentSelfScopedReadRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    AssignedCurrentAttemptReadFoundationStateReadServiceImpl(
        AssignmentSelfScopedReadRepository assignmentSelfScopedReadRepository,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.assignmentSelfScopedReadRepository = Objects.requireNonNull(
            assignmentSelfScopedReadRepository,
            "assignmentSelfScopedReadRepository must not be null"
        );
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.contextResolver = Objects.requireNonNull(contextResolver, "contextResolver must not be null");
    }

    @Override
    public AssignedCurrentAttemptReadFoundationState findAssignedCurrentAttemptReadFoundationState(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId
    ) {
        AccessPolicyQueryContext context = contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        if (!context.actorUserId().equals(actorUserId)) {
            throw new PolicyViolationException(NOT_AUTHORIZED, ASSIGNED_CURRENT_ATTEMPT_MESSAGE);
        }
        if (!accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException(NOT_AUTHORIZED, ASSIGNED_CURRENT_ATTEMPT_MESSAGE);
        }

        Assignment assignment = assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(actorUserId, assignmentId);
        List<AssignmentTest> assignmentTests = assignmentSelfScopedReadRepository
            .findSelfScopedAssignmentTestsByAssignmentId(actorUserId, assignmentId);
        boolean assignmentTestBelongsToAssignment = assignmentTests.stream()
            .anyMatch(assignmentTest -> assignmentTest.id().equals(assignmentTestId));
        if (!assignmentTestBelongsToAssignment) {
            return null;
        }

        return new AssignedCurrentAttemptReadFoundationState(assignment.id(), assignmentTestId);
    }
}
