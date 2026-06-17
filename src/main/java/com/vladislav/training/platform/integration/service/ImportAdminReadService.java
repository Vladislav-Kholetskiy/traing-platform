package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code ImportAdminReadService}.
 */
public interface ImportAdminReadService {

    List<ImportJobReadModel> listImportJobs(Long actorUserId, ImportJobReadFilter filter);

    ImportJobReadModel findImportJobById(Long actorUserId, Long importJobId);

    List<ImportJobItemReadModel> listImportJobItems(Long actorUserId, Long importJobId, ImportJobItemReadFilter filter);

    ImportJobItemReadModel findImportJobItemById(Long actorUserId, Long itemId);

    record ImportJobReadFilter(
        ImportJobStatus status,
        String sourceType
    ) {

        public ImportJobReadFilter {
            if (sourceType != null && sourceType.isBlank()) {
                throw new IllegalArgumentException("sourceType must not be blank");
            }
        }
    }

    record ImportJobItemReadFilter(
        ImportItemStatus status
    ) {
    }

    record ImportJobReadModel(
        Long id,
        String sourceType,
        String sourceRef,
        ImportJobStatus status,
        Integer totalItemCount,
        Integer processedItemCount,
        Integer appliedItemCount,
        Integer failedItemCount,
        Integer requiresReviewItemCount,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    record ImportJobItemReadModel(
        Long id,
        Long importJobId,
        int itemNo,
        String targetEntityType,
        String externalId,
        String employeeNumber,
        ImportItemStatus status,
        String matchedEntityId,
        String errorCode,
        String errorMessage,
        Instant processedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
