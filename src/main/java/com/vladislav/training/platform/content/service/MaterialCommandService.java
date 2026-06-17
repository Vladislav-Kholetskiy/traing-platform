package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Material;

/**
 * Контракт командного сервиса {@code MaterialCommandService}.
 */
public interface MaterialCommandService {

    Material createMaterial(CreateMaterialCommand command);

    Material updateMaterial(Long materialId, UpdateMaterialCommand command);
}
