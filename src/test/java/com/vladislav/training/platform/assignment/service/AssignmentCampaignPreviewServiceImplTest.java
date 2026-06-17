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
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.OrganizationalTargetingQueryService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentCampaignPreviewServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCampaignPreviewServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-09T10:00:00Z");

    @Mock
    private OrganizationQueryService organizationQueryService;
    @Mock
    private OrganizationalTargetingQueryService organizationalTargetingQueryService;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService;
    @Mock
    private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    private MandatoryAssignmentRecipientEligibilityService mandatoryRecipientEligibilitySeam() {
        return new MandatoryAssignmentRecipientEligibilityService(
            organizationQueryService,
            userQueryService,
            userOrgFoundationStateReadService,
            temporaryRoleAssignmentReadService,
            userOrganizationAssignmentRepository
        );
    }

    @Test
    void previewRecipientPoolComputesFromCurrentTargetingCandidatesWithoutPersistenceArtifacts() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("ORG_UNIT", "/company/ops");
        AccessPolicyQueryContext context = previewContext();
        OrganizationalUnit targetUnit = organizationalUnit(41L, OrganizationalUnitStatus.ACTIVE, "/company/ops", "Operations");

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops")).thenReturn(targetUnit);
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree(
            "/company/ops",
            FIXED_INSTANT
        )).thenReturn(Set.of(201L, 202L));
        stubEligibleRecipient(201L, 301L, "/company/ops/line-1", "E-201", "Petrov", "Petr");
        stubEligibleRecipient(202L, 302L, "/company/ops/line-2", "E-202", "Ivanov", "Ivan");

        AssignmentCampaignPreviewService.AssignmentCampaignRecipientPoolPreview preview =
            service.previewRecipientPool(request);

        assertThat(preview.sourceType()).isEqualTo("ORG_UNIT");
        assertThat(preview.sourceRef()).isEqualTo("/company/ops");
        assertThat(preview.targetingUnitId()).isEqualTo(41L);
        assertThat(preview.targetingUnitPath()).isEqualTo("/company/ops");
        assertThat(preview.targetingUnitName()).isEqualTo("Operations");
        assertThat(preview.targetingBasisActive()).isTrue();
        assertThat(preview.previewedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(preview.recipients())
            .extracting(AssignmentCampaignPreviewService.PreviewRecipient::userId)
            .containsExactly(201L, 202L);

        verify(contextResolver).resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verify(organizationQueryService).findOrganizationalUnitByPath("/company/ops");
        verify(organizationalTargetingQueryService).resolveCurrentCandidateUserIdsForUnitSubtree(
            "/company/ops",
            FIXED_INSTANT
        );
        verify(userQueryService).findUserById(201L);
        verify(userQueryService).findUserById(202L);
    }

    @Test
    void previewRecipientPoolRemainsPolicyAwareAndFailClosedBeforeTargetingExpansion() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("ORG_UNIT", "/company/ops");
        AccessPolicyQueryContext context = previewContext();

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.previewRecipientPool(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized to preview assignment campaign recipient pool");

        verify(contextResolver).resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(
            organizationQueryService,
            organizationalTargetingQueryService,
            userQueryService,
            userOrgFoundationStateReadService,
            temporaryRoleAssignmentReadService,
            userOrganizationAssignmentRepository
        );
    }

    @Test
    void previewRecipientPoolStaysEphemeralWhenTargetingBasisIsArchived() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("ORG_UNIT", "/company/archive");
        AccessPolicyQueryContext context = previewContext();
        OrganizationalUnit targetUnit = organizationalUnit(
            42L,
            OrganizationalUnitStatus.ARCHIVED,
            "/company/archive",
            "Archive"
        );

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(organizationQueryService.findOrganizationalUnitByPath("/company/archive")).thenReturn(targetUnit);

        AssignmentCampaignPreviewService.AssignmentCampaignRecipientPoolPreview preview =
            service.previewRecipientPool(request);

        assertThat(preview.targetingBasisActive()).isFalse();
        assertThat(preview.recipients()).isEmpty();
        assertThat(preview.targetingUnitId()).isEqualTo(42L);

        verify(organizationQueryService).findOrganizationalUnitByPath("/company/archive");
        verifyNoInteractions(
            organizationalTargetingQueryService,
            userQueryService,
            userOrgFoundationStateReadService,
            temporaryRoleAssignmentReadService,
            userOrganizationAssignmentRepository
        );
    }

    @Test
    void previewRecipientPoolRejectsUnsupportedTargetingBasisBeforeAnyPersistenceLikeFlowAppears() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("POSITION", "line-operators");
        AccessPolicyQueryContext context = previewContext();

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);

        assertThatThrownBy(() -> service.previewRecipientPool(request))
            .isInstanceOf(PolicyViolationException.class);

        verify(contextResolver).resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        );
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(
            organizationQueryService,
            organizationalTargetingQueryService,
            userQueryService,
            userOrgFoundationStateReadService,
            temporaryRoleAssignmentReadService,
            userOrganizationAssignmentRepository
        );
    }

    @Test
    void previewExcludesNonOperatorRecipientRejectedByMandatoryLaunchEligibility() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("ORG_UNIT", "/company/ops");
        AccessPolicyQueryContext context = previewContext();
        OrganizationalUnit targetUnit = organizationalUnit(41L, OrganizationalUnitStatus.ACTIVE, "/company/ops", "Operations");

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops")).thenReturn(targetUnit);
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree(
            "/company/ops",
            FIXED_INSTANT
        )).thenReturn(Set.of(201L));
        when(userQueryService.findUserById(201L)).thenReturn(activeUser(201L, "E-201", "NonOperator", "Recipient"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(201L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(201L, true, Set.of("EMPLOYEE"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(201L, FIXED_INSTANT))
            .thenReturn(List.of());

        AssignmentCampaignPreviewService.AssignmentCampaignRecipientPoolPreview preview =
            service.previewRecipientPool(request);

        assertThat(preview.recipients())
            
            .isEmpty();
    }

    @Test
    void previewExcludesUserWithoutValidActivePrimaryHomeUnit() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("ORG_UNIT", "/company/ops");
        AccessPolicyQueryContext context = previewContext();
        OrganizationalUnit targetUnit = organizationalUnit(41L, OrganizationalUnitStatus.ACTIVE, "/company/ops", "Operations");

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops")).thenReturn(targetUnit);
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree(
            "/company/ops",
            FIXED_INSTANT
        )).thenReturn(Set.of(202L));
        when(userQueryService.findUserById(202L)).thenReturn(activeUser(202L, "E-202", "NoPrimary", "Recipient"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(202L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(202L, true, Set.of("OPERATOR"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(202L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(202L, FIXED_INSTANT))
            .thenReturn(List.of());

        AssignmentCampaignPreviewService.AssignmentCampaignRecipientPoolPreview preview =
            service.previewRecipientPool(request);

        assertThat(preview.recipients())
            
            .isEmpty();
    }

    @Test
    void previewExcludesUserWithInvalidHomeUnitSemanticsForMandatoryAssignmentContour() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("ORG_UNIT", "/company/ops");
        AccessPolicyQueryContext context = previewContext();
        OrganizationalUnit targetUnit = organizationalUnit(41L, OrganizationalUnitStatus.ACTIVE, "/company/ops", "Operations");

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops")).thenReturn(targetUnit);
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree(
            "/company/ops",
            FIXED_INSTANT
        )).thenReturn(Set.of(203L));
        when(userQueryService.findUserById(203L)).thenReturn(activeUser(203L, "E-203", "InvalidHomeUnit", "Recipient"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(203L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(203L, true, Set.of("OPERATOR"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(203L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(203L, FIXED_INSTANT))
            .thenReturn(List.of(primary(203L, 303L)));
        when(organizationQueryService.findOrganizationalUnitById(303L))
            .thenReturn(organizationalUnit(303L, OrganizationalUnitStatus.ACTIVE, "/company/ops/line-3", "Line 3"));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L))
            .thenReturn(organizationalUnitType(7L, false));

        AssignmentCampaignPreviewService.AssignmentCampaignRecipientPoolPreview preview =
            service.previewRecipientPool(request);

        assertThat(preview.recipients())
            
            .isEmpty();
    }

    @Test
    void previewAndLaunchStayAlignedOnCriticalMandatoryEligibilitySemantics() {
        AssignmentCampaignPreviewServiceImpl service = new AssignmentCampaignPreviewServiceImpl(
            organizationQueryService,
            organizationalTargetingQueryService,
            mandatoryRecipientEligibilitySeam(),
            accessSpecificationPolicy,
            contextResolver
        );
        AssignmentCampaignPreviewService.RecipientPoolPreviewRequest request =
            new AssignmentCampaignPreviewService.RecipientPoolPreviewRequest("ORG_UNIT", "/company/ops");
        AccessPolicyQueryContext context = previewContext();
        OrganizationalUnit targetUnit = organizationalUnit(41L, OrganizationalUnitStatus.ACTIVE, "/company/ops", "Operations");

        when(contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            "assignment_campaign_preview"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(organizationQueryService.findOrganizationalUnitByPath("/company/ops")).thenReturn(targetUnit);
        when(organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree(
            "/company/ops",
            FIXED_INSTANT
        )).thenReturn(Set.of(201L, 202L, 203L, 204L));
        stubEligibleRecipient(201L, 301L, "/company/ops/line-1", "E-201", "Eligible", "Recipient");
        when(userQueryService.findUserById(202L)).thenReturn(activeUser(202L, "E-202", "NonOperator", "Recipient"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(202L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(202L, true, Set.of("EMPLOYEE"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(202L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userQueryService.findUserById(203L)).thenReturn(activeUser(203L, "E-203", "NoPrimary", "Recipient"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(203L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(203L, true, Set.of("OPERATOR"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(203L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(203L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userQueryService.findUserById(204L)).thenReturn(activeUser(204L, "E-204", "InvalidHomeUnit", "Recipient"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(204L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(204L, true, Set.of("OPERATOR"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(204L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(204L, FIXED_INSTANT))
            .thenReturn(List.of(primary(204L, 304L)));
        when(organizationQueryService.findOrganizationalUnitById(304L))
            .thenReturn(new OrganizationalUnit(
                304L,
                11L,
                8L,
                "Line 4",
                OrganizationalUnitStatus.ACTIVE,
                "/company/ops/line-4",
                2,
                "ou-304",
                FIXED_INSTANT,
                FIXED_INSTANT
            ));
        when(organizationQueryService.findOrganizationalUnitTypeById(8L))
            .thenReturn(new OrganizationalUnitType(
                8L,
                "STAFF",
                "Staff",
                null,
                OrganizationalNodeKind.FUNCTIONAL,
                false,
                true,
                true,
                false,
                false,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));

        AssignmentCampaignPreviewService.AssignmentCampaignRecipientPoolPreview preview =
            service.previewRecipientPool(request);

        assertThat(preview.recipients())
            
            .extracting(AssignmentCampaignPreviewService.PreviewRecipient::userId)
            .containsExactly(201L);
    }

    private AccessPolicyQueryContext previewContext() {
        return new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign_preview"
        );
    }

    private OrganizationalUnit organizationalUnit(
        Long unitId,
        OrganizationalUnitStatus status,
        String path,
        String name
    ) {
        return new OrganizationalUnit(
            unitId,
            11L,
            7L,
            name,
            status,
            path,
            2,
            "ou-" + unitId,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private void stubEligibleRecipient(
        Long userId,
        Long homeUnitId,
        String homeUnitPath,
        String employeeNumber,
        String lastName,
        String firstName
    ) {
        when(userQueryService.findUserById(userId)).thenReturn(activeUser(userId, employeeNumber, lastName, firstName));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(userId, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(userId, true, Set.of("OPERATOR"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(userId, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(userId, FIXED_INSTANT))
            .thenReturn(List.of(primary(userId, homeUnitId)));
        when(organizationQueryService.findOrganizationalUnitById(homeUnitId))
            .thenReturn(organizationalUnit(homeUnitId, OrganizationalUnitStatus.ACTIVE, homeUnitPath, "Line " + homeUnitId));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L))
            .thenReturn(organizationalUnitType(7L, true));
    }

    private UserOrganizationAssignment primary(Long userId, Long organizationalUnitId) {
        return new UserOrganizationAssignment(
            null,
            userId,
            organizationalUnitId,
            OrganizationAssignmentType.PRIMARY,
            FIXED_INSTANT.minusSeconds(3600),
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
    }

    private OrganizationalUnitType organizationalUnitType(Long typeId, boolean canBeOperatorHomeUnit) {
        return new OrganizationalUnitType(
            typeId,
            "LINE",
            "Line",
            null,
            OrganizationalNodeKind.LINEAR,
            canBeOperatorHomeUnit,
            true,
            true,
            false,
            false,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AppUser activeUser(Long userId, String employeeNumber, String lastName, String firstName) {
        return new AppUser(
            userId,
            employeeNumber,
            "user-" + userId,
            lastName,
            firstName,
            null,
            UserStatus.ACTIVE,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}

