package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.testing.admission.AssignedCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code AssignedCurrentAttemptReadService}.
 */
@Service
@Transactional(readOnly = true)
public class AssignedCurrentAttemptReadService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String ASSIGNED_CURRENT_ATTEMPT_MESSAGE =
        "Actor is not authorized to read assigned current attempt";

    private final AssignedCurrentAttemptReadFoundationStateReadService assignedCurrentAttemptReadFoundationStateReadService;
    private final ActiveAttemptOwnerLocalReadService activeAttemptOwnerLocalReadService;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    public AssignedCurrentAttemptReadService(
        AssignedCurrentAttemptReadFoundationStateReadService assignedCurrentAttemptReadFoundationStateReadService,
        ActiveAttemptOwnerLocalReadService activeAttemptOwnerLocalReadService,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.assignedCurrentAttemptReadFoundationStateReadService = Objects.requireNonNull(
            assignedCurrentAttemptReadFoundationStateReadService,
            "assignedCurrentAttemptReadFoundationStateReadService must not be null"
        );
        this.activeAttemptOwnerLocalReadService = Objects.requireNonNull(
            activeAttemptOwnerLocalReadService,
            "activeAttemptOwnerLocalReadService must not be null"
        );
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.contextResolver = Objects.requireNonNull(contextResolver, "contextResolver must not be null");
    }

    public TestAttempt findCurrentAssignedAttemptForActor(Long actorUserId, Long assignmentId, Long assignmentTestId) {
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

        var foundationState = assignedCurrentAttemptReadFoundationStateReadService
            .findAssignedCurrentAttemptReadFoundationState(actorUserId, assignmentId, assignmentTestId);
        if (foundationState == null) {
            throw new NotFoundException(
                "Assignment test not found in self-scoped assignment context: assignmentId="
                    + assignmentId
                    + ", assignmentTestId="
                    + assignmentTestId
            );
        }

        TestAttempt activeAttempt = activeAttemptOwnerLocalReadService.findActiveAssignedAttemptForActor(
            actorUserId,
            assignmentTestId
        );
        if (activeAttempt == null) {
            throw new NotFoundException("Active assigned attempt not found for assignmentTestId=" + assignmentTestId);
        }
        return activeAttempt;
    }
}
