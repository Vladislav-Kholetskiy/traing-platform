package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.common.exception.ValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AssignmentReplacementProvenancePolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentReplacementProvenancePolicyTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-04-10T12:00:00Z");

    @Test
    void replacementWithMatchingCampaignIdUsesCanonicalProvenanceAnchor() {
        AssignmentReplacementProvenancePolicy policy = new AssignmentReplacementProvenancePolicy();
        Assignment targetAssignment = assignment(77L, 800L);

        Long replacementCampaignId = policy.resolveReplacementCampaignId(
            targetAssignment,
            replacementCommand(77L, 800L, OCCURRED_AT.plusSeconds(7200))
        );

        assertThat(replacementCampaignId).isEqualTo(800L);
    }

    @Test
    void replacementWithoutCampaignIdFailsClosed() {
        AssignmentReplacementProvenancePolicy policy = new AssignmentReplacementProvenancePolicy();
        Assignment targetAssignment = assignment(77L, 800L);

        assertThatThrownBy(() -> policy.resolveReplacementCampaignId(
            targetAssignment,
            replacementCommand(77L, null, OCCURRED_AT.plusSeconds(7200))
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("campaignId must not be null");
    }

    @Test
    void adHocProvenanceChoiceIsRejectedExplicitly() {
        AssignmentReplacementProvenancePolicy policy = new AssignmentReplacementProvenancePolicy();
        Assignment targetAssignment = assignment(77L, 800L);

        assertThatThrownBy(() -> policy.resolveReplacementCampaignId(
            targetAssignment,
            replacementCommand(77L, 900L, OCCURRED_AT.plusSeconds(7200))
        ))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must not choose ad hoc campaignId provenance");
    }

    private Assignment assignment(Long assignmentId, Long campaignId) {
        return new Assignment(
            assignmentId,
            campaignId,
            101L,
            301L,
            AssignmentStatus.ASSIGNED,
            OCCURRED_AT.minusSeconds(3600),
            OCCURRED_AT.plusSeconds(3600),
            null,
            null,
            OCCURRED_AT.minusSeconds(3660),
            OCCURRED_AT.minusSeconds(3660)
        );
    }

    private AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand replacementCommand(
        Long assignmentId,
        Long campaignId,
        Instant deadlineAt
    ) {
        return new AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand(
            assignmentId,
            campaignId,
            deadlineAt,
            null
        );
    }
}
