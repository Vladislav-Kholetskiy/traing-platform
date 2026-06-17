package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса {@code ImportAdminReadServiceImpl}.
 */

@Service
@Transactional(readOnly = true)
public class ImportAdminReadServiceImpl implements ImportAdminReadService {

    private static final AccessReadArea SUPPORTED_CONTOUR = AccessReadArea.IMPORT_JOB_ADMINISTRATION;
    private static final String DENIAL_MESSAGE = "Import administration read is forbidden by AccessSpecificationPolicy";

    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final ImportQueryService importQueryService;

    public ImportAdminReadServiceImpl(
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver queryContextResolver,
        ImportQueryService importQueryService
    ) {
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.queryContextResolver = Objects.requireNonNull(
            queryContextResolver,
            "queryContextResolver must not be null"
        );
        this.importQueryService = Objects.requireNonNull(importQueryService, "importQueryService must not be null");
    }

    @Override
    public List<ImportJobReadModel> listImportJobs(Long actorUserId, ImportJobReadFilter filter) {
        ImportJobReadFilter effectiveFilter = filter == null ? new ImportJobReadFilter(null, null) : filter;
        AccessPolicyQueryContext context = queryContextResolver.resolveImportJobAdministrationContext(actorUserId);
        ensureReadAllowed(context);

        return materializeJobCandidates(effectiveFilter).stream()
            .filter(matchesJobFilter(effectiveFilter))
            .sorted(Comparator
                .comparing(ImportJob::createdAt, Comparator.reverseOrder())
                .thenComparing(ImportJob::id, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toReadModel)
            .toList();
    }

    @Override
    public ImportJobReadModel findImportJobById(Long actorUserId, Long importJobId) {
        AccessPolicyQueryContext context = queryContextResolver.resolveImportJobAdministrationDetailContext(
            actorUserId,
            importJobId
        );
        ensureReadAllowed(context);
        return toReadModel(importQueryService.findImportJobById(importJobId));
    }

    @Override
    public List<ImportJobItemReadModel> listImportJobItems(
        Long actorUserId,
        Long importJobId,
        ImportJobItemReadFilter filter
    ) {
        ImportJobItemReadFilter effectiveFilter = filter == null ? new ImportJobItemReadFilter(null) : filter;
        AccessPolicyQueryContext context = queryContextResolver.resolveImportJobItemAdministrationContext(actorUserId);
        ensureReadAllowed(context);

        return importQueryService.findImportJobItemsByJobId(importJobId).stream()
            .filter(matchesItemFilter(effectiveFilter))
            .sorted(Comparator.comparingInt(ImportJobItem::itemNo).thenComparing(ImportJobItem::id, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::toReadModel)
            .toList();
    }

    @Override
    public ImportJobItemReadModel findImportJobItemById(Long actorUserId, Long itemId) {
        AccessPolicyQueryContext context = queryContextResolver.resolveImportJobItemAdministrationDetailContext(
            actorUserId,
            itemId
        );
        ensureReadAllowed(context);
        return toReadModel(importQueryService.findImportJobItemById(itemId));
    }

    private void ensureReadAllowed(AccessPolicyQueryContext context) {
        if (context.contour() != SUPPORTED_CONTOUR || !accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException("IMPORT_JOB_ADMINISTRATION_DENIED", DENIAL_MESSAGE);
        }
    }

    private List<ImportJob> materializeJobCandidates(ImportJobReadFilter filter) {
        List<ImportJob> baseSlice;
        if (filter.sourceType() != null) {
            baseSlice = importQueryService.findImportJobsBySourceType(filter.sourceType());
        } else if (filter.status() != null) {
            baseSlice = importQueryService.findImportJobsByStatus(filter.status());
        } else {
            baseSlice = Arrays.stream(ImportJobStatus.values())
                .flatMap(status -> importQueryService.findImportJobsByStatus(status).stream())
                .toList();
        }

        Map<Long, ImportJob> deduplicated = new LinkedHashMap<>();
        for (ImportJob job : baseSlice) {
            deduplicated.putIfAbsent(job.id(), job);
        }
        return List.copyOf(deduplicated.values());
    }

    private Predicate<ImportJob> matchesJobFilter(ImportJobReadFilter filter) {
        return job ->
            (filter.status() == null || job.status() == filter.status())
                && (filter.sourceType() == null || filter.sourceType().equals(job.sourceType()));
    }

    private Predicate<ImportJobItem> matchesItemFilter(ImportJobItemReadFilter filter) {
        return item -> filter.status() == null || item.status() == filter.status();
    }

    private ImportJobReadModel toReadModel(ImportJob job) {
        return new ImportJobReadModel(
            job.id(),
            job.sourceType(),
            job.sourceRef(),
            job.status(),
            job.totalItemCount(),
            job.processedItemCount(),
            job.appliedItemCount(),
            job.failedItemCount(),
            job.requiresReviewItemCount(),
            job.startedAt(),
            job.completedAt(),
            job.createdAt(),
            job.updatedAt()
        );
    }

    private ImportJobItemReadModel toReadModel(ImportJobItem item) {
        return new ImportJobItemReadModel(
            item.id(),
            item.importJobId(),
            item.itemNo(),
            item.targetEntityType(),
            item.externalId(),
            item.employeeNumber(),
            item.status(),
            item.matchedEntityId(),
            item.errorCode(),
            item.errorMessage(),
            item.processedAt(),
            item.createdAt(),
            item.updatedAt()
        );
    }
}
