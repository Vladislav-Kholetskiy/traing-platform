package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.admission.SelfAnswerMutationAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationRequest;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code SelfAttemptAnswerMutationEntryService}.
 */
@Service
@Transactional
public class SelfAttemptAnswerMutationEntryService {

    private final InteractiveActorResolver interactiveActorResolver;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final SelfAnswerMutationAdmissionFoundationStateReadService foundationStateReadService;
    private final ActiveAttemptAnswerMutationService activeAttemptAnswerMutationService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final AttemptAnswerMutationCriticalAuditPayloadFactory auditPayloadFactory;
    private final UtcClock utcClock;

    public SelfAttemptAnswerMutationEntryService(
        InteractiveActorResolver interactiveActorResolver,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        SelfAnswerMutationAdmissionFoundationStateReadService foundationStateReadService,
        ActiveAttemptAnswerMutationService activeAttemptAnswerMutationService,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        UtcClock utcClock
    ) {
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
        this.capabilityAdmissionPolicy = Objects.requireNonNull(
            capabilityAdmissionPolicy,
            "capabilityAdmissionPolicy must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
        this.foundationStateReadService = Objects.requireNonNull(
            foundationStateReadService,
            "foundationStateReadService must not be null"
        );
        this.activeAttemptAnswerMutationService = Objects.requireNonNull(
            activeAttemptAnswerMutationService,
            "activeAttemptAnswerMutationService must not be null"
        );
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.auditPayloadFactory = new AttemptAnswerMutationCriticalAuditPayloadFactory();
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public TestAttempt saveOrReplaceSelfAnswer(
        Long testAttemptId,
        Long questionId,
        ActiveAttemptAnswerMutationRequest request
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        var foundationState = requireFoundation(actorUserId, testAttemptId);
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createSelfAnswerMutation(
            actorUserId,
            foundationState.testId()
        ));
        TestAttempt mutatedAttempt = activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            actorUserId,
            testAttemptId,
            questionId,
            toMutations(request.answerItems()),
            utcClock.now()
        );
        recordSelfAnswerMutationAudit(actorUserId, questionId, "save_or_replace", request.answerItems().size(), mutatedAttempt);
        return mutatedAttempt;
    }

    public TestAttempt clearSelfAnswer(Long testAttemptId, Long questionId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        var foundationState = requireFoundation(actorUserId, testAttemptId);
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createSelfAnswerMutation(
            actorUserId,
            foundationState.testId()
        ));
        TestAttempt mutatedAttempt = activeAttemptAnswerMutationService.clearAnswer(actorUserId, testAttemptId, questionId, utcClock.now());
        recordSelfAnswerMutationAudit(actorUserId, questionId, "clear", 0, mutatedAttempt);
        return mutatedAttempt;
    }

    private SelfAnswerMutationAdmissionFoundationStateReadService.SelfAnswerMutationAdmissionFoundationState requireFoundation(
        Long actorUserId,
        Long testAttemptId
    ) {
        var foundationState = foundationStateReadService.findSelfAnswerMutationAdmissionFoundationState(
            actorUserId,
            testAttemptId
        );
        if (foundationState == null) {
            throw new NotFoundException("Self answer mutation foundation not found: testAttemptId=" + testAttemptId);
        }
        return foundationState;
    }

    private List<ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation> toMutations(
        List<ActiveAttemptAnswerMutationRequest.ActiveAttemptAnswerItemRequest> answerItems
    ) {
        return answerItems.stream()
            .map(item -> new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(
                item.answerOptionId(),
                item.leftAnswerOptionId(),
                item.rightAnswerOptionId(),
                item.userOrderPosition()
            ))
            .toList();
    }

    private void recordSelfAnswerMutationAudit(
        Long actorUserId,
        Long questionId,
        String mutationAction,
        int answerItemCount,
        TestAttempt mutatedAttempt
    ) {
        AttemptAnswerMutationCriticalAuditCatalog auditCatalog =
            AttemptAnswerMutationCriticalAuditCatalog.SELF_ANSWER_MUTATED;
        criticalCommandAuditSupport.recordAudit(
            actorUserId,
            auditCatalog.auditEventType(),
            auditCatalog.auditEntityType(),
            mutatedAttempt.id(),
            null,
            auditPayloadFactory.payloadAfter(mutatedAttempt, mutationAction, questionId, answerItemCount, null),
            criticalCommandAuditSupport.buildAuditContext(
                "Testing",
                auditCatalog.operationCode().code(),
                auditPayloadFactory.createSelfDetails(
                    mutationAction,
                    mutatedAttempt.testId(),
                    questionId,
                    mutatedAttempt.id(),
                    mutatedAttempt.attemptMode(),
                    answerItemCount
                )
            )
        );
    }
}
