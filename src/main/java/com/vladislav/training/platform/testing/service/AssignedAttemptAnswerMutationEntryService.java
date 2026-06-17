package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.admission.AssignedAnswerMutationAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationRequest;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code AssignedAttemptAnswerMutationEntryService}.
 */
@Service
@Transactional
public class AssignedAttemptAnswerMutationEntryService {

    private final InteractiveActorResolver interactiveActorResolver;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final AssignedAnswerMutationAdmissionFoundationStateReadService foundationStateReadService;
    private final AttemptStatusRecalculationService attemptStatusRecalculationService;
    private final ActiveAttemptAnswerMutationService activeAttemptAnswerMutationService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final AttemptAnswerMutationCriticalAuditPayloadFactory auditPayloadFactory;
    private final UtcClock utcClock;

    public AssignedAttemptAnswerMutationEntryService(
        InteractiveActorResolver interactiveActorResolver,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        AssignedAnswerMutationAdmissionFoundationStateReadService foundationStateReadService,
        AttemptStatusRecalculationService attemptStatusRecalculationService,
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
        this.attemptStatusRecalculationService = Objects.requireNonNull(
            attemptStatusRecalculationService,
            "attemptStatusRecalculationService must not be null"
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

    public TestAttempt saveOrReplaceAssignedAnswer(
        Long testAttemptId,
        Long questionId,
        ActiveAttemptAnswerMutationRequest request
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        var foundationState = requireFoundation(actorUserId, testAttemptId);
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(
            actorUserId,
            foundationState.assignmentId(),
            foundationState.assignmentTestId()
        ));
        Instant now = utcClock.now();
        requireAssignedMutationWindowOpen(testAttemptId, now);
        TestAttempt mutatedAttempt = activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            actorUserId,
            testAttemptId,
            questionId,
            toMutations(request.answerItems()),
            now
        );
        recordAssignedAnswerMutationAudit(
            actorUserId,
            foundationState.assignmentId(),
            foundationState.assignmentTestId(),
            questionId,
            "save_or_replace",
            request.answerItems().size(),
            mutatedAttempt
        );
        return mutatedAttempt;
    }

    public TestAttempt clearAssignedAnswer(Long testAttemptId, Long questionId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        var foundationState = requireFoundation(actorUserId, testAttemptId);
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(
            actorUserId,
            foundationState.assignmentId(),
            foundationState.assignmentTestId()
        ));
        Instant now = utcClock.now();
        requireAssignedMutationWindowOpen(testAttemptId, now);
        TestAttempt mutatedAttempt = activeAttemptAnswerMutationService.clearAnswer(actorUserId, testAttemptId, questionId, now);
        recordAssignedAnswerMutationAudit(
            actorUserId,
            foundationState.assignmentId(),
            foundationState.assignmentTestId(),
            questionId,
            "clear",
            0,
            mutatedAttempt
        );
        return mutatedAttempt;
    }

    private AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState
    requireFoundation(Long actorUserId, Long testAttemptId) {
        var foundationState = foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(
            actorUserId,
            testAttemptId
        );
        if (foundationState == null) {
            throw new NotFoundException("Assigned answer mutation foundation not found: testAttemptId=" + testAttemptId);
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

    private void requireAssignedMutationWindowOpen(Long testAttemptId, Instant effectiveAt) {
        TestAttemptStatus recalculatedStatus = attemptStatusRecalculationService.recalculateAttemptStatus(testAttemptId, effectiveAt);
        if (recalculatedStatus == TestAttemptStatus.EXPIRED) {
            throw new ConflictException(
                "Assigned answer mutation is not allowed after deadline: testAttemptId=" + testAttemptId
            );
        }
    }

    private void recordAssignedAnswerMutationAudit(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId,
        Long questionId,
        String mutationAction,
        int answerItemCount,
        TestAttempt mutatedAttempt
    ) {
        AttemptAnswerMutationCriticalAuditCatalog auditCatalog =
            AttemptAnswerMutationCriticalAuditCatalog.ASSIGNED_ANSWER_MUTATED;
        criticalCommandAuditSupport.recordAudit(
            actorUserId,
            auditCatalog.auditEventType(),
            auditCatalog.auditEntityType(),
            mutatedAttempt.id(),
            null,
            auditPayloadFactory.payloadAfter(mutatedAttempt, mutationAction, questionId, answerItemCount, assignmentId),
            criticalCommandAuditSupport.buildAuditContext(
                "Testing",
                auditCatalog.operationCode().code(),
                auditPayloadFactory.createAssignedDetails(
                    mutationAction,
                    assignmentId,
                    assignmentTestId,
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
