package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
/**
 * Класс {@code TransactionalImportTypedOwnerCommandExecutor}.
 */

@Service
class TransactionalImportTypedOwnerCommandExecutor implements ImportTypedOwnerCommandExecutor {

    private final UserCommandService userCommandService;

    TransactionalImportTypedOwnerCommandExecutor(UserCommandService userCommandService) {
        this.userCommandService = Objects.requireNonNull(userCommandService, "userCommandService must not be null");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUser updateAppUser(AppUser user) {
        return userCommandService.updateUser(user);
    }
}
