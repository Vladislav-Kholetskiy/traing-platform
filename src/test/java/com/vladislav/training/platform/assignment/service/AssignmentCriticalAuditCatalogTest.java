package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AssignmentCriticalAuditCatalog}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentCriticalAuditCatalogTest {

    @Test
    void assignmentCriticalAuditCatalogFixesMandatoryAnchorsAndCoverage() {
        assertThat(AssignmentCriticalAuditCatalog.values())
            .containsExactlyInAnyOrder(
                AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH,
                AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL,
                AssignmentCriticalAuditCatalog.ASSIGNMENT_DEADLINE_EXTEND,
                AssignmentCriticalAuditCatalog.ASSIGNMENT_REPLACE_WITH_NEW
            );

        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.operationCode())
            .isEqualTo(CapabilityOperationCode.ASSIGNMENT_CAMPAIGN_LAUNCH);
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.auditEntityType())
            .isEqualTo("assignment_campaign");
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.auditEventType().value())
            .isEqualTo("ASSIGNMENT_CAMPAIGN_LAUNCHED");
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.anchorsCampaignRoot()).isTrue();
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.requiresSynchronousCompanion()).isTrue();

        assertAdministrativeAuditAnchor(
            AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL,
            CapabilityOperationCode.ASSIGNMENT_CANCEL,
            "ASSIGNMENT_CANCELLED"
        );
        assertAdministrativeAuditAnchor(
            AssignmentCriticalAuditCatalog.ASSIGNMENT_DEADLINE_EXTEND,
            CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND,
            "ASSIGNMENT_DEADLINE_EXTENDED"
        );
        assertAdministrativeAuditAnchor(
            AssignmentCriticalAuditCatalog.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW,
            "ASSIGNMENT_REPLACED_WITH_NEW"
        );
    }

    @Test
    void criticalAuditCatalogDoesNotMixAuditWithOwnerHistoryOrReadSide() {
        assertThat(AssignmentCriticalAuditCatalog.values())
            .extracting(AssignmentCriticalAuditCatalog::auditEntityType)
            .doesNotContain("assignment_administrative_action", "assignment_campaign_recipient_snapshot", "audit_event");
    }

    @Test
    void criticalAuditCatalogResolvesSupportedOperationsAndRejectsForeignOnes() {
        assertThat(AssignmentCriticalAuditCatalog.isTrackedCriticalOperation(CapabilityOperationCode.ASSIGNMENT_CANCEL))
            .isTrue();
        assertThat(AssignmentCriticalAuditCatalog.forOperation(CapabilityOperationCode.ASSIGNMENT_CANCEL))
            .isEqualTo(AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL);

        assertThat(AssignmentCriticalAuditCatalog.isTrackedCriticalOperation(CapabilityOperationCode.CONTENT_PUBLISH))
            .isFalse();
        assertThatThrownBy(() -> AssignmentCriticalAuditCatalog.forOperation(CapabilityOperationCode.CONTENT_PUBLISH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported assignment contour critical audit operation");
    }

    private void assertAdministrativeAuditAnchor(
        AssignmentCriticalAuditCatalog catalogEntry,
        CapabilityOperationCode operationCode,
        String auditEventType
    ) {
        assertThat(catalogEntry.operationCode()).isEqualTo(operationCode);
        assertThat(catalogEntry.auditEntityType()).isEqualTo("assignment");
        assertThat(catalogEntry.auditEventType().value()).isEqualTo(auditEventType);
        assertThat(catalogEntry.anchorsAssignmentRoot()).isTrue();
        assertThat(catalogEntry.requiresSynchronousCompanion()).isTrue();
    }
}