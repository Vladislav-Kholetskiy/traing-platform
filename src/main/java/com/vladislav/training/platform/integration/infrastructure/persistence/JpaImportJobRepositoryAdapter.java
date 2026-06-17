package com.vladislav.training.platform.integration.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Адаптер репозитория {@code JpaImportJobRepositoryAdapter}.
 */

@Repository
@Transactional(readOnly = true)
public class JpaImportJobRepositoryAdapter implements ImportJobRepository {

    private final SpringDataImportJobJpaRepository repository;
    private final ImportMapper mapper;

    public JpaImportJobRepositoryAdapter(
        SpringDataImportJobJpaRepository repository,
        ImportMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ImportJob findImportJobById(Long importJobId) {
        return repository.findById(importJobId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Import job not found: " + importJobId));
    }

    @Override
    public List<ImportJob> findImportJobsByStatus(ImportJobStatus status) {
        return mapper.toImportJobs(repository.findAllByStatusOrderByIdAsc(status.name()));
    }

    @Override
    public List<ImportJob> findImportJobsBySourceType(String sourceType) {
        return mapper.toImportJobs(repository.findAllBySourceTypeOrderByIdAsc(sourceType));
    }

    @Override
    @Transactional
    public ImportJob saveImportJob(ImportJob importJob) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(importJob)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist import job", exception);
        }
    }
}
