package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code OrganizationalUnitSemanticMutationValidation}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationalUnitSemanticMutationValidationSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private OrganizationalUnitRepository organizationalUnitRepository;
    @Mock
    private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private UserOperatorStateSupport UserOperatorStateSupport;

    private OrganizationalUnitSemanticMutationValidationSupport support;

    @BeforeEach
    void setUp() {
        support = new OrganizationalUnitSemanticMutationValidationSupport(
            organizationalUnitRepository,
            userOrganizationAssignmentRepository,
            accessFoundationStateReadService,
            UserOperatorStateSupport
        );
    }

    @Test
    void unitTypeMutationAllowsCampaignCapabilityChangeWhenNoActiveDownstreamUsageExists() {
        OrganizationalUnitType currentUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, true, true, true, true);
        OrganizationalUnitType candidateUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, false, true, true, true);
        OrganizationalUnit activeUnit = unit(20L, 10L, OrganizationalUnitStatus.ACTIVE);

        when(organizationalUnitRepository.findUnitsByStatus(OrganizationalUnitStatus.ACTIVE)).thenReturn(List.of(activeUnit));
        when(accessFoundationStateReadService.findOrganizationalUnitUsage(20L, FIXED_INSTANT)).thenReturn(emptyUsage());
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(20L)).thenReturn(List.of());

        assertThatCode(() -> support.ensureUnitTypeMutationAllowed(currentUnitType, candidateUnitType, FIXED_INSTANT))
            .doesNotThrowAnyException();

        verifyNoInteractions(UserOperatorStateSupport);
    }

    @Test
    void unitTypeMutationRejectsActiveUserAccessAreaWhenResultingTypeRemovesCapability() {
        OrganizationalUnitType currentUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, true, true, true, true);
        OrganizationalUnitType candidateUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, true, true, true, false);
        OrganizationalUnit activeUnit = unit(20L, 10L, OrganizationalUnitStatus.ACTIVE);

        when(organizationalUnitRepository.findUnitsByStatus(OrganizationalUnitStatus.ACTIVE)).thenReturn(List.of(activeUnit));
        when(accessFoundationStateReadService.findOrganizationalUnitUsage(20L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.OrganizationalUnitAccessUsage(true, false, false, false, false, false)
        );

        assertThatThrownBy(() -> support.ensureUnitTypeMutationAllowed(currentUnitType, candidateUnitType, FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("user_access_area");
    }

    @Test
    void unitTypeMutationRejectsActiveTemporarySubtreeAccessAreaForFunctionalUnitWithoutSubtreeParticipation() {
        OrganizationalUnitType currentUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, true, true, true, true);
        OrganizationalUnitType candidateUnitType = unitType(10L, OrganizationalNodeKind.FUNCTIONAL, false, true, false, true, true);
        OrganizationalUnit activeUnit = unit(20L, 10L, OrganizationalUnitStatus.ACTIVE);

        when(organizationalUnitRepository.findUnitsByStatus(OrganizationalUnitStatus.ACTIVE)).thenReturn(List.of(activeUnit));
        when(accessFoundationStateReadService.findOrganizationalUnitUsage(20L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.OrganizationalUnitAccessUsage(false, false, false, true, true, false)
        );

        assertThatThrownBy(() -> support.ensureUnitTypeMutationAllowed(currentUnitType, candidateUnitType, FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("temporary_access_area")
            .hasMessageContaining("UNIT_SUBTREE");
    }

    @Test
    void unitTypeMutationRejectsActiveManagementRelationWhenResultingTypeRemovesCapability() {
        OrganizationalUnitType currentUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, true, true, true, true);
        OrganizationalUnitType candidateUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, true, true, false, true);
        OrganizationalUnit activeUnit = unit(20L, 10L, OrganizationalUnitStatus.ACTIVE);

        when(organizationalUnitRepository.findUnitsByStatus(OrganizationalUnitStatus.ACTIVE)).thenReturn(List.of(activeUnit));
        when(accessFoundationStateReadService.findOrganizationalUnitUsage(20L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.OrganizationalUnitAccessUsage(false, false, true, false, false, false)
        );

        assertThatThrownBy(() -> support.ensureUnitTypeMutationAllowed(currentUnitType, candidateUnitType, FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("management_relation");
    }

    @Test
    void unitReassignmentDelegatesOperatorContourPrimaryHomeValidation() {
        OrganizationalUnit currentUnit = unit(20L, 10L, OrganizationalUnitStatus.ACTIVE);
        OrganizationalUnitType candidateUnitType = unitType(11L, OrganizationalNodeKind.FUNCTIONAL, false, true, true, true, true);
        UserOrganizationAssignment primaryAssignment = assignment(301L, 20L, OrganizationAssignmentType.PRIMARY, FIXED_INSTANT.minusSeconds(60), null);

        when(accessFoundationStateReadService.findOrganizationalUnitUsage(20L, FIXED_INSTANT)).thenReturn(emptyUsage());
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(20L)).thenReturn(List.of(primaryAssignment));
        doThrow(new ConflictException("Operator home unit must reference LINEAR organizational unit: 20"))
            .when(UserOperatorStateSupport)
            .ensureCurrentPrimaryHomeUnitTypeAllowed(301L, FIXED_INSTANT, 20L, candidateUnitType);

        assertThatThrownBy(() -> support.ensureUnitTypeReassignmentAllowed(currentUnit, candidateUnitType, FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Operator home unit");

        verify(UserOperatorStateSupport).ensureCurrentPrimaryHomeUnitTypeAllowed(301L, FIXED_INSTANT, 20L, candidateUnitType);
    }

    @Test
    void unitReassignmentWithSameTypeIdIsNoOp() {
        OrganizationalUnit currentUnit = unit(20L, 10L, OrganizationalUnitStatus.ACTIVE);
        OrganizationalUnitType currentUnitType = unitType(10L, OrganizationalNodeKind.LINEAR, true, true, true, true, true);

        assertThatCode(() -> support.ensureUnitTypeReassignmentAllowed(currentUnit, currentUnitType, FIXED_INSTANT))
            .doesNotThrowAnyException();

        verifyNoInteractions(organizationalUnitRepository, userOrganizationAssignmentRepository, accessFoundationStateReadService, UserOperatorStateSupport);
    }

    private AccessFoundationStateReadService.OrganizationalUnitAccessUsage emptyUsage() {
        return new AccessFoundationStateReadService.OrganizationalUnitAccessUsage(false, false, false, false, false, false);
    }

    private OrganizationalUnitType unitType(
        Long id,
        OrganizationalNodeKind nodeKind,
        boolean canBeOperatorHomeUnit,
        boolean canBeCampaignTarget,
        boolean participatesInSubtreeScope,
        boolean canHaveManagementRelation,
        boolean canHaveAccessArea
    ) {
        return new OrganizationalUnitType(
            id,
            "TYPE-" + id,
            "Type " + id,
            null,
            nodeKind,
            canBeOperatorHomeUnit,
            canBeCampaignTarget,
            participatesInSubtreeScope,
            canHaveManagementRelation,
            canHaveAccessArea,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private OrganizationalUnit unit(Long id, Long organizationalUnitTypeId, OrganizationalUnitStatus status) {
        return new OrganizationalUnit(
            id,
            null,
            organizationalUnitTypeId,
            "Unit " + id,
            status,
            "/unit-" + id,
            0,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private UserOrganizationAssignment assignment(
        Long userId,
        Long organizationalUnitId,
        OrganizationAssignmentType assignmentType,
        Instant validFrom,
        Instant validTo
    ) {
        return new UserOrganizationAssignment(
            1L,
            userId,
            organizationalUnitId,
            assignmentType,
            validFrom,
            validTo,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}

