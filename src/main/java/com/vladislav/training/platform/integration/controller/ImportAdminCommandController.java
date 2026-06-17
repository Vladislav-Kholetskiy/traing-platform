package com.vladislav.training.platform.integration.controller;

import com.vladislav.training.platform.integration.controller.dto.ImportLaunchRequest;
import com.vladislav.training.platform.integration.controller.dto.ImportLaunchResponse;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.service.ImportCommandService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code ImportAdminCommandController}.
 */

@RestController
@Validated
@RequestMapping("/api/v1/admin/import-jobs")
public class ImportAdminCommandController {

    private static final Instant COMMAND_MAPPING_SENTINEL_TIME = Instant.EPOCH;

    private final ImportCommandService importCommandService;

    public ImportAdminCommandController(ImportCommandService importCommandService) {
        this.importCommandService = Objects.requireNonNull(
            importCommandService,
            "importCommandService must not be null"
        );
    }

    @PostMapping
    public ResponseEntity<ImportLaunchResponse> launchImportJob(@Valid @RequestBody ImportLaunchRequest request) {
        ImportJob launchedJob = importCommandService.launchImportJob(toImportJob(request), toImportJobItems(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(launchedJob));
    }

    private ImportJob toImportJob(ImportLaunchRequest request) {
        return new ImportJob(
            null,
            request.sourceType(),
            request.sourceRef(),
            null,
            ImportJobStatus.PENDING,
            request.payload(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            COMMAND_MAPPING_SENTINEL_TIME,
            COMMAND_MAPPING_SENTINEL_TIME
        );
    }

    private List<ImportJobItem> toImportJobItems(ImportLaunchRequest request) {
        return request.items().stream()
            .map(item -> new ImportJobItem(
                null,
                0L,
                0,
                item.targetEntityType(),
                item.externalId(),
                item.employeeNumber(),
                ImportItemStatus.PENDING,
                null,
                item.payload(),
                null,
                null,
                null,
                COMMAND_MAPPING_SENTINEL_TIME,
                COMMAND_MAPPING_SENTINEL_TIME
            ))
            .toList();
    }

    private ImportLaunchResponse toResponse(ImportJob launchedJob) {
        return new ImportLaunchResponse(
            launchedJob.id(),
            launchedJob.status(),
            launchedJob.totalItemCount(),
            launchedJob.createdAt(),
            launchedJob.updatedAt()
        );
    }
}
