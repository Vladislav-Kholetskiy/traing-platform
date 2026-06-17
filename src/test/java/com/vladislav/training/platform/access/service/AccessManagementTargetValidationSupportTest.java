package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code AccessManagementTargetValidation}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class AccessManagementTargetValidationSupportTest {

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    @Test
    void ensureAccessAreaTargetAllowedUsesFoundationContractState() {
        AccessManagementTargetValidationSupport support = new AccessManagementTargetValidationSupport(
            userOrgFoundationStateReadService
        );
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(30L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                30L,
                true,
                "/root/branch",
                false,
                true,
                false,
                true,
                true,
                true
            )
        );

        assertThatCode(() -> support.ensureAccessAreaTargetAllowed(30L, AccessScopeType.UNIT_SUBTREE))
            .doesNotThrowAnyException();
    }

    @Test
    void ensureOperatorHomeUnitAllowedRejectsNonLinearTargetFromFoundationContract() {
        AccessManagementTargetValidationSupport support = new AccessManagementTargetValidationSupport(
            userOrgFoundationStateReadService
        );
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(31L)).thenReturn(
            new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
                31L,
                true,
                "/root/functional",
                false,
                true,
                true,
                true,
                true,
                false
            )
        );

        assertThatThrownBy(() -> support.ensureOperatorHomeUnitAllowed(31L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("LINEAR");
    }
}