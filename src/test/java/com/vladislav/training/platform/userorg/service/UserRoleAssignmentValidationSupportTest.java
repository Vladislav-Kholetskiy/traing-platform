package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code UserRoleAssignmentValidation}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class UserRoleAssignmentValidationSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-01T12:00:00Z");

    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserOperatorStateSupport UserOperatorStateSupport;

    @Test
    void ensureAssignableRejectsInactiveUserBeforeHistoryAndOperatorChecks() {
        UserRoleAssignmentValidationSupport support = new UserRoleAssignmentValidationSupport(
                userRoleAssignmentRepository,
                appUserRepository,
                UserOperatorStateSupport
        );
        when(appUserRepository.findUserById(7L)).thenReturn(user(7L, UserStatus.INACTIVE));

        assertThatThrownBy(() -> support.ensureAssignable(roleAssignment(7L, 100L, FIXED_INSTANT)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Inactive user cannot receive new active role assignment");

        verify(appUserRepository).findUserById(7L);
        verify(userRoleAssignmentRepository, never()).findRoleAssignmentsByUserId(7L);
        verifyNoInteractions(UserOperatorStateSupport);
    }

    @Test
    void ensureAssignableContinuesWithOwnerHistoryChecksForActiveUser() {
        UserRoleAssignmentValidationSupport support = new UserRoleAssignmentValidationSupport(
                userRoleAssignmentRepository,
                appUserRepository,
                UserOperatorStateSupport
        );
        when(appUserRepository.findUserById(7L)).thenReturn(user(7L, UserStatus.ACTIVE));
        when(userRoleAssignmentRepository.findRoleAssignmentsByUserId(7L)).thenReturn(List.of());
        when(UserOperatorStateSupport.loadActivePermanentRoleAssignments(7L, FIXED_INSTANT)).thenReturn(List.of());
        when(UserOperatorStateSupport.loadActiveOrganizationAssignments(7L, FIXED_INSTANT)).thenReturn(List.of());

        support.ensureAssignable(roleAssignment(7L, 100L, FIXED_INSTANT));

        verify(appUserRepository).findUserById(7L);
        verify(userRoleAssignmentRepository).findRoleAssignmentsByUserId(7L);
        verify(UserOperatorStateSupport).ensureResultingStateConsistent(7L, FIXED_INSTANT, List.of(
                new UserRoleAssignment(
                        null,
                        7L,
                        100L,
                        FIXED_INSTANT,
                        null,
                        FIXED_INSTANT,
                        FIXED_INSTANT
                )
        ), List.of());
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

    private UserRoleAssignment roleAssignment(Long userId, Long roleId, Instant validFrom) {
        return new UserRoleAssignment(
                null,
                userId,
                roleId,
                validFrom,
                null,
                validFrom,
                validFrom
        );
    }
}
