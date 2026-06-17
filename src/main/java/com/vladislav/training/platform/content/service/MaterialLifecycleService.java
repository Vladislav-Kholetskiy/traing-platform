package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Material;

/**
 * Контракт сервиса {@code MaterialLifecycleService}.
 */
public interface MaterialLifecycleService {

    Material publish(Long materialId);

    Material archive(Long materialId);
}
