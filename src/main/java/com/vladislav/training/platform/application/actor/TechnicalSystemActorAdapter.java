package com.vladislav.training.platform.application.actor;

import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import org.springframework.stereotype.Component;

/**
 * Класс {@code TechnicalSystemActorAdapter}.
 */
@Component
public class TechnicalSystemActorAdapter {

    static final String SYSTEM_EMPLOYEE_NUMBER = "SYSTEM";

    private final UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    public TechnicalSystemActorAdapter(UserOrgFoundationStateReadService userOrgFoundationStateReadService) {
        this.userOrgFoundationStateReadService = userOrgFoundationStateReadService;
    }

    public Long resolveSystemActorUserId() {
        try {
            UserOrgFoundationStateReadService.UserIdentityFoundationState systemActor =
                userOrgFoundationStateReadService.findUserIdentityFoundationStateByEmployeeNumber(SYSTEM_EMPLOYEE_NUMBER);
            if (!systemActor.active()) {
                throw unresolvedSystemActor();
            }
            return systemActor.userId();
        } catch (RuntimeException exception) {
            if (exception instanceof PolicyViolationException policyViolationException) {
                throw policyViolationException;
            }
            throw unresolvedSystemActor();
        }
    }

    private PolicyViolationException unresolvedSystemActor() {
        return new PolicyViolationException(
            "Системный пользователь не найден или неактивен. "
                + "Technical SYSTEM actor must resolve to ACTIVE app_user"
        );
    }
}
