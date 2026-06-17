package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.testing.admission.SelfCurrentAttemptReadFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code SelfCurrentAttemptReadService}.
 */
@Service
@Transactional(readOnly = true)
public class SelfCurrentAttemptReadService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String SELF_CURRENT_ATTEMPT_MESSAGE =
        "Actor is not authorized to read self current attempt";

    private final SelfCurrentAttemptReadFoundationStateReadService selfCurrentAttemptReadFoundationStateReadService;
    private final ActiveAttemptOwnerLocalReadService activeAttemptOwnerLocalReadService;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    public SelfCurrentAttemptReadService(
        SelfCurrentAttemptReadFoundationStateReadService selfCurrentAttemptReadFoundationStateReadService,
        ActiveAttemptOwnerLocalReadService activeAttemptOwnerLocalReadService,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.selfCurrentAttemptReadFoundationStateReadService = Objects.requireNonNull(
            selfCurrentAttemptReadFoundationStateReadService,
            "selfCurrentAttemptReadFoundationStateReadService must not be null"
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

    public TestAttempt findCurrentSelfAttemptForActor(Long actorUserId, Long testId) {
        AccessPolicyQueryContext context = contextResolver.resolveSelfCurrentAttemptContext(actorUserId, testId);
        if (!accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException(NOT_AUTHORIZED, SELF_CURRENT_ATTEMPT_MESSAGE);
        }

        var foundationState = selfCurrentAttemptReadFoundationStateReadService
            .findSelfCurrentAttemptReadFoundationState(actorUserId, testId);
        if (foundationState == null) {
            throw new NotFoundException("Self current attempt foundation not found for testId=" + testId);
        }

        TestAttempt activeAttempt = activeAttemptOwnerLocalReadService.findActiveSelfAttempt(actorUserId, testId);
        if (activeAttempt == null) {
            throw new NotFoundException("Active self attempt not found for testId=" + testId);
        }
        return activeAttempt;
    }
}
