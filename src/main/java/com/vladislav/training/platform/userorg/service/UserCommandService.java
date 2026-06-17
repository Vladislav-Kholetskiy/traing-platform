package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.AppUser;
import java.time.Instant;

/**
 * Контракт командного сервиса {@code UserCommandService}.
 */
public interface UserCommandService {

    AppUser createUser(AppUser user);

    AppUser updateUser(AppUser user);

        AppUser deactivateUserAfterAdmission(Long userId, Instant effectiveAt);
}
