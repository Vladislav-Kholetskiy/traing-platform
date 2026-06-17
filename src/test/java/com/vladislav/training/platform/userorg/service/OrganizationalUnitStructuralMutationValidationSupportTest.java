package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code OrganizationalUnitStructuralMutationValidation}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationalUnitStructuralMutationValidationSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private UserOperatorStateSupport UserOperatorStateSupport;

    private OrganizationalUnitStructuralMutationValidationSupport support;

    @BeforeEach
    void setUp() {
        support = new OrganizationalUnitStructuralMutationValidationSupport(
            userOrganizationAssignmentRepository,
            accessFoundationStateReadService,
            UserOperatorStateSupport
        );
    }

    @Test
    void moveAllowsActiveSubtreeUnderActiveParent() {
        OrganizationalUnit root = unit(10L, OrganizationalUnitStatus.ACTIVE);
        OrganizationalUnit child = unit(11L, OrganizationalUnitStatus.ACTIVE);
        OrganizationalUnit targetParent = unit(20L, OrganizationalUnitStatus.ACTIVE);

        assertThatCode(() -> support.ensureMoveAllowed(List.of(root, child), targetParent))
            .doesNotThrowAnyException();
    }

    @Test
    void moveRejectsArchivedParentWhenSubtreeContainsActiveNode() {
        OrganizationalUnit root = unit(10L, OrganizationalUnitStatus.ACTIVE);
        OrganizationalUnit targetParent = unit(20L, OrganizationalUnitStatus.ARCHIVED);

        assertThatThrownBy(() -> support.ensureMoveAllowed(List.of(root), targetParent))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Archived organizational unit cannot be parent");
    }

    @Test
    void archiveRejectsActiveOrganizationAssignment() {
        OrganizationalUnit root = unit(10L, OrganizationalUnitStatus.ACTIVE);
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(10L)).thenReturn(List.of(
            assignment(101L, 10L, OrganizationAssignmentType.SECONDARY, FIXED_INSTANT.minusSeconds(60), null)
        ));

        assertThatThrownBy(() -> support.ensureArchiveAllowed(List.of(root), FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("user_organization_assignment");

        verify(accessFoundationStateReadService, never()).findOrganizationalUnitUsage(10L, FIXED_INSTANT);
    }

    @Test
    void archiveRejectsOperatorContourPrimaryHomeUnit() {
        OrganizationalUnit root = unit(10L, OrganizationalUnitStatus.ACTIVE);
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(10L)).thenReturn(List.of(
            assignment(101L, 10L, OrganizationAssignmentType.PRIMARY, FIXED_INSTANT.minusSeconds(60), null)
        ));
        when(UserOperatorStateSupport.isUserInOperatorContour(101L, FIXED_INSTANT)).thenReturn(true);

        assertThatThrownBy(() -> support.ensureArchiveAllowed(List.of(root), FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("PRIMARY home unit")
            .hasMessageContaining("operator-contour");
    }

    @Test
    void archiveRejectsActiveTemporaryManagementDelegation() {
        OrganizationalUnit root = unit(10L, OrganizationalUnitStatus.ACTIVE);
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(10L)).thenReturn(List.of());
        when(accessFoundationStateReadService.findOrganizationalUnitUsage(10L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.OrganizationalUnitAccessUsage(false, false, false, false, false, true)
        );

        assertThatThrownBy(() -> support.ensureArchiveAllowed(List.of(root), FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("temporary_management_delegation");
    }

    @Test
    void archiveAllowsSubtreeWithoutActiveDownstreamState() {
        OrganizationalUnit root = unit(10L, OrganizationalUnitStatus.ACTIVE);
        OrganizationalUnit child = unit(11L, OrganizationalUnitStatus.ARCHIVED);
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(10L)).thenReturn(List.of());
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(11L)).thenReturn(List.of());
        when(accessFoundationStateReadService.findOrganizationalUnitUsage(10L, FIXED_INSTANT)).thenReturn(emptyUsage());
        when(accessFoundationStateReadService.findOrganizationalUnitUsage(11L, FIXED_INSTANT)).thenReturn(emptyUsage());

        assertThatCode(() -> support.ensureArchiveAllowed(List.of(root, child), FIXED_INSTANT))
            .doesNotThrowAnyException();

        verifyNoInteractions(UserOperatorStateSupport);
    }

    private AccessFoundationStateReadService.OrganizationalUnitAccessUsage emptyUsage() {
        return new AccessFoundationStateReadService.OrganizationalUnitAccessUsage(false, false, false, false, false, false);
    }

    private OrganizationalUnit unit(Long id, OrganizationalUnitStatus status) {
        return new OrganizationalUnit(id, null, 10L, "Unit " + id, status, "/unit-" + id, 0, null, FIXED_INSTANT, FIXED_INSTANT);
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

