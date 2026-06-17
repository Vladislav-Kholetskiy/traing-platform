package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code ActiveAttemptOwnerLocalReadService}.
 */
@Service
@Transactional(readOnly = true)
public class ActiveAttemptOwnerLocalReadService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String ASSIGNED_CURRENT_ATTEMPT_MESSAGE =
        "Actor is not authorized to read assigned current attempt";

    private final TestAttemptRepository testAttemptRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    public ActiveAttemptOwnerLocalReadService(
        TestAttemptRepository testAttemptRepository,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.contextResolver = Objects.requireNonNull(contextResolver, "contextResolver must not be null");
    }

    public TestAttempt findActiveAssignedAttemptForActor(Long actorUserId, Long assignmentTestId) {
        AccessPolicyQueryContext context = contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            "assigned_current_attempt"
        );
        if (!context.actorUserId().equals(actorUserId)) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                ASSIGNED_CURRENT_ATTEMPT_MESSAGE
            );
        }
        if (!accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                ASSIGNED_CURRENT_ATTEMPT_MESSAGE
            );
        }
        return testAttemptRepository.findActiveAssignedAttemptForActor(actorUserId, assignmentTestId);
    }

    public TestAttempt findActiveSelfAttempt(Long userId, Long testId) {
        return testAttemptRepository.findActiveSelfAttempt(userId, testId);
    }
}
