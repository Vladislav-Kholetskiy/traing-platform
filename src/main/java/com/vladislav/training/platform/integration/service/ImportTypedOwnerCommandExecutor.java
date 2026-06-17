package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.userorg.domain.AppUser;
/**
 * Интерфейс {@code ImportTypedOwnerCommandExecutor}.
 */
public interface ImportTypedOwnerCommandExecutor {

    AppUser updateAppUser(AppUser user);
}
