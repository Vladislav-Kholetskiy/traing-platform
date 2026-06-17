package com.vladislav.training.platform.integration.repository;

import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.util.List;

/**
 * Контракт репозитория {@code ImportJobRepository}.
 */
public interface ImportJobRepository {

    ImportJob findImportJobById(Long importJobId);

    List<ImportJob> findImportJobsByStatus(ImportJobStatus status);

    List<ImportJob> findImportJobsBySourceType(String sourceType);

    ImportJob saveImportJob(ImportJob importJob);
}
