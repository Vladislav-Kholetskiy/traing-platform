package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.common.exception.ValidationException;
import java.util.Objects;

final class AssignmentReplacementProvenancePolicy {

    Long resolveReplacementCampaignId(
        Assignment targetAssignment,
        AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand replacementCommand
    ) {
        Objects.requireNonNull(targetAssignment, "targetAssignment must not be null");
        Objects.requireNonNull(replacementCommand, "replacementCommand must not be null");

        Long replacementCampaignId = replacementCommand.campaignId();
        if (replacementCampaignId == null) {
            throw new ValidationException("REPLACE_WITH_NEW requires explicit campaignId provenance");
        }
        if (!Objects.equals(targetAssignment.campaignId(), replacementCampaignId)) {
            throw new ValidationException(
                "REPLACE_WITH_NEW must not choose ad hoc campaignId provenance for assignment: "
                    + targetAssignment.id()
            );
        }
        return replacementCampaignId;
    }
}
