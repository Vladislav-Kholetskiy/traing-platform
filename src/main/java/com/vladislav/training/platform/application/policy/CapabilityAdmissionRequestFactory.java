package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignLaunchFoundationStateReadService;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Фабрика {@code CapabilityAdmissionRequestFactory}.
 */
@Component
public class CapabilityAdmissionRequestFactory {

    private final InteractiveActorResolver interactiveActorResolver;
    private final UtcClock utcClock;

    public CapabilityAdmissionRequestFactory(
        InteractiveActorResolver interactiveActorResolver,
        UtcClock utcClock
    ) {
        this.interactiveActorResolver = interactiveActorResolver;
        this.utcClock = utcClock;
    }

    public CapabilityAdmissionRequest create(
        String operationCode,
        CapabilityTargetEntityType targetEntityType,
        Long targetEntityId
    ) {
        return create(operationCode, targetEntityType, targetEntityId, CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    public CapabilityAdmissionRequest create(
        String operationCode,
        CapabilityTargetEntityType targetEntityType,
        Long targetEntityId,
        CapabilityAdmissionPayload payloadContext
    ) {
        return create(
            interactiveActorResolver.resolveActorUserId(),
            operationCode,
            targetEntityType,
            targetEntityId,
            payloadContext
        );
    }

    public CapabilityAdmissionRequest create(
        Long actorUserId,
        String operationCode,
        CapabilityTargetEntityType targetEntityType,
        Long targetEntityId
    ) {
        return create(actorUserId, operationCode, targetEntityType, targetEntityId, CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    public CapabilityAdmissionRequest create(
        Long actorUserId,
        String operationCode,
        CapabilityTargetEntityType targetEntityType,
        Long targetEntityId,
        CapabilityAdmissionPayload payloadContext
    ) {
        return new CapabilityAdmissionRequest(
            actorUserId,
            operationCode,
            targetEntityType,
            targetEntityId,
            payloadContext == null ? CapabilityAdmissionPayload.Empty.INSTANCE : payloadContext,
            utcClock.now()
        );
    }

        public CapabilityAdmissionRequest createAssignmentCampaignLaunch(
        AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchAdmissionAnchor launchAdmissionAnchor
    ) {
        return createAssignmentCampaignLaunch(interactiveActorResolver.resolveActorUserId(), launchAdmissionAnchor);
    }

    public CapabilityAdmissionRequest createAssignmentCampaignLaunch(
        Long actorUserId,
        AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchAdmissionAnchor launchAdmissionAnchor
    ) {
        Objects.requireNonNull(launchAdmissionAnchor, "launchAdmissionAnchor must not be null");
        return create(
            actorUserId,
            CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH,
            CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN,
            null,
            new CapabilityAdmissionPayload.AssignmentCampaignLaunch(
                launchAdmissionAnchor.sourceType(),
                launchAdmissionAnchor.sourceRef()
            )
        );
    }

    public CapabilityAdmissionRequest createAssignmentCancel(Long assignmentId) {
        return createAssignmentCancel(interactiveActorResolver.resolveActorUserId(), assignmentId);
    }

    public CapabilityAdmissionRequest createAssignmentCancel(Long actorUserId, Long assignmentId) {
        return create(
            actorUserId,
            CapabilityOperationCodes.ASSIGNMENT_CANCEL,
            CapabilityTargetEntityType.ASSIGNMENT,
            assignmentId,
            CapabilityAdmissionPayload.AssignmentCancel.INSTANCE
        );
    }

    public CapabilityAdmissionRequest createAssignmentDeadlineExtend(Long assignmentId, Instant newDeadlineAt) {
        return createAssignmentDeadlineExtend(interactiveActorResolver.resolveActorUserId(), assignmentId, newDeadlineAt);
    }

    public CapabilityAdmissionRequest createAssignmentDeadlineExtend(
        Long actorUserId,
        Long assignmentId,
        Instant newDeadlineAt
    ) {
        return create(
            actorUserId,
            CapabilityOperationCodes.ASSIGNMENT_DEADLINE_EXTEND,
            CapabilityTargetEntityType.ASSIGNMENT,
            assignmentId,
            new CapabilityAdmissionPayload.AssignmentDeadlineExtend(newDeadlineAt)
        );
    }

    public CapabilityAdmissionRequest createAssignmentReplaceWithNew(Long assignmentId, Long campaignId) {
        return createAssignmentReplaceWithNew(interactiveActorResolver.resolveActorUserId(), assignmentId, campaignId);
    }

    public CapabilityAdmissionRequest createAssignmentReplaceWithNew(
        Long actorUserId,
        Long assignmentId,
        Long campaignId
    ) {
        return create(
            actorUserId,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            assignmentId,
            new CapabilityAdmissionPayload.AssignmentReplaceWithNew(campaignId)
        );
    }

    public CapabilityAdmissionRequest createAssignedAttemptStart(Long assignmentId, Long assignmentTestId) {
        return createAssignedAttemptStart(interactiveActorResolver.resolveActorUserId(), assignmentId, assignmentTestId);
    }

    public CapabilityAdmissionRequest createAssignedAttemptStart(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId
    ) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            assignmentTestId,
            new CapabilityAdmissionPayload.AssignedExecution(assignmentId)
        );
    }

    public CapabilityAdmissionRequest createAssignedAttemptContinue(Long assignmentId, Long assignmentTestId) {
        return createAssignedAttemptContinue(interactiveActorResolver.resolveActorUserId(), assignmentId, assignmentTestId);
    }

    public CapabilityAdmissionRequest createAssignedAttemptContinue(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId
    ) {
        return create(
            actorUserId,
            CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_CONTINUE.code(),
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            assignmentTestId,
            new CapabilityAdmissionPayload.AssignedExecution(assignmentId)
        );
    }

    public CapabilityAdmissionRequest createAssignedAttemptSubmit(Long assignmentId, Long assignmentTestId) {
        return createAssignedAttemptSubmit(interactiveActorResolver.resolveActorUserId(), assignmentId, assignmentTestId);
    }

    public CapabilityAdmissionRequest createAssignedAttemptSubmit(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId
    ) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_SUBMIT,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            assignmentTestId,
            new CapabilityAdmissionPayload.AssignedExecution(assignmentId)
        );
    }

    public CapabilityAdmissionRequest createSelfAttemptStart(Long testId) {
        return createSelfAttemptStart(interactiveActorResolver.resolveActorUserId(), testId);
    }

    public CapabilityAdmissionRequest createSelfAttemptStart(Long actorUserId, Long testId) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            testId,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE
        );
    }

    public CapabilityAdmissionRequest createSelfAttemptContinue(Long testId) {
        return createSelfAttemptContinue(interactiveActorResolver.resolveActorUserId(), testId);
    }

    public CapabilityAdmissionRequest createSelfAttemptContinue(Long actorUserId, Long testId) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_CONTINUE,
            CapabilityTargetEntityType.TEST,
            testId,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE
        );
    }

    public CapabilityAdmissionRequest createSelfAttemptSubmit(Long testId) {
        return createSelfAttemptSubmit(interactiveActorResolver.resolveActorUserId(), testId);
    }

    public CapabilityAdmissionRequest createSelfAttemptSubmit(Long actorUserId, Long testId) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_SUBMIT,
            CapabilityTargetEntityType.TEST,
            testId,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE
        );
    }

    public CapabilityAdmissionRequest createSelfAttemptAbandon(Long testId) {
        return createSelfAttemptAbandon(interactiveActorResolver.resolveActorUserId(), testId);
    }

    public CapabilityAdmissionRequest createSelfAttemptAbandon(Long actorUserId, Long testId) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_ABANDON,
            CapabilityTargetEntityType.TEST,
            testId,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE
        );
    }

    public CapabilityAdmissionRequest createAssignedAnswerMutation(Long assignmentId, Long assignmentTestId) {
        return createAssignedAnswerMutation(
            interactiveActorResolver.resolveActorUserId(),
            assignmentId,
            assignmentTestId
        );
    }

    public CapabilityAdmissionRequest createAssignedAnswerMutation(Long actorUserId, Long assignmentId, Long assignmentTestId) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_ASSIGNED_ANSWER_MUTATION,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            assignmentTestId,
            new CapabilityAdmissionPayload.AssignedExecution(assignmentId)
        );
    }

    public CapabilityAdmissionRequest createSelfAnswerMutation(Long testId) {
        return createSelfAnswerMutation(interactiveActorResolver.resolveActorUserId(), testId);
    }

    public CapabilityAdmissionRequest createSelfAnswerMutation(Long actorUserId, Long testId) {
        return create(
            actorUserId,
            CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION,
            CapabilityTargetEntityType.TEST,
            testId,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE
        );
    }

    public CapabilityAdmissionRequest createNotificationRuleCreate() {
        return createNotificationRuleCreate(interactiveActorResolver.resolveActorUserId());
    }

    public CapabilityAdmissionRequest createNotificationRuleCreate(Long actorUserId) {
        return create(
            actorUserId,
            CapabilityOperationCode.NOTIFICATION_RULE_CREATE.code(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            null
        );
    }

    public CapabilityAdmissionRequest createNotificationRuleUpdate(Long notificationRuleId) {
        return createNotificationRuleUpdate(interactiveActorResolver.resolveActorUserId(), notificationRuleId);
    }

    public CapabilityAdmissionRequest createNotificationRuleUpdate(Long actorUserId, Long notificationRuleId) {
        return create(
            actorUserId,
            CapabilityOperationCode.NOTIFICATION_RULE_UPDATE.code(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            notificationRuleId
        );
    }

    public CapabilityAdmissionRequest createNotificationRuleEnable(Long notificationRuleId) {
        return createNotificationRuleEnable(interactiveActorResolver.resolveActorUserId(), notificationRuleId);
    }

    public CapabilityAdmissionRequest createNotificationRuleEnable(Long actorUserId, Long notificationRuleId) {
        return create(
            actorUserId,
            CapabilityOperationCode.NOTIFICATION_RULE_ENABLE.code(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            notificationRuleId
        );
    }

    public CapabilityAdmissionRequest createNotificationRuleDisable(Long notificationRuleId) {
        return createNotificationRuleDisable(interactiveActorResolver.resolveActorUserId(), notificationRuleId);
    }

    public CapabilityAdmissionRequest createNotificationRuleDisable(Long actorUserId, Long notificationRuleId) {
        return create(
            actorUserId,
            CapabilityOperationCode.NOTIFICATION_RULE_DISABLE.code(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            notificationRuleId
        );
    }

    public CapabilityAdmissionRequest createAnalyticsResultRebuild() {
        return createAnalyticsResultRebuild(interactiveActorResolver.resolveActorUserId());
    }

    public CapabilityAdmissionRequest createAnalyticsResultRebuild(Long actorUserId) {
        return create(
            actorUserId,
            CapabilityOperationCode.ANALYTICS_RESULT_REBUILD.code(),
            CapabilityTargetEntityType.ANALYTICS_AGGREGATE,
            null
        );
    }

    public CapabilityAdmissionRequest createPersonnelExcelDryRun() {
        return createPersonnelExcelDryRun(interactiveActorResolver.resolveActorUserId());
    }

    public CapabilityAdmissionRequest createPersonnelExcelDryRun(Long actorUserId) {
        return create(
            actorUserId,
            CapabilityOperationCode.PERSONNEL_EXCEL_DRY_RUN.code(),
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null
        );
    }

    public CapabilityAdmissionRequest createPersonnelExcelApply() {
        return createPersonnelExcelApply(interactiveActorResolver.resolveActorUserId());
    }

    public CapabilityAdmissionRequest createPersonnelExcelApply(Long actorUserId) {
        return create(
            actorUserId,
            CapabilityOperationCode.PERSONNEL_EXCEL_APPLY.code(),
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null
        );
    }

    public CapabilityAdmissionRequest createImportJobLaunch() {
        return createImportJobLaunch(interactiveActorResolver.resolveActorUserId());
    }

    public CapabilityAdmissionRequest createImportJobLaunch(Long actorUserId) {
        return create(
            actorUserId,
            CapabilityOperationCode.IMPORT_JOB_LAUNCH.code(),
            CapabilityTargetEntityType.IMPORT_JOB,
            null
        );
    }

    public CapabilityAdmissionRequest createImportItemReviewApply(Long itemId) {
        return createImportItemReviewApply(interactiveActorResolver.resolveActorUserId(), itemId);
    }

    public CapabilityAdmissionRequest createImportItemReviewApply(Long actorUserId, Long itemId) {
        return create(
            actorUserId,
            CapabilityOperationCode.IMPORT_ITEM_REVIEW_APPLY.code(),
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            itemId
        );
    }

    public CapabilityAdmissionRequest createImportItemReviewReject(Long itemId) {
        return createImportItemReviewReject(interactiveActorResolver.resolveActorUserId(), itemId);
    }

    public CapabilityAdmissionRequest createImportItemReviewReject(Long actorUserId, Long itemId) {
        return create(
            actorUserId,
            CapabilityOperationCode.IMPORT_ITEM_REVIEW_REJECT.code(),
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            itemId
        );
    }
}
