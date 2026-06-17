package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AssignmentCriticalAuditPayloadFactory {

    Map<String, Object> launchDetails(
        LaunchAssignmentCampaignCommand command,
        Long targetUnitId,
        int recipientCount,
        int assignmentCount,
        int assignmentTestCount
    ) {
        Map<String, Object> details = baseDetails("launch", null, false, true);
        details.put("targetingBasisType", command.targeting().basisType());
        details.put("targetingBasisRef", command.targeting().basisRef());
        details.put("targetUnitId", targetUnitId);
        details.put("materializedRecipientCount", recipientCount);
        details.put("materializedAssignmentCount", assignmentCount);
        details.put("materializedAssignmentTestCount", assignmentTestCount);
        return details;
    }

    Map<String, Object> launchPayloadAfter(
        AssignmentCampaign campaign,
        LaunchAssignmentCampaignCommand command,
        int recipientCount,
        int assignmentCount,
        int assignmentTestCount
    ) {
        Map<String, Object> payloadAfter = new LinkedHashMap<>();
        payloadAfter.put("campaign", campaignPayload(campaign));
        payloadAfter.put("command", launchCommandPayload(command));
        payloadAfter.put("materialization", materializationPayload(recipientCount, assignmentCount, assignmentTestCount));
        return payloadAfter;
    }

    Map<String, Object> administrativeDetails(
        String commandType,
        Instant occurredAt,
        boolean notePresent,
        Map<String, Object> commandSpecificDetails
    ) {
        Map<String, Object> details = baseDetails(commandType, occurredAt, notePresent, true);
        details.putAll(commandSpecificDetails);
        return details;
    }

    Map<String, Object> administrativePayloadAfter(
        Assignment assignmentAfter,
        AssignmentAdministrativeAction administrativeAction,
        Map<String, Object> commandPayload,
        Map<String, Object> relatedPayload
    ) {
        Map<String, Object> payloadAfter = assignmentPayload(assignmentAfter);
        payloadAfter.put("administrativeAction", administrativeActionPayload(administrativeAction));
        payloadAfter.put("command", commandPayload);
        if (relatedPayload != null) {
            payloadAfter.put("relatedAssignment", relatedPayload);
        }
        return payloadAfter;
    }

    Map<String, Object> assignmentPayload(Assignment assignment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assignmentId", assignment.id());
        payload.put("campaignId", assignment.campaignId());
        payload.put("userId", assignment.userId());
        payload.put("courseId", assignment.courseId());
        payload.put("status", assignment.status());
        payload.put("assignedAt", assignment.assignedAt());
        payload.put("deadlineAt", assignment.deadlineAt());
        payload.put("cancelledAt", assignment.cancelledAt());
        payload.put("closedAt", assignment.closedAt());
        return payload;
    }

    Map<String, Object> cancelCommandPayload(String note) {
        return notePayload(note);
    }

    Map<String, Object> extendDeadlineCommandPayload(Instant newDeadlineAt, String note) {
        Map<String, Object> payload = notePayload(note);
        payload.put("newDeadlineAt", newDeadlineAt);
        return payload;
    }

    Map<String, Object> replaceCommandPayload(String note) {
        return notePayload(note);
    }

    Map<String, Object> replacementAssignmentRelatedPayload(Assignment replacementAssignment) {
        return assignmentPayload(replacementAssignment);
    }

    private Map<String, Object> baseDetails(
        String commandType,
        Instant occurredAt,
        boolean notePresent,
        boolean statusRefreshIntegrated
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("commandType", commandType);
        details.put("occurredAt", occurredAt);
        details.put("notePresent", notePresent);
        details.put("statusRefreshIntegrated", statusRefreshIntegrated);
        return details;
    }

    private Map<String, Object> campaignPayload(AssignmentCampaign campaign) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignId", campaign.id());
        payload.put("name", campaign.name());
        payload.put("description", campaign.description());
        payload.put("sourceType", campaign.sourceType());
        payload.put("sourceRef", campaign.sourceRef());
        payload.put("sourceNameSnapshot", campaign.sourceNameSnapshot());
        return payload;
    }

    private Map<String, Object> launchCommandPayload(LaunchAssignmentCampaignCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("courseIds", List.copyOf(command.courseIds()));
        payload.put("targetingBasisType", command.targeting().basisType());
        payload.put("targetingBasisRef", command.targeting().basisRef());
        payload.put("deadlineAt", command.deadlinePolicy().deadlineAt());
        return payload;
    }

    private Map<String, Object> materializationPayload(
        int recipientCount,
        int assignmentCount,
        int assignmentTestCount
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientCount", recipientCount);
        payload.put("assignmentCount", assignmentCount);
        payload.put("assignmentTestCount", assignmentTestCount);
        return payload;
    }

    private Map<String, Object> administrativeActionPayload(AssignmentAdministrativeAction administrativeAction) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", administrativeAction.id());
        payload.put("type", administrativeAction.actionType());
        payload.put("occurredAt", administrativeAction.occurredAt());
        payload.put("note", administrativeAction.note());
        return payload;
    }

    private Map<String, Object> notePayload(String note) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("note", note);
        return payload;
    }
}
