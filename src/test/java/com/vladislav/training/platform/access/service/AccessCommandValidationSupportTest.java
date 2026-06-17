package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.access.repository.ManagementRelationTypeRepository;
import com.vladislav.training.platform.access.repository.TemporaryAccessAreaRepository;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
import com.vladislav.training.platform.access.repository.TemporaryRoleAssignmentRepository;
import com.vladislav.training.platform.access.repository.UserAccessAreaRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.service.UserOperatorStateValidationService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code AccessCommandValidation}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class AccessCommandValidationSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-01T12:00:00Z");

    @Mock
    private UserAccessAreaRepository userAccessAreaRepository;
    @Mock
    private ManagementRelationRepository managementRelationRepository;
    @Mock
    private TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository;
    @Mock
    private TemporaryAccessAreaRepository temporaryAccessAreaRepository;
    @Mock
    private TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;
    @Mock
    private ManagementRelationTypeRepository managementRelationTypeRepository;
    @Mock
    private UserOperatorStateValidationService UserOperatorStateValidationService;
    @Mock
    private AccessManagementTargetValidationSupport accessManagementTargetValidationSupport;
    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    @Test
    void ensureTemporaryRoleAssignableRejectsInactiveUserBeforeRoleLookupOverlapAndOperatorChecks() {
        AccessCommandValidationSupport support = support();
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(7L, FIXED_INSTANT))
                .thenReturn(inactiveUserState(7L));

        assertThatThrownBy(() -> support.ensureTemporaryRoleAssignable(
                temporaryRoleAssignment(7L, 900L, FIXED_INSTANT)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INACTIVE user cannot receive new active temporary role assignment");

        verify(userOrgFoundationStateReadService).findUserAccessPolicyFoundationState(7L, FIXED_INSTANT);
        verify(userOrgFoundationStateReadService, never()).findRoleCodesByIds(List.of(900L));
        verifyNoInteractions(temporaryRoleAssignmentRepository);
        verifyNoInteractions(UserOperatorStateValidationService);
    }

    @Test
    void ensureTemporaryRoleAssignableUsesOwnerRoleExistenceAndOperatorContourForActiveUser() {
        AccessCommandValidationSupport support = support();
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(7L, FIXED_INSTANT))
                .thenReturn(activeUserState(7L));
        when(userOrgFoundationStateReadService.findRoleCodesByIds(List.of(900L)))
                .thenReturn(Set.of("OPERATOR"));
        when(temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentsByUserId(7L)).thenReturn(List.of());
        when(temporaryRoleAssignmentRepository.findActiveTemporaryRoleAssignmentsByUserId(7L, FIXED_INSTANT))
                .thenReturn(List.of());

        support.ensureTemporaryRoleAssignable(
                temporaryRoleAssignment(7L, 900L, FIXED_INSTANT)
        );

        verify(userOrgFoundationStateReadService).findUserAccessPolicyFoundationState(7L, FIXED_INSTANT);
        verify(userOrgFoundationStateReadService).findRoleCodesByIds(List.of(900L));
        verify(temporaryRoleAssignmentRepository).findTemporaryRoleAssignmentsByUserId(7L);
        verify(temporaryRoleAssignmentRepository).findActiveTemporaryRoleAssignmentsByUserId(7L, FIXED_INSTANT);
        verify(UserOperatorStateValidationService).ensureResultingTemporaryRoleStateConsistent(
                7L,
                FIXED_INSTANT,
                List.of(900L)
        );
    }

    @Test
    void ensureTemporaryAccessAreaAssignableRejectsInactiveUserBeforeTargetAndHistoryChecks() {
        AccessCommandValidationSupport support = support();
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(7L, FIXED_INSTANT))
                .thenReturn(inactiveUserState(7L));

        assertThatThrownBy(() -> support.ensureTemporaryAccessAreaAssignable(
                temporaryAccessArea(7L, 30L, AccessScopeType.UNIT_ONLY, FIXED_INSTANT)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INACTIVE user cannot receive new active temporary access area");

        verify(userOrgFoundationStateReadService).findUserAccessPolicyFoundationState(7L, FIXED_INSTANT);
        verifyNoInteractions(accessManagementTargetValidationSupport);
        verify(temporaryAccessAreaRepository, never()).findTemporaryAccessAreasByUserId(anyLong());
    }

    @Test
    void ensureTemporaryAccessAreaAssignableUsesOwnerTargetValidationForActiveUser() {
        AccessCommandValidationSupport support = support();
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(7L, FIXED_INSTANT))
                .thenReturn(activeUserState(7L));
        when(temporaryAccessAreaRepository.findTemporaryAccessAreasByUserId(7L)).thenReturn(List.of());

        support.ensureTemporaryAccessAreaAssignable(
                temporaryAccessArea(7L, 30L, AccessScopeType.UNIT_SUBTREE, FIXED_INSTANT)
        );

        verify(userOrgFoundationStateReadService).findUserAccessPolicyFoundationState(7L, FIXED_INSTANT);
        verify(accessManagementTargetValidationSupport).ensureAccessAreaTargetAllowed(30L, AccessScopeType.UNIT_SUBTREE);
        verify(temporaryAccessAreaRepository).findTemporaryAccessAreasByUserId(7L);
    }

    @Test
    void ensureTemporaryManagementAssignableUsesOwnerReferenceAndTargetValidationForActiveUser() {
        AccessCommandValidationSupport support = support();
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(7L, FIXED_INSTANT))
                .thenReturn(activeUserState(7L));
        when(managementRelationTypeRepository.findManagementRelationTypeById(500L))
                .thenReturn(new ManagementRelationType(
                        500L,
                        "LINE_MANAGER",
                        "Line Manager",
                        null,
                        FIXED_INSTANT.minusSeconds(3600),
                        FIXED_INSTANT.minusSeconds(3600)
                ));
        when(temporaryManagementDelegationRepository.findTemporaryManagementDelegationsByUserId(7L))
                .thenReturn(List.of());

        support.ensureTemporaryManagementAssignable(
                temporaryManagementDelegation(7L, 30L, 500L, FIXED_INSTANT)
        );

        verify(userOrgFoundationStateReadService).findUserAccessPolicyFoundationState(7L, FIXED_INSTANT);
        verify(managementRelationTypeRepository).findManagementRelationTypeById(500L);
        verify(accessManagementTargetValidationSupport).ensureManagementRelationTargetAllowed(30L);
        verify(temporaryManagementDelegationRepository).findTemporaryManagementDelegationsByUserId(7L);
    }

    private AccessCommandValidationSupport support() {
        return new AccessCommandValidationSupport(
                userAccessAreaRepository,
                managementRelationRepository,
                temporaryRoleAssignmentRepository,
                temporaryAccessAreaRepository,
                temporaryManagementDelegationRepository,
                managementRelationTypeRepository,
                UserOperatorStateValidationService,
                accessManagementTargetValidationSupport,
                userOrgFoundationStateReadService
        );
    }

    private UserOrgFoundationStateReadService.UserAccessPolicyFoundationState activeUserState(Long userId) {
        return new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(
                userId,
                true,
                Set.of("ADMIN")
        );
    }

    private UserOrgFoundationStateReadService.UserAccessPolicyFoundationState inactiveUserState(Long userId) {
        return new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(
                userId,
                false,
                Set.of("ADMIN")
        );
    }

    private TemporaryRoleAssignment temporaryRoleAssignment(Long userId, Long roleId, Instant validFrom) {
        return new TemporaryRoleAssignment(
                null,
                userId,
                roleId,
                validFrom,
                null,
                validFrom,
                validFrom
        );
    }

    private TemporaryAccessArea temporaryAccessArea(
            Long userId,
            Long organizationalUnitId,
            AccessScopeType accessScopeType,
            Instant validFrom
    ) {
        return new TemporaryAccessArea(
                null,
                userId,
                organizationalUnitId,
                accessScopeType,
                validFrom,
                null,
                validFrom,
                validFrom
        );
    }

    private TemporaryManagementDelegation temporaryManagementDelegation(
            Long userId,
            Long organizationalUnitId,
            Long managementRelationTypeId,
            Instant validFrom
    ) {
        return new TemporaryManagementDelegation(
                null,
                userId,
                organizationalUnitId,
                managementRelationTypeId,
                validFrom,
                null,
                validFrom,
                validFrom
        );
    }
}
