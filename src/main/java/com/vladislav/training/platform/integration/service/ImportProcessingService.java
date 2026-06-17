package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
/**
 * Контракт сервиса {@code ImportProcessingService}.
 */
public interface ImportProcessingService {

    ImportJob processImportJob(Long importJobId);

    ImportJobItem processImportJobItem(Long importJobItemId);
}
