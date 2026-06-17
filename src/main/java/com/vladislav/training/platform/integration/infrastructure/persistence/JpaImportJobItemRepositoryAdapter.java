package com.vladislav.training.platform.integration.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Адаптер репозитория {@code JpaImportJobItemRepositoryAdapter}.
 */

@Repository
@Transactional(readOnly = true)
public class JpaImportJobItemRepositoryAdapter implements ImportJobItemRepository {

    private final SpringDataImportJobItemJpaRepository repository;
    private final ImportMapper mapper;

    public JpaImportJobItemRepositoryAdapter(
        SpringDataImportJobItemJpaRepository repository,
        ImportMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ImportJobItem findImportJobItemById(Long importJobItemId) {
        return repository.findById(importJobItemId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Import job item not found: " + importJobItemId));
    }

    @Override
    public List<ImportJobItem> findImportJobItemsByJobId(Long importJobId) {
        return mapper.toImportJobItems(repository.findAllByImportJobIdOrderByItemNoAsc(importJobId));
    }

    @Override
    public List<ImportJobItem> findImportJobItemsByStatus(ImportItemStatus status) {
        return mapper.toImportJobItems(repository.findAllByStatusOrderByIdAsc(status.name()));
    }

    @Override
    @Transactional
    public ImportJobItem saveImportJobItem(ImportJobItem importJobItem) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(importJobItem)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist import job item", exception);
        }
    }
}
