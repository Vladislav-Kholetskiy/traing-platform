package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaMaterialRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaMaterialRepositoryAdapter implements MaterialRepository {
    private final SpringDataMaterialJpaRepository repository; private final ContentMapper mapper;
    public JpaMaterialRepositoryAdapter(SpringDataMaterialJpaRepository repository, ContentMapper mapper){this.repository=repository;this.mapper=mapper;}
    @Override public Material findMaterialById(Long materialId){ return repository.findById(materialId).map(mapper::toDomain).orElseThrow(() -> new NotFoundException("Material not found: "+materialId)); }
    @Override public List<Material> findMaterialsByTopicId(Long topicId){ return mapper.toMaterials(repository.findAllByTopicIdOrderBySortOrderAscIdAsc(topicId)); }
    @Override public List<Material> findMaterialsByTopicIdAndStatus(Long topicId, ContentStatus status){ return mapper.toMaterials(repository.findAllByTopicIdAndStatusOrderBySortOrderAscIdAsc(topicId, status)); }
    @Override public boolean existsNonArchivedByTopicId(Long topicId){ return repository.existsByTopicIdAndStatusNot(topicId, ContentStatus.ARCHIVED); }
    @Override @Transactional public Material saveMaterial(Material material){ try { return mapper.toDomain(repository.save(mapper.toEntity(material))); } catch (DataIntegrityViolationException ex){ throw new PersistenceConstraintViolationException("Failed to persist material", ex); } }
}
