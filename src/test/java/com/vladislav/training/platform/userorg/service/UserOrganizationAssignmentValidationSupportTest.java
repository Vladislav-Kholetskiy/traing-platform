package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code UserOrganizationAssignmentValidation}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class UserOrganizationAssignmentValidationSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-01T12:00:00Z");

    @Mock
    private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private OrganizationalUnitAssignmentValidationSupport organizationalUnitAssignmentValidationSupport;
    @Mock
    private UserOperatorStateSupport UserOperatorStateSupport;

    @Test
    void ensureAssignableRejectsInactiveUserBeforeTargetAndHistoryChecks() {
        UserOrganizationAssignmentValidationSupport support = new UserOrganizationAssignmentValidationSupport(
                userOrganizationAssignmentRepository,
                appUserRepository,
                organizationalUnitAssignmentValidationSupport,
                UserOperatorStateSupport
        );
        when(appUserRepository.findUserById(7L)).thenReturn(user(7L, UserStatus.INACTIVE));

        assertThatThrownBy(() -> support.ensureAssignable(
                organizationAssignment(7L, 30L, OrganizationAssignmentType.SECONDARY, FIXED_INSTANT)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Inactive user cannot receive new active organization assignment");

        verify(appUserRepository).findUserById(7L);
        verifyNoInteractions(organizationalUnitAssignmentValidationSupport);
        verify(userOrganizationAssignmentRepository, never()).findOrganizationAssignmentsByUserId(7L);
        verifyNoInteractions(UserOperatorStateSupport);
    }

    @Test
    void ensurePrimaryHomeReplacementAllowedRejectsInactiveUser() {
        UserOrganizationAssignmentValidationSupport support = new UserOrganizationAssignmentValidationSupport(
                userOrganizationAssignmentRepository,
                appUserRepository,
                organizationalUnitAssignmentValidationSupport,
                UserOperatorStateSupport
        );
        when(appUserRepository.findUserById(7L)).thenReturn(user(7L, UserStatus.INACTIVE));

        assertThatThrownBy(() -> support.ensurePrimaryHomeReplacementAllowed(7L, 31L, FIXED_INSTANT))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Inactive user cannot replace PRIMARY home unit");

        verify(appUserRepository).findUserById(7L);
        verifyNoInteractions(organizationalUnitAssignmentValidationSupport);
        verify(userOrganizationAssignmentRepository, never()).findOrganizationAssignmentsByUserId(7L);
        verifyNoInteractions(UserOperatorStateSupport);
    }

    @Test
    void ensureAssignableContinuesWithOwnerChecksForActiveUser() {
        UserOrganizationAssignmentValidationSupport support = new UserOrganizationAssignmentValidationSupport(
                userOrganizationAssignmentRepository,
                appUserRepository,
                organizationalUnitAssignmentValidationSupport,
                UserOperatorStateSupport
        );
        when(appUserRepository.findUserById(7L)).thenReturn(user(7L, UserStatus.ACTIVE));
        when(userOrganizationAssignmentRepository.findOrganizationAssignmentsByUserId(7L)).thenReturn(List.of());
        when(UserOperatorStateSupport.loadActiveOrganizationAssignments(7L, FIXED_INSTANT)).thenReturn(List.of());
        when(UserOperatorStateSupport.loadActivePermanentRoleAssignments(7L, FIXED_INSTANT)).thenReturn(List.of());

        support.ensureAssignable(organizationAssignment(7L, 30L, OrganizationAssignmentType.SECONDARY, FIXED_INSTANT));

        verify(appUserRepository).findUserById(7L);
        verify(organizationalUnitAssignmentValidationSupport).requireAssignableTarget(30L);
        verify(userOrganizationAssignmentRepository).findOrganizationAssignmentsByUserId(7L);
        verify(UserOperatorStateSupport).ensureResultingStateConsistent(7L, FIXED_INSTANT, List.of(), List.of(
                new UserOrganizationAssignment(
                        null,
                        7L,
                        30L,
                        OrganizationAssignmentType.SECONDARY,
                        FIXED_INSTANT,
                        null,
                        FIXED_INSTANT,
                        FIXED_INSTANT
                )
        ));
    }

    private AppUser user(Long userId, UserStatus status) {
        return new AppUser(
                userId,
                "EMP-" + userId,
                "EXT-" + userId,
                "Last",
                "First",
                null,
                status,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.minusSeconds(3600)
        );
    }

    private UserOrganizationAssignment organizationAssignment(
            Long userId,
            Long organizationalUnitId,
            OrganizationAssignmentType assignmentType,
            Instant validFrom
    ) {
        return new UserOrganizationAssignment(
                null,
                userId,
                organizationalUnitId,
                assignmentType,
                validFrom,
                null,
                validFrom,
                validFrom
        );
    }
}
