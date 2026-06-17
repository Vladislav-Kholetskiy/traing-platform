package com.vladislav.training.platform.content.repository;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import java.util.List;

/**
 * Контракт репозитория {@code MaterialRepository}.
 */
public interface MaterialRepository {

    Material findMaterialById(Long materialId);

    List<Material> findMaterialsByTopicId(Long topicId);

    List<Material> findMaterialsByTopicIdAndStatus(Long topicId, ContentStatus status);

    boolean existsNonArchivedByTopicId(Long topicId);

    Material saveMaterial(Material material);
}
