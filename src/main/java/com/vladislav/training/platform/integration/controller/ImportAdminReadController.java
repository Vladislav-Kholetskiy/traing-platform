package com.vladislav.training.platform.integration.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.integration.controller.dto.ImportJobItemReadResponse;
import com.vladislav.training.platform.integration.controller.dto.ImportJobReadResponse;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.service.ImportAdminReadService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code ImportAdminReadController}.
 */

@RestController
@Validated
@RequestMapping("/api/v1/admin")
class ImportAdminReadController {

    private final ImportAdminReadService importAdminReadService;
    private final InteractiveActorResolver interactiveActorResolver;

    ImportAdminReadController(
        ImportAdminReadService importAdminReadService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.importAdminReadService = Objects.requireNonNull(
            importAdminReadService,
            "importAdminReadService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @GetMapping("/import-jobs")
    List<ImportJobReadResponse> listImportJobs(
        @RequestParam(required = false) ImportJobStatus status,
        @RequestParam(required = false) String sourceType
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return importAdminReadService.listImportJobs(
            actorUserId,
            new ImportAdminReadService.ImportJobReadFilter(status, sourceType)
        ).stream().map(this::toResponse).toList();
    }

    @GetMapping("/import-jobs/{importJobId}")
    ImportJobReadResponse findImportJobById(@PathVariable @Positive Long importJobId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(importAdminReadService.findImportJobById(actorUserId, importJobId));
    }

    @GetMapping("/import-jobs/{importJobId}/items")
    List<ImportJobItemReadResponse> listImportJobItems(
        @PathVariable @Positive Long importJobId,
        @RequestParam(required = false) ImportItemStatus status
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return importAdminReadService.listImportJobItems(
            actorUserId,
            importJobId,
            new ImportAdminReadService.ImportJobItemReadFilter(status)
        ).stream().map(this::toResponse).toList();
    }

    @GetMapping("/import-job-items/{itemId}")
    ImportJobItemReadResponse findImportJobItemById(@PathVariable @Positive Long itemId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(importAdminReadService.findImportJobItemById(actorUserId, itemId));
    }

    private ImportJobReadResponse toResponse(ImportAdminReadService.ImportJobReadModel readModel) {
        return new ImportJobReadResponse(
            readModel.id(),
            readModel.sourceType(),
            readModel.sourceRef(),
            readModel.status(),
            readModel.totalItemCount(),
            readModel.processedItemCount(),
            readModel.appliedItemCount(),
            readModel.failedItemCount(),
            readModel.requiresReviewItemCount(),
            readModel.startedAt(),
            readModel.completedAt(),
            readModel.createdAt(),
            readModel.updatedAt()
        );
    }

    private ImportJobItemReadResponse toResponse(ImportAdminReadService.ImportJobItemReadModel readModel) {
        return new ImportJobItemReadResponse(
            readModel.id(),
            readModel.importJobId(),
            readModel.itemNo(),
            readModel.targetEntityType(),
            readModel.externalId(),
            readModel.employeeNumber(),
            readModel.status(),
            readModel.matchedEntityId(),
            readModel.errorCode(),
            readModel.errorMessage(),
            readModel.processedAt(),
            readModel.createdAt(),
            readModel.updatedAt()
        );
    }
}
