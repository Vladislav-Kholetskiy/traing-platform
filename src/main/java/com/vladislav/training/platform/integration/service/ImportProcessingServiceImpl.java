package com.vladislav.training.platform.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса {@code ImportProcessingServiceImpl}.
 */

@Service
@Transactional
public class ImportProcessingServiceImpl implements ImportProcessingService {

    private static final String TARGET_APP_USER = "APP_USER";
    private static final String OWNER_COMMAND_FAILED = "OWNER_COMMAND_FAILED";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemRepository importJobItemRepository;
    private final AppUserRepository appUserRepository;
    private final ImportTypedOwnerCommandExecutor importTypedOwnerCommandExecutor;
    private final UtcClock utcClock;
    private final ObjectMapper objectMapper;

    @Autowired
    public ImportProcessingServiceImpl(
        ImportJobRepository importJobRepository,
        ImportJobItemRepository importJobItemRepository,
        AppUserRepository appUserRepository,
        ImportTypedOwnerCommandExecutor importTypedOwnerCommandExecutor,
        UtcClock utcClock
    ) {
        this.importJobRepository = Objects.requireNonNull(importJobRepository, "importJobRepository must not be null");
        this.importJobItemRepository = Objects.requireNonNull(
            importJobItemRepository,
            "importJobItemRepository must not be null"
        );
        this.appUserRepository = Objects.requireNonNull(appUserRepository, "appUserRepository must not be null");
        this.importTypedOwnerCommandExecutor = Objects.requireNonNull(
            importTypedOwnerCommandExecutor,
            "importTypedOwnerCommandExecutor must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
        this.objectMapper = new ObjectMapper();
    }

    public ImportProcessingServiceImpl(
        ImportJobRepository importJobRepository,
        ImportJobItemRepository importJobItemRepository,
        AppUserRepository appUserRepository,
        UserCommandService userCommandService,
        UtcClock utcClock
    ) {
        this(
            importJobRepository,
            importJobItemRepository,
            appUserRepository,
            new DirectImportTypedOwnerCommandExecutor(userCommandService),
            utcClock
        );
    }

    @Override
    public ImportJob processImportJob(Long importJobId) {
        Objects.requireNonNull(importJobId, "importJobId must not be null");
        ImportJob currentJob = importJobRepository.findImportJobById(importJobId);
        if (isTerminal(currentJob.status())) {
            return currentJob;
        }

        Instant now = utcClock.now();
        ImportJob activeJob = currentJob.status() == ImportJobStatus.PENDING ? saveInProgressJob(currentJob, now) : currentJob;
        List<ImportJobItem> items = importJobItemRepository.findImportJobItemsByJobId(importJobId);
        List<ImportJobItem> finalItems = new java.util.ArrayList<>(items.size());

        boolean fatalFailure = false;
        for (ImportJobItem item : items) {
            if (isTerminal(item.status())) {
                finalItems.add(item);
                if (isFatalOwnerCommandFailure(item)) {
                    fatalFailure = true;
                    break;
                }
                continue;
            }
            ImportJobItem processedItem = processPendingItem(item, now);
            finalItems.add(processedItem);
            if (isFatalOwnerCommandFailure(processedItem)) {
                fatalFailure = true;
                break;
            }
        }

        if (finalItems.size() < items.size()) {
            finalItems.addAll(items.subList(finalItems.size(), items.size()));
        }

        return importJobRepository.saveImportJob(finishJob(activeJob, finalItems, fatalFailure, now));
    }

    @Override
    public ImportJobItem processImportJobItem(Long importJobItemId) {
        Objects.requireNonNull(importJobItemId, "importJobItemId must not be null");
        ImportJobItem currentItem = importJobItemRepository.findImportJobItemById(importJobItemId);
        if (isTerminal(currentItem.status())) {
            return currentItem;
        }
        return processPendingItem(currentItem, utcClock.now());
    }

    private ImportJobItem processPendingItem(ImportJobItem item, Instant now) {
        ImportJobItem processingItem = item.status() == ImportItemStatus.PROCESSING ? item : saveProcessingItem(item, now);
        try {
            return finalizeKnownOutcome(processingItem, now);
        } catch (RuntimeException exception) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                processingItem,
                ImportItemStatus.FAILED,
                null,
                "OWNER_COMMAND_FAILED",
                exception.getMessage(),
                now
            ));
        }
    }

    private ImportJobItem finalizeKnownOutcome(ImportJobItem item, Instant now) {
        if (!TARGET_APP_USER.equals(item.targetEntityType())) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                item,
                ImportItemStatus.REQUIRES_REVIEW,
                null,
                "UNSUPPORTED_TARGET_TYPE",
                "Для этого типа сущности обработка не поддерживается: targetEntityType=" + item.targetEntityType(),
                now
            ));
        }

        AnchorResolution anchorResolution = resolveAppUserAnchor(item);
        if (anchorResolution.requiresReview()) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                item,
                ImportItemStatus.REQUIRES_REVIEW,
                null,
                anchorResolution.errorCode(),
                anchorResolution.errorMessage(),
                now
            ));
        }
        if (anchorResolution.failure()) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                null,
                anchorResolution.errorCode(),
                anchorResolution.errorMessage(),
                now
            ));
        }

        ImportAppUserPayload payload = parseAppUserPayload(item.payload());
        if (!payload.valid()) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                anchorResolution.matchedUserId(),
                payload.errorCode(),
                payload.errorMessage(),
                now
            ));
        }

        AppUser matchedUser = anchorResolution.matchedUser();
        if (payload.employeeNumber() != null && !payload.employeeNumber().equals(matchedUser.employeeNumber())) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                anchorResolution.matchedUserId(),
                "IDENTITY_MISMATCH",
                "Payload employeeNumber does not match anchored app_user",
                now
            ));
        }
        if (payload.externalId() != null && !Objects.equals(payload.externalId(), matchedUser.externalId())) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                anchorResolution.matchedUserId(),
                "IDENTITY_MISMATCH",
                "Payload externalId does not match anchored app_user",
                now
            ));
        }

        if (isNoChange(matchedUser, payload)) {
            return importJobItemRepository.saveImportJobItem(toTerminalItem(
                item,
                ImportItemStatus.NO_CHANGE,
                anchorResolution.matchedUserId(),
                null,
                null,
                now
            ));
        }

        // Изменение пользователя выполняется через основной сервис пользователей,
        // но в рамках отдельной границы импортного сценария.
        importTypedOwnerCommandExecutor.updateAppUser(new AppUser(
            matchedUser.id(),
            matchedUser.employeeNumber(),
            matchedUser.externalId(),
            payload.lastName(),
            payload.firstName(),
            payload.middleName(),
            payload.status(),
            matchedUser.createdAt(),
            now
        ));

        return importJobItemRepository.saveImportJobItem(toTerminalItem(
            item,
            ImportItemStatus.APPLIED,
            anchorResolution.matchedUserId(),
            null,
            null,
            now
        ));
    }

    private AnchorResolution resolveAppUserAnchor(ImportJobItem item) {
        List<AppUser> users = appUserRepository.findAllUsers();
        String normalizedExternalId = normalizeOptional(item.externalId());
        if (normalizedExternalId != null) {
            List<AppUser> matches = users.stream()
                .filter(user -> normalizedExternalId.equals(normalizeOptional(user.externalId())))
                .toList();
            return resolveMatches(matches, "externalId");
        }

        String normalizedEmployeeNumber = normalizeOptional(item.employeeNumber());
        if (normalizedEmployeeNumber == null) {
            return AnchorResolution.failure("ANCHOR_NOT_FOUND", "Import item does not provide any anchor fields");
        }

        List<AppUser> matches = users.stream()
            .filter(user -> normalizedEmployeeNumber.equals(normalizeOptional(user.employeeNumber())))
            .toList();
        return resolveMatches(matches, "employeeNumber");
    }

    private AnchorResolution resolveMatches(List<AppUser> matches, String anchorType) {
        if (matches.isEmpty()) {
            return AnchorResolution.failure("ANCHOR_NOT_FOUND", "No app_user found by " + anchorType);
        }
        if (matches.size() > 1) {
            return AnchorResolution.review("AMBIGUOUS_ANCHOR", "Multiple app_user rows matched by " + anchorType);
        }
        return AnchorResolution.match(matches.get(0));
    }

    private ImportAppUserPayload parseAppUserPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return ImportAppUserPayload.invalid("INVALID_PAYLOAD", "Import item payload must not be blank");
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(payload, MAP_TYPE);
            String lastName = normalizeOptional(asString(raw.get("lastName")));
            String firstName = normalizeOptional(asString(raw.get("firstName")));
            String middleName = normalizeOptional(asString(raw.get("middleName")));
            String employeeNumber = normalizeOptional(asString(raw.get("employeeNumber")));
            String externalId = normalizeOptional(asString(raw.get("externalId")));
            String statusValue = normalizeOptional(asString(raw.get("status")));
            if (lastName == null || firstName == null || statusValue == null) {
                return ImportAppUserPayload.invalid(
                    "INVALID_PAYLOAD",
                    "Payload must include lastName, firstName and status for APP_USER processing"
                );
            }
            UserStatus status = UserStatus.valueOf(statusValue);
            return ImportAppUserPayload.valid(lastName, firstName, middleName, employeeNumber, externalId, status);
        } catch (IllegalArgumentException exception) {
            return ImportAppUserPayload.invalid("INVALID_PAYLOAD", exception.getMessage());
        } catch (Exception exception) {
            return ImportAppUserPayload.invalid("INVALID_PAYLOAD", "Payload is not valid JSON for APP_USER processing");
        }
    }

    private boolean isNoChange(AppUser currentUser, ImportAppUserPayload payload) {
        return Objects.equals(currentUser.lastName(), payload.lastName())
            && Objects.equals(currentUser.firstName(), payload.firstName())
            && Objects.equals(currentUser.middleName(), payload.middleName())
            && currentUser.status() == payload.status();
    }

    private ImportJob saveInProgressJob(ImportJob job, Instant now) {
        return importJobRepository.saveImportJob(new ImportJob(
            job.id(),
            job.sourceType(),
            job.sourceRef(),
            job.initiatedByUserId(),
            ImportJobStatus.IN_PROGRESS,
            job.payload(),
            job.startedAt() == null ? now : job.startedAt(),
            null,
            job.totalItemCount(),
            job.processedItemCount(),
            job.appliedItemCount(),
            job.failedItemCount(),
            job.requiresReviewItemCount(),
            job.createdAt(),
            now
        ));
    }

    private ImportJobItem saveProcessingItem(ImportJobItem item, Instant now) {
        return importJobItemRepository.saveImportJobItem(new ImportJobItem(
            item.id(),
            item.importJobId(),
            item.itemNo(),
            item.targetEntityType(),
            item.externalId(),
            item.employeeNumber(),
            ImportItemStatus.PROCESSING,
            item.matchedEntityId(),
            item.payload(),
            null,
            null,
            null,
            item.createdAt(),
            now
        ));
    }

    private ImportJobItem toTerminalItem(
        ImportJobItem item,
        ImportItemStatus status,
        String matchedEntityId,
        String errorCode,
        String errorMessage,
        Instant now
    ) {
        return new ImportJobItem(
            item.id(),
            item.importJobId(),
            item.itemNo(),
            item.targetEntityType(),
            item.externalId(),
            item.employeeNumber(),
            status,
            matchedEntityId,
            item.payload(),
            errorCode,
            errorMessage,
            now,
            item.createdAt(),
            now
        );
    }

    private ImportJob finishJob(ImportJob baseJob, List<ImportJobItem> items, boolean fatalFailure, Instant now) {
        int totalItemCount = items.size();
        int processedItemCount = 0;
        int appliedItemCount = 0;
        int failedItemCount = 0;
        int requiresReviewItemCount = 0;

        for (ImportJobItem item : items) {
            if (isTerminal(item.status())) {
                processedItemCount++;
            }
            if (item.status() == ImportItemStatus.APPLIED) {
                appliedItemCount++;
            } else if (item.status() == ImportItemStatus.FAILED) {
                failedItemCount++;
            } else if (item.status() == ImportItemStatus.REQUIRES_REVIEW) {
                requiresReviewItemCount++;
            }
        }

        ImportJobStatus status;
        if (fatalFailure) {
            status = ImportJobStatus.FAILED;
        } else if (failedItemCount == 0 && requiresReviewItemCount == 0) {
            status = ImportJobStatus.COMPLETED;
        } else {
            status = ImportJobStatus.COMPLETED_WITH_ERRORS;
        }

        return new ImportJob(
            baseJob.id(),
            baseJob.sourceType(),
            baseJob.sourceRef(),
            baseJob.initiatedByUserId(),
            status,
            baseJob.payload(),
            baseJob.startedAt() == null ? now : baseJob.startedAt(),
            now,
            totalItemCount,
            processedItemCount,
            appliedItemCount,
            failedItemCount,
            requiresReviewItemCount,
            baseJob.createdAt(),
            now
        );
    }

    private boolean isTerminal(ImportJobStatus status) {
        return status == ImportJobStatus.COMPLETED
            || status == ImportJobStatus.COMPLETED_WITH_ERRORS
            || status == ImportJobStatus.FAILED;
    }

    private boolean isTerminal(ImportItemStatus status) {
        return status == ImportItemStatus.APPLIED
            || status == ImportItemStatus.NO_CHANGE
            || status == ImportItemStatus.FAILED
            || status == ImportItemStatus.REQUIRES_REVIEW;
    }

    private boolean isFatalOwnerCommandFailure(ImportJobItem item) {
        return item.status() == ImportItemStatus.FAILED
            && OWNER_COMMAND_FAILED.equals(item.errorCode());
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record AnchorResolution(AppUser matchedUser, String errorCode, String errorMessage, boolean failure, boolean requiresReview) {

        private static AnchorResolution match(AppUser matchedUser) {
            return new AnchorResolution(matchedUser, null, null, false, false);
        }

        private static AnchorResolution failure(String errorCode, String errorMessage) {
            return new AnchorResolution(null, errorCode, errorMessage, true, false);
        }

        private static AnchorResolution review(String errorCode, String errorMessage) {
            return new AnchorResolution(null, errorCode, errorMessage, false, true);
        }

        private String matchedUserId() {
            return matchedUser == null ? null : String.valueOf(matchedUser.id());
        }
    }

    private record ImportAppUserPayload(
        String lastName,
        String firstName,
        String middleName,
        String employeeNumber,
        String externalId,
        UserStatus status,
        String errorCode,
        String errorMessage,
        boolean valid
    ) {

        private static ImportAppUserPayload valid(
            String lastName,
            String firstName,
            String middleName,
            String employeeNumber,
            String externalId,
            UserStatus status
        ) {
            return new ImportAppUserPayload(
                lastName,
                firstName,
                middleName,
                employeeNumber,
                externalId,
                status,
                null,
                null,
                true
            );
        }

        private static ImportAppUserPayload invalid(String errorCode, String errorMessage) {
            return new ImportAppUserPayload(null, null, null, null, null, null, errorCode, errorMessage, false);
        }
    }

    private static final class DirectImportTypedOwnerCommandExecutor implements ImportTypedOwnerCommandExecutor {

        private final UserCommandService userCommandService;

        private DirectImportTypedOwnerCommandExecutor(UserCommandService userCommandService) {
            this.userCommandService = Objects.requireNonNull(userCommandService, "userCommandService must not be null");
        }

        @Override
        public AppUser updateAppUser(AppUser user) {
            return userCommandService.updateUser(user);
        }
    }
}
