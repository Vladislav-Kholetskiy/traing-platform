package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import java.util.List;

/**
 * Контракт сервиса чтения {@code MaterialQueryService}.
 */
public interface MaterialQueryService {

    Material findMaterialById(Long materialId);

    List<Material> findMaterialsByTopicId(Long topicId);

    List<Material> findMaterialsByTopicIdAndStatus(Long topicId, ContentStatus status);
}
