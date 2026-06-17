package com.vladislav.training.platform.application.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code TechnicalSystemActorAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TechnicalSystemActorAdapterTest {

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    @InjectMocks
    private TechnicalSystemActorAdapter technicalSystemActorAdapter;

    @Test
    void resolveSystemActorUserIdReturnsActiveSystemUserFromOwnerOwnedContract() {
        when(userOrgFoundationStateReadService.findUserIdentityFoundationStateByEmployeeNumber("SYSTEM"))
            .thenReturn(new UserOrgFoundationStateReadService.UserIdentityFoundationState(900L, "SYSTEM", true));

        assertThat(technicalSystemActorAdapter.resolveSystemActorUserId()).isEqualTo(900L);
    }

    @Test
    void resolveSystemActorUserIdRejectsInactiveSystemUser() {
        when(userOrgFoundationStateReadService.findUserIdentityFoundationStateByEmployeeNumber("SYSTEM"))
            .thenReturn(new UserOrgFoundationStateReadService.UserIdentityFoundationState(900L, "SYSTEM", false));

        assertThatThrownBy(() -> technicalSystemActorAdapter.resolveSystemActorUserId())
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Technical SYSTEM actor must resolve to ACTIVE app_user");
    }

    @Test
    void resolveSystemActorUserIdFailsClosedWhenSystemUserIsMissing() {
        when(userOrgFoundationStateReadService.findUserIdentityFoundationStateByEmployeeNumber("SYSTEM"))
            .thenThrow(new NotFoundException("SYSTEM user not found"));

        assertThatThrownBy(() -> technicalSystemActorAdapter.resolveSystemActorUserId())
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Technical SYSTEM actor must resolve to ACTIVE app_user");
    }
}
