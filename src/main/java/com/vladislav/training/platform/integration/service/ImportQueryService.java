package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.util.List;
/**
 * Контракт сервиса чтения {@code ImportQueryService}.
 */
public interface ImportQueryService {

    ImportJob findImportJobById(Long importJobId);

    List<ImportJob> findImportJobsByStatus(ImportJobStatus status);

    List<ImportJob> findImportJobsBySourceType(String sourceType);

    ImportJobItem findImportJobItemById(Long importJobItemId);

    List<ImportJobItem> findImportJobItemsByJobId(Long importJobId);

    List<ImportJobItem> findImportJobItemsByStatus(ImportItemStatus status);
}
