package com.vladislav.training.platform.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса {@code ImportItemReviewServiceImpl}.
 */

@Service
@Transactional
public class ImportItemReviewServiceImpl implements ImportItemReviewService {

    private static final String TARGET_APP_USER = "APP_USER";
    private static final String OWNER_COMMAND_FAILED = "OWNER_COMMAND_FAILED";
    private static final String REVIEW_REJECTED = "REVIEW_REJECTED";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemRepository importJobItemRepository;
    private final AppUserRepository appUserRepository;
    private final ImportTypedOwnerCommandExecutor importTypedOwnerCommandExecutor;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final UtcClock utcClock;
    private final ObjectMapper objectMapper;

    @Autowired
    public ImportItemReviewServiceImpl(
        ImportJobRepository importJobRepository,
        ImportJobItemRepository importJobItemRepository,
        AppUserRepository appUserRepository,
        ImportTypedOwnerCommandExecutor importTypedOwnerCommandExecutor,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
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
        this.capabilityAdmissionPolicy = Objects.requireNonNull(
            capabilityAdmissionPolicy,
            "capabilityAdmissionPolicy must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
        this.objectMapper = new ObjectMapper();
    }

    public ImportItemReviewServiceImpl(
        ImportJobRepository importJobRepository,
        ImportJobItemRepository importJobItemRepository,
        AppUserRepository appUserRepository,
        UserCommandService userCommandService,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        UtcClock utcClock
    ) {
        this(
            importJobRepository,
            importJobItemRepository,
            appUserRepository,
            new DirectImportTypedOwnerCommandExecutor(userCommandService),
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            utcClock
        );
    }

    @Override
    public ImportReviewResult applyReview(Long actorUserId, Long itemId, ImportReviewApplyCommand command) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createImportItemReviewApply(actorUserId, itemId);
        capabilityAdmissionPolicy.check(request);

        ImportJobItem item = importJobItemRepository.findImportJobItemById(itemId);
        ensureRequiresReview(item);

        Long matchedUserId = command == null ? null : command.matchedUserId();
        if (matchedUserId == null) {
            throw new ValidationException("matchedUserId must not be null");
        }

        Instant now = utcClock.now();
        ImportJobItem terminalItem;
        if (!TARGET_APP_USER.equals(item.targetEntityType())) {
            terminalItem = saveItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                null,
                "UNSUPPORTED_TARGET_TYPE",
                "Для этого типа сущности ручной разбор не поддерживается: targetEntityType=" + item.targetEntityType(),
                now
            ));
        } else {
            AppUser matchedUser = appUserRepository.findUserById(matchedUserId);
            if (matchedUser == null) {
                throw new ValidationException("matchedUserId must reference an existing app_user");
            }
            terminalItem = applyAppUserReview(item, matchedUser, now);
        }

        saveRecalculatedJob(terminalItem.importJobId(), terminalItem, now);
        return toResult(terminalItem);
    }

    @Override
    public ImportReviewResult rejectReview(Long actorUserId, Long itemId, ImportReviewRejectCommand command) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createImportItemReviewReject(actorUserId, itemId);
        capabilityAdmissionPolicy.check(request);

        ImportJobItem item = importJobItemRepository.findImportJobItemById(itemId);
        ensureRequiresReview(item);

        Instant now = utcClock.now();
        String reason = normalizeOptional(command == null ? null : command.reason());
        ImportJobItem terminalItem = saveItem(toTerminalItem(
            item,
            ImportItemStatus.FAILED,
            item.matchedEntityId(),
            REVIEW_REJECTED,
            reason == null ? "Review rejected by actor" : reason,
            now
        ));

        saveRecalculatedJob(terminalItem.importJobId(), terminalItem, now);
        return toResult(terminalItem);
    }

    private ImportJobItem applyAppUserReview(ImportJobItem item, AppUser matchedUser, Instant now) {
        ImportAppUserPayload payload = parseAppUserPayload(item.payload());
        if (!payload.valid()) {
            return saveItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                String.valueOf(matchedUser.id()),
                payload.errorCode(),
                payload.errorMessage(),
                now
            ));
        }
        if (payload.employeeNumber() != null && !payload.employeeNumber().equals(matchedUser.employeeNumber())) {
            return saveItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                String.valueOf(matchedUser.id()),
                "IDENTITY_MISMATCH",
                "Payload employeeNumber does not match reviewed app_user",
                now
            ));
        }
        if (payload.externalId() != null && !Objects.equals(payload.externalId(), matchedUser.externalId())) {
            return saveItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                String.valueOf(matchedUser.id()),
                "IDENTITY_MISMATCH",
                "Payload externalId does not match reviewed app_user",
                now
            ));
        }
        if (isNoChange(matchedUser, payload)) {
            return saveItem(toTerminalItem(
                item,
                ImportItemStatus.NO_CHANGE,
                String.valueOf(matchedUser.id()),
                null,
                null,
                now
            ));
        }

        try {
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
            return saveItem(toTerminalItem(
                item,
                ImportItemStatus.APPLIED,
                String.valueOf(matchedUser.id()),
                null,
                null,
                now
            ));
        } catch (RuntimeException exception) {
            return saveItem(toTerminalItem(
                item,
                ImportItemStatus.FAILED,
                String.valueOf(matchedUser.id()),
                OWNER_COMMAND_FAILED,
                exception.getMessage(),
                now
            ));
        }
    }

    private void saveRecalculatedJob(Long importJobId, ImportJobItem updatedItem, Instant now) {
        ImportJob baseJob = importJobRepository.findImportJobById(importJobId);
        List<ImportJobItem> items = new ArrayList<>(importJobItemRepository.findImportJobItemsByJobId(importJobId));
        for (int index = 0; index < items.size(); index++) {
            if (Objects.equals(items.get(index).id(), updatedItem.id())) {
                items.set(index, updatedItem);
                break;
            }
        }
        importJobRepository.saveImportJob(finishJob(baseJob, items, now));
    }

    private ImportJob finishJob(ImportJob baseJob, List<ImportJobItem> items, Instant now) {
        int totalItemCount = items.size();
        int processedItemCount = 0;
        int appliedItemCount = 0;
        int failedItemCount = 0;
        int requiresReviewItemCount = 0;
        boolean fatalFailure = false;

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
            if (isFatalOwnerCommandFailure(item)) {
                fatalFailure = true;
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
            baseJob.startedAt(),
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

    private ImportJobItem saveItem(ImportJobItem item) {
        return importJobItemRepository.saveImportJobItem(item);
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

    private void ensureRequiresReview(ImportJobItem item) {
        if (item.status() != ImportItemStatus.REQUIRES_REVIEW) {
            throw new ConflictException("Import review action is allowed only for items in REQUIRES_REVIEW status");
        }
    }

    private boolean isTerminal(ImportItemStatus status) {
        return status == ImportItemStatus.APPLIED
            || status == ImportItemStatus.NO_CHANGE
            || status == ImportItemStatus.FAILED
            || status == ImportItemStatus.REQUIRES_REVIEW;
    }

    private boolean isFatalOwnerCommandFailure(ImportJobItem item) {
        return item.status() == ImportItemStatus.FAILED && OWNER_COMMAND_FAILED.equals(item.errorCode());
    }

    private boolean isNoChange(AppUser currentUser, ImportAppUserPayload payload) {
        return Objects.equals(currentUser.lastName(), payload.lastName())
            && Objects.equals(currentUser.firstName(), payload.firstName())
            && Objects.equals(currentUser.middleName(), payload.middleName())
            && currentUser.status() == payload.status();
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
                    "Payload must include lastName, firstName and status for APP_USER review"
                );
            }
            return ImportAppUserPayload.valid(
                lastName,
                firstName,
                middleName,
                employeeNumber,
                externalId,
                UserStatus.valueOf(statusValue)
            );
        } catch (IllegalArgumentException exception) {
            return ImportAppUserPayload.invalid("INVALID_PAYLOAD", exception.getMessage());
        } catch (Exception exception) {
            return ImportAppUserPayload.invalid("INVALID_PAYLOAD", "Payload is not valid JSON for APP_USER review");
        }
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

    private ImportReviewResult toResult(ImportJobItem item) {
        return new ImportReviewResult(
            item.id(),
            item.importJobId(),
            item.status(),
            item.matchedEntityId(),
            item.errorCode(),
            item.errorMessage(),
            item.processedAt(),
            item.updatedAt()
        );
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
