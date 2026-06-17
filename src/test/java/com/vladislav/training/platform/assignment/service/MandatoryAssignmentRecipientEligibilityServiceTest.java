package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
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
 * Проверяет поведение сервиса {@code MandatoryAssignmentRecipientEligibility}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class MandatoryAssignmentRecipientEligibilityServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-12T08:00:00Z");

    @Mock private OrganizationQueryService organizationQueryService;
    @Mock private UserQueryService userQueryService;
    @Mock private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock private TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService;
    @Mock private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;

    @Test
    void seamAcceptsEligibleRecipientForMandatoryAssignmentContour() {
        MandatoryAssignmentRecipientEligibilityService seam = seam();
        stubEligibleRecipient(201L, 301L, "/company/ops/line-1");

        MandatoryAssignmentRecipientEligibilityService.MandatoryRecipientEligibility eligibility =
            seam.evaluateRecipient(201L, "/company/ops", FIXED_INSTANT);

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.userId()).isEqualTo(201L);
        assertThat(eligibility.organizationalUnitIdSnapshot()).isEqualTo(301L);
        assertThat(eligibility.organizationalPathSnapshot()).isEqualTo("/company/ops/line-1");
        assertThat(eligibility.fullNameSnapshot()).isEqualTo("Petrov Petr");
    }

    @Test
    void seamAcceptsPersonnelProvisionedOperatorAlias() {
        MandatoryAssignmentRecipientEligibilityService seam = seam();
        when(userQueryService.findUserById(203L)).thenReturn(activeUser(203L, "E-203", "Sidorov", "Stepan"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(203L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(203L, true, Set.of("ROLE_OPERATIONS"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(203L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(203L, FIXED_INSTANT))
            .thenReturn(List.of(primary(203L, 303L)));
        when(organizationQueryService.findOrganizationalUnitById(303L))
            .thenReturn(unit(303L, 7L, OrganizationalUnitStatus.ACTIVE, "/company/ops/line-2"));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L))
            .thenReturn(type(7L, true));

        MandatoryAssignmentRecipientEligibilityService.MandatoryRecipientEligibility eligibility =
            seam.evaluateRecipient(203L, "/company/ops", FIXED_INSTANT);

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.organizationalPathSnapshot()).isEqualTo("/company/ops/line-2");
    }

    @Test
    void seamReturnsSameCanonicalIneligibilityForPreviewFilteringAndLaunchFailClosed() {
        MandatoryAssignmentRecipientEligibilityService seam = seam();
        when(userQueryService.findUserById(202L)).thenReturn(activeUser(202L, "E-202", "NonOperator", "Recipient"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(202L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(202L, true, Set.of("EMPLOYEE"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(202L, FIXED_INSTANT))
            .thenReturn(List.of());

        MandatoryAssignmentRecipientEligibilityService.MandatoryRecipientEligibility eligibility =
            seam.evaluateRecipient(202L, "/company/ops", FIXED_INSTANT);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.ineligibilityReason())
            .isEqualTo(MandatoryAssignmentRecipientEligibilityService.IneligibilityReason.NOT_ADMISSIBLE_MANDATORY_OPERATOR);
        assertThatThrownBy(eligibility::requireEligibleForLaunch)
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("mandatory-assignment operator");
    }

    @Test
    void seamKeepsTargetingMatrixStepUnchangedForOrgUnitOnlyScope() {
        assertThat(AssignmentCampaignTargetingSupport.supportedBasisTypes()).containsExactly("ORG_UNIT");
    }

    private MandatoryAssignmentRecipientEligibilityService seam() {
        return new MandatoryAssignmentRecipientEligibilityService(
            organizationQueryService,
            userQueryService,
            userOrgFoundationStateReadService,
            temporaryRoleAssignmentReadService,
            userOrganizationAssignmentRepository
        );
    }

    private void stubEligibleRecipient(Long userId, Long homeUnitId, String homeUnitPath) {
        when(userQueryService.findUserById(userId)).thenReturn(activeUser(userId, "E-" + userId, "Petrov", "Petr"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(userId, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(userId, true, Set.of("OPERATOR"))
        );
        when(temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(userId, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(userId, FIXED_INSTANT))
            .thenReturn(List.of(primary(userId, homeUnitId)));
        when(organizationQueryService.findOrganizationalUnitById(homeUnitId))
            .thenReturn(unit(homeUnitId, 7L, OrganizationalUnitStatus.ACTIVE, homeUnitPath));
        when(organizationQueryService.findOrganizationalUnitTypeById(7L))
            .thenReturn(type(7L, true));
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

    private OrganizationalUnit unit(Long id, Long typeId, OrganizationalUnitStatus status, String path) {
        return new OrganizationalUnit(id, 11L, typeId, "Unit " + id, status, path, 2, "ou-" + id, FIXED_INSTANT, FIXED_INSTANT);
    }

    private OrganizationalUnitType type(Long id, boolean canBeOperatorHomeUnit) {
        return new OrganizationalUnitType(
            id,
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
}


