package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentCampaignQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCampaignQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-08T10:00:00Z");

    @Mock
    private AssignmentCampaignReadRepository assignmentCampaignReadRepository;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    @Test
    void campaignRootPostLaunchReadUsesAssignmentCampaignContourThroughCanonicalPipeline() {
        AssignmentCampaignQueryServiceImpl service = new AssignmentCampaignQueryServiceImpl(
            assignmentCampaignReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign"
        );
        AssignmentCampaign campaign = new AssignmentCampaign(
            1L,
            "Запущенная кампания назначений",
            null,
            "ORG_UNIT",
            "unit-42",
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentCampaignReadRepository.findAllAssignmentCampaigns()).thenReturn(List.of(campaign));

        assertThat(service.findAllAssignmentCampaigns()).containsExactly(campaign);
        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentCampaignReadRepository).findAllAssignmentCampaigns();
    }

    @Test
    void campaignCoursePostLaunchReadRejectsDeniedAssignmentCampaignContour() {
        AssignmentCampaignQueryServiceImpl service = new AssignmentCampaignQueryServiceImpl(
            assignmentCampaignReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign_course"
        );
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign_course"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findAssignmentCampaignCoursesByCampaignId(10L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized to read post-launch assignment campaign data");

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign_course");
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(assignmentCampaignReadRepository);
    }

    @Test
    void recipientSnapshotDetailReadUsesPostLaunchCampaignContourAndReturnsHistoricalSnapshot() {
        AssignmentCampaignQueryServiceImpl service = new AssignmentCampaignQueryServiceImpl(
            assignmentCampaignReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign"
        );
        AssignmentCampaignRecipientSnapshot snapshot = recipientSnapshot(51L, 10L, 201L);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.DETAIL, "assignment_campaign"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotById(51L)).thenReturn(snapshot);

        assertThat(service.findAssignmentCampaignRecipientSnapshotById(51L)).isEqualTo(snapshot);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.DETAIL, "assignment_campaign");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentCampaignReadRepository).findAssignmentCampaignRecipientSnapshotById(51L);
    }

    @Test
    void recipientSnapshotCampaignReadUsesPersistedSnapshotContourWithoutPreviewRecompute() {
        AssignmentCampaignQueryServiceImpl service = new AssignmentCampaignQueryServiceImpl(
            assignmentCampaignReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign"
        );
        AssignmentCampaignRecipientSnapshot snapshot = recipientSnapshot(51L, 10L, 201L);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(10L))
            .thenReturn(List.of(snapshot));

        assertThat(service.findAssignmentCampaignRecipientSnapshotsByCampaignId(10L)).containsExactly(snapshot);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentCampaignReadRepository).findAssignmentCampaignRecipientSnapshotsByCampaignId(10L);
    }

    @Test
    void recipientSnapshotHistoricalReadReturnsStoredSnapshotFieldsWithoutLiveOrgFallback() {
        AssignmentCampaignQueryServiceImpl service = new AssignmentCampaignQueryServiceImpl(
            assignmentCampaignReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign"
        );
        AssignmentCampaignRecipientSnapshot snapshot = new AssignmentCampaignRecipientSnapshot(
            77L,
            10L,
            201L,
            901L,
            "/historical/company/unit-a",
            "ORG_UNIT_TARGETING",
            "EMP-201",
            "Historic Snapshot Name",
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.DETAIL, "assignment_campaign"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotById(77L)).thenReturn(snapshot);

        AssignmentCampaignRecipientSnapshot historicalRead = service.findAssignmentCampaignRecipientSnapshotById(77L);

        assertThat(historicalRead.organizationalUnitIdSnapshot()).isEqualTo(901L);
        assertThat(historicalRead.organizationalPathSnapshot()).isEqualTo("/historical/company/unit-a");
        assertThat(historicalRead.inclusionBasisCode()).isEqualTo("ORG_UNIT_TARGETING");
        assertThat(historicalRead.capturedAt()).isEqualTo(FIXED_INSTANT.minusSeconds(120));
        verify(assignmentCampaignReadRepository).findAssignmentCampaignRecipientSnapshotById(77L);
    }

    @Test
    void recipientSnapshotUserReadReturnsPersistedHistoricalSnapshotsAndAllowsEmptyResult() {
        AssignmentCampaignQueryServiceImpl service = new AssignmentCampaignQueryServiceImpl(
            assignmentCampaignReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign"
        );
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotsByUserId(201L))
            .thenReturn(List.of());

        assertThat(service.findAssignmentCampaignRecipientSnapshotsByUserId(201L)).isEmpty();

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, "assignment_campaign");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentCampaignReadRepository).findAssignmentCampaignRecipientSnapshotsByUserId(201L);
    }

    @Test
    void recipientSnapshotDetailReadPropagatesNotFoundFromNarrowReadRepository() {
        AssignmentCampaignQueryServiceImpl service = new AssignmentCampaignQueryServiceImpl(
            assignmentCampaignReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign"
        );
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.DETAIL, "assignment_campaign"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotById(99L))
            .thenThrow(new NotFoundException("Assignment campaign recipient snapshot not found: 99"));

        assertThatThrownBy(() -> service.findAssignmentCampaignRecipientSnapshotById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("recipient snapshot not found: 99");

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.DETAIL, "assignment_campaign");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentCampaignReadRepository).findAssignmentCampaignRecipientSnapshotById(99L);
    }

    private AssignmentCampaignRecipientSnapshot recipientSnapshot(Long snapshotId, Long campaignId, Long userId) {
        return new AssignmentCampaignRecipientSnapshot(
            snapshotId,
            campaignId,
            userId,
            301L,
            "/company/division/unit",
            "ORG_UNIT",
            "EMP-001",
            "Ivan Ivanov",
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
