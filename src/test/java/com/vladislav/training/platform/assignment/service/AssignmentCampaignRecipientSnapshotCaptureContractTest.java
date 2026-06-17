package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AssignmentCampaignRecipientSnapshotCapture}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AssignmentCampaignRecipientSnapshotCaptureContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-12T09:00:00Z");

    @Test
    void captureMaterializesCanonicalHistoricalSnapshotFieldsExplicitly() {
        AssignmentCampaignRecipientSnapshotCaptureContract contract =
            new AssignmentCampaignRecipientSnapshotCaptureContract();

        var snapshot = contract.capture(
            10L,
            201L,
            301L,
            "/company/ops/line-1",
            "E-201",
            "Petrov Petr",
            "ORG_UNIT_TARGETING",
            FIXED_INSTANT
        );

        assertThat(snapshot.campaignId()).isEqualTo(10L);
        assertThat(snapshot.userId()).isEqualTo(201L);
        assertThat(snapshot.organizationalUnitIdSnapshot()).isEqualTo(301L);
        assertThat(snapshot.organizationalPathSnapshot()).isEqualTo("/company/ops/line-1");
        assertThat(snapshot.inclusionBasisCode()).isEqualTo("ORG_UNIT_TARGETING");
        assertThat(snapshot.capturedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(snapshot.createdAt()).isEqualTo(FIXED_INSTANT);
    }
}
