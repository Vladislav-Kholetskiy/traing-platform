package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация командного сервиса {@code ImportCommandServiceImpl}.
 */

@Service
@Transactional
public class ImportCommandServiceImpl implements ImportCommandService {

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemRepository importJobItemRepository;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final SystemActorResolver systemActorResolver;
    private final UtcClock utcClock;

    public ImportCommandServiceImpl(
        ImportJobRepository importJobRepository,
        ImportJobItemRepository importJobItemRepository,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        SystemActorResolver systemActorResolver,
        UtcClock utcClock
    ) {
        this.importJobRepository = Objects.requireNonNull(importJobRepository, "importJobRepository must not be null");
        this.importJobItemRepository = Objects.requireNonNull(
            importJobItemRepository,
            "importJobItemRepository must not be null"
        );
        this.capabilityAdmissionPolicy = Objects.requireNonNull(
            capabilityAdmissionPolicy,
            "capabilityAdmissionPolicy must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
        this.systemActorResolver = Objects.requireNonNull(systemActorResolver, "systemActorResolver must not be null");
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @Override
    public ImportJob launchImportJob(ImportJob importJob, List<ImportJobItem> importJobItems) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createImportJobLaunch();
        capabilityAdmissionPolicy.check(request);
        return materializeRawImportJob(importJob, importJobItems, request.actorUserId(), true);
    }

    @Override
    public ImportJob launchSystemImportJob(ImportJob importJob, List<ImportJobItem> importJobItems) {
        Long systemActorUserId = systemActorResolver.resolveSystemActorUserId();
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createImportJobLaunch(systemActorUserId);
        capabilityAdmissionPolicy.check(request);
        return materializeRawImportJob(importJob, importJobItems, systemActorUserId, false);
    }

    private ImportJob materializeRawImportJob(
        ImportJob importJob,
        List<ImportJobItem> importJobItems,
        Long actorUserId,
        boolean interactiveLaunch
    ) {
        Objects.requireNonNull(importJob, "importJob must not be null");
        List<ImportJobItem> rawItems = importJobItems == null ? List.of() : List.copyOf(importJobItems);
        Instant now = utcClock.now();

        ImportJob normalizedJob = new ImportJob(
            null,
            importJob.sourceType(),
            importJob.sourceRef(),
            interactiveLaunch ? actorUserId : importJob.initiatedByUserId(),
            ImportJobStatus.PENDING,
            importJob.payload(),
            null,
            null,
            rawItems.size(),
            0,
            0,
            0,
            0,
            now,
            now
        );
        ImportJob persistedJob = importJobRepository.saveImportJob(normalizedJob);

        for (int itemNo = 0; itemNo < rawItems.size(); itemNo++) {
            ImportJobItem rawItem = rawItems.get(itemNo);
            importJobItemRepository.saveImportJobItem(new ImportJobItem(
                null,
                persistedJob.id(),
                itemNo,
                rawItem.targetEntityType(),
                rawItem.externalId(),
                rawItem.employeeNumber(),
                ImportItemStatus.PENDING,
                null,
                rawItem.payload(),
                null,
                null,
                null,
                now,
                now
            ));
        }

        return persistedJob;
    }
}
