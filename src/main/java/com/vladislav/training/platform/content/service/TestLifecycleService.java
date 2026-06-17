package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Test;

/**
 * Контракт сервиса {@code TestLifecycleService}.
 */
public interface TestLifecycleService {

    Test publish(Long testId);

    Test archive(Long testId);
}
