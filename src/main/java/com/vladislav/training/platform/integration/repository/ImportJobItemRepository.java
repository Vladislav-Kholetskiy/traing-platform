package com.vladislav.training.platform.integration.repository;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import java.util.List;

/**
 * Контракт репозитория {@code ImportJobItemRepository}.
 */
public interface ImportJobItemRepository {

    ImportJobItem findImportJobItemById(Long importJobItemId);

    List<ImportJobItem> findImportJobItemsByJobId(Long importJobId);

    List<ImportJobItem> findImportJobItemsByStatus(ImportItemStatus status);

    ImportJobItem saveImportJobItem(ImportJobItem importJobItem);
}
