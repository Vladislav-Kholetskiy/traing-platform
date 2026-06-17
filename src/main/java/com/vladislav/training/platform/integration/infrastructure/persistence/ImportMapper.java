package com.vladislav.training.platform.integration.infrastructure.persistence;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.util.List;
import org.springframework.stereotype.Component;
/**
 * Преобразователь {@code ImportMapper}.
 */

@Component
public class ImportMapper {

    public ImportJob toDomain(ImportJobEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ImportJob(
            entity.getId(),
            entity.getSourceType(),
            entity.getSourceRef(),
            entity.getInitiatedByUserId(),
            ImportJobStatus.valueOf(entity.getStatus()),
            entity.getPayload(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getTotalItemCount(),
            entity.getProcessedItemCount(),
            entity.getAppliedItemCount(),
            entity.getFailedItemCount(),
            entity.getRequiresReviewItemCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public ImportJobEntity toEntity(ImportJob domain) {
        if (domain == null) {
            return null;
        }
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(domain.id());
        entity.setSourceType(domain.sourceType());
        entity.setSourceRef(domain.sourceRef());
        entity.setInitiatedByUserId(domain.initiatedByUserId());
        entity.setStatus(domain.status().name());
        entity.setPayload(domain.payload());
        entity.setStartedAt(domain.startedAt());
        entity.setCompletedAt(domain.completedAt());
        entity.setTotalItemCount(domain.totalItemCount());
        entity.setProcessedItemCount(domain.processedItemCount());
        entity.setAppliedItemCount(domain.appliedItemCount());
        entity.setFailedItemCount(domain.failedItemCount());
        entity.setRequiresReviewItemCount(domain.requiresReviewItemCount());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<ImportJob> toImportJobs(List<ImportJobEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public ImportJobItem toDomain(ImportJobItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ImportJobItem(
            entity.getId(),
            entity.getImportJobId(),
            entity.getItemNo(),
            entity.getTargetEntityType(),
            entity.getExternalId(),
            entity.getEmployeeNumber(),
            ImportItemStatus.valueOf(entity.getStatus()),
            entity.getMatchedEntityId(),
            entity.getPayload(),
            entity.getErrorCode(),
            entity.getErrorMessage(),
            entity.getProcessedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public ImportJobItemEntity toEntity(ImportJobItem domain) {
        if (domain == null) {
            return null;
        }
        ImportJobItemEntity entity = new ImportJobItemEntity();
        entity.setId(domain.id());
        entity.setImportJobId(domain.importJobId());
        entity.setItemNo(domain.itemNo());
        entity.setTargetEntityType(domain.targetEntityType());
        entity.setExternalId(domain.externalId());
        entity.setEmployeeNumber(domain.employeeNumber());
        entity.setStatus(domain.status().name());
        entity.setMatchedEntityId(domain.matchedEntityId());
        entity.setPayload(domain.payload());
        entity.setErrorCode(domain.errorCode());
        entity.setErrorMessage(domain.errorMessage());
        entity.setProcessedAt(domain.processedAt());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<ImportJobItem> toImportJobItems(List<ImportJobItemEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }
}
