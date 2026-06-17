package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code MaterialQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class MaterialQueryServiceImpl implements MaterialQueryService {

    private final MaterialRepository repository;
    private final AccessSpecificationPolicy policy;
    private final AccessPolicyQueryContextResolver resolver;

    public MaterialQueryServiceImpl(
        MaterialRepository repository,
        AccessSpecificationPolicy policy,
        AccessPolicyQueryContextResolver resolver
    ) {
        this.repository = repository;
        this.policy = policy;
        this.resolver = resolver;
    }

    @Override
    public Material findMaterialById(Long materialId) {
        ensure(AccessReadType.DETAIL);
        return repository.findMaterialById(materialId);
    }

    @Override
    public List<Material> findMaterialsByTopicId(Long topicId) {
        ensure(AccessReadType.LIST);
        return repository.findMaterialsByTopicId(topicId);
    }

    @Override
    public List<Material> findMaterialsByTopicIdAndStatus(Long topicId, ContentStatus status) {
        ensure(AccessReadType.LIST);
        return repository.findMaterialsByTopicIdAndStatus(topicId, status);
    }

    private void ensure(AccessReadType readType) {
        if (!policy.canRead(resolver.resolve(AccessReadArea.CONTENT_AUTHORING, readType, "material"))) {
            throw new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Actor is not authorized to read material data");
        }
    }
}
