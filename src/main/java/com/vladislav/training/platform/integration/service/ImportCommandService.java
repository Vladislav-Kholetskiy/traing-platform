package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import java.util.List;
/**
 * Контракт командного сервиса {@code ImportCommandService}.
 */
public interface ImportCommandService {

    ImportJob launchImportJob(ImportJob importJob, List<ImportJobItem> importJobItems);

    ImportJob launchSystemImportJob(ImportJob importJob, List<ImportJobItem> importJobItems);

}
