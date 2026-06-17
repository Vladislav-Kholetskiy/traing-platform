package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса чтения {@code ImportQueryServiceImpl}.
 */

@Service
@Transactional(readOnly = true)
public class ImportQueryServiceImpl implements ImportQueryService {

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemRepository importJobItemRepository;

    public ImportQueryServiceImpl(
        ImportJobRepository importJobRepository,
        ImportJobItemRepository importJobItemRepository
    ) {
        this.importJobRepository = Objects.requireNonNull(importJobRepository, "importJobRepository must not be null");
        this.importJobItemRepository = Objects.requireNonNull(
            importJobItemRepository,
            "importJobItemRepository must not be null"
        );
    }

    @Override
    public ImportJob findImportJobById(Long importJobId) {
        return importJobRepository.findImportJobById(importJobId);
    }

    @Override
    public List<ImportJob> findImportJobsByStatus(ImportJobStatus status) {
        return importJobRepository.findImportJobsByStatus(status);
    }

    @Override
    public List<ImportJob> findImportJobsBySourceType(String sourceType) {
        return importJobRepository.findImportJobsBySourceType(sourceType);
    }

    @Override
    public ImportJobItem findImportJobItemById(Long importJobItemId) {
        return importJobItemRepository.findImportJobItemById(importJobItemId);
    }

    @Override
    public List<ImportJobItem> findImportJobItemsByJobId(Long importJobId) {
        return importJobItemRepository.findImportJobItemsByJobId(importJobId);
    }

    @Override
    public List<ImportJobItem> findImportJobItemsByStatus(ImportItemStatus status) {
        return importJobItemRepository.findImportJobItemsByStatus(status);
    }
}
