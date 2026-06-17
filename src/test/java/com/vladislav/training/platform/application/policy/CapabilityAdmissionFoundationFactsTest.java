package com.vladislav.training.platform.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignLaunchFoundationStateReadService;
import com.vladislav.training.platform.testing.admission.SelfExecutionAdmissionFoundationStateReadService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code CapabilityAdmissionFoundationFacts}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class CapabilityAdmissionFoundationFactsTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-07T10:00:00Z");

    @Test
    void extractsLaunchAdmissionAnchorFromLaunchPayload() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH,
            CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN,
            null,
            new CapabilityAdmissionPayload.AssignmentCampaignLaunch("ORG_UNIT", "unit-42"),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);
        var launchAnchor = foundationFacts.assignmentCampaignLaunchAdmissionAnchor();

        assertThat(launchAnchor.sourceType()).isEqualTo("ORG_UNIT");
        assertThat(launchAnchor.sourceRef()).isEqualTo("unit-42");
    }

    @Test
    void extractsRequiredAssignmentIdForAdministrativeTarget() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CANCEL,
            CapabilityTargetEntityType.ASSIGNMENT,
            77L,
            CapabilityAdmissionPayload.AssignmentCancel.INSTANCE,
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);

        assertThat(foundationFacts.requiredAssignmentId()).isEqualTo(77L);
    }

    @Test
    void extractsRequiredCampaignIdForReplacementPayload() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            79L,
            new CapabilityAdmissionPayload.AssignmentReplaceWithNew(902L),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);
        var payload = foundationFacts.assignmentReplaceWithNew();

        assertThat(foundationFacts.requiredCampaignId(payload)).isEqualTo(902L);
    }

    @Test
    void resolvesLaunchFoundationStateThroughExistingNarrowReader() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH,
            CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN,
            null,
            new CapabilityAdmissionPayload.AssignmentCampaignLaunch("ORG_UNIT", "unit-42"),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(
            request,
            launchAdmissionAnchor -> new AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchFoundationState(
                "ORG_UNIT".equals(launchAdmissionAnchor.sourceType()) && "unit-42".equals(launchAdmissionAnchor.sourceRef())
            ),
            null,
            null,
            null
        );

        var foundationState = foundationFacts.resolveAssignmentCampaignLaunchFoundationState();

        assertThat(foundationState.sourceAnchorAlreadyMaterialized()).isTrue();
    }

    @Test
    void resolvesAdministrativeFoundationStateThroughExistingNarrowReader() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            79L,
            new CapabilityAdmissionPayload.AssignmentReplaceWithNew(902L),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(
            request,
            null,
            assignmentId -> new AssignmentAdministrativeAdmissionFoundationStateReadService
                .AssignmentAdministrativeAdmissionFoundationState(assignmentId, 901L, false, false),
            null,
            null
        );

        var foundationState = foundationFacts.resolveAssignmentAdministrativeFoundationState(79L);

        assertThat(foundationState.assignmentId()).isEqualTo(79L);
        assertThat(foundationFacts.requiredCurrentAssignmentCampaignId(foundationState)).isEqualTo(901L);
    }

    @Test
    void rejectsAdministrativeTargetWhenRequiredAssignmentIdIsMissing() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_CANCEL,
            CapabilityTargetEntityType.ASSIGNMENT,
            null,
            CapabilityAdmissionPayload.AssignmentCancel.INSTANCE,
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);

        assertThatThrownBy(foundationFacts::requiredAssignmentId)
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("required foundation fact: assignmentId");
    }

    @Test
    void rejectsReplacementExtractionWhenPayloadShapeDoesNotMatch() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            79L,
            CapabilityAdmissionPayload.AssignmentCancel.INSTANCE,
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);

        assertThatThrownBy(foundationFacts::assignmentReplaceWithNew)
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("requires payload type AssignmentReplaceWithNew");
    }

    @Test
    void extractsRequiredAssignmentAndAssignmentTestIdsForAssignedExecutionTarget() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);

        assertThat(foundationFacts.requiredAssignmentTestId()).isEqualTo(701L);
        assertThat(foundationFacts.requiredAssignmentId(foundationFacts.assignedExecution())).isEqualTo(77L);
    }

    @Test
    void resolvesAssignedExecutionFoundationStateThroughAssignmentOwnedReader() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(
            request,
            null,
            null,
            (actorUserId, assignmentId, assignmentTestId) ->
                new AssignmentAssignedExecutionAdmissionFoundationStateReadService
                    .AssignmentAssignedExecutionAdmissionFoundationState(
                        assignmentId,
                        assignmentTestId,
                        501L,
                        FIXED_INSTANT,
                        false,
                        false,
                        false
                    )
            ,
            null
        );

        var foundationState = foundationFacts.resolveAssignmentAssignedExecutionFoundationState();

        assertThat(foundationState.assignmentId()).isEqualTo(77L);
        assertThat(foundationState.assignmentTestId()).isEqualTo(701L);
        assertThat(foundationState.testId()).isEqualTo(501L);
    }

    @Test
    void rejectsAssignedExecutionWhenFoundationReaderReturnsUnavailableState() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(
            request,
            null,
            null,
            (actorUserId, assignmentId, assignmentTestId) -> null,
            null
        );

        assertThatThrownBy(foundationFacts::resolveAssignmentAssignedExecutionFoundationState)
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("Assigned execution foundation facts are unavailable");
    }

    @Test
    void extractsRequiredTestIdForSelfExecutionTarget() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);

        assertThat(foundationFacts.requiredSelfTestId()).isEqualTo(501L);
        assertThat(foundationFacts.selfExecution()).isSameAs(CapabilityAdmissionPayload.SelfExecution.INSTANCE);
    }

    @Test
    void resolvesSelfExecutionFoundationStateThroughTestingOwnedReader() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(
            request,
            null,
            null,
            null,
            (actorUserId, testId) -> new SelfExecutionAdmissionFoundationStateReadService.SelfExecutionAdmissionFoundationState(testId)
        );

        var foundationState = foundationFacts.resolveSelfExecutionFoundationState();

        assertThat(foundationState.testId()).isEqualTo(501L);
    }

    @Test
    void rejectsSelfExecutionWhenAssignmentShapedPayloadIsUsed() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            501L,
            new CapabilityAdmissionPayload.AssignedExecution(77L),
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(request);

        assertThatThrownBy(foundationFacts::selfExecution)
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("requires payload type SelfExecution");
    }

    @Test
    void rejectsSelfExecutionWhenFoundationReaderReturnsUnavailableState() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START,
            CapabilityTargetEntityType.TEST,
            501L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
            FIXED_INSTANT
        );

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(
            request,
            null,
            null,
            null,
            (actorUserId, testId) -> null
        );

        assertThatThrownBy(foundationFacts::resolveSelfExecutionFoundationState)
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("Self execution foundation facts are unavailable");
    }
}
