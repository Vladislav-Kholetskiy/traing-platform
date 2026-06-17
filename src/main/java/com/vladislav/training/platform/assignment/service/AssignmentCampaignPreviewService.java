package com.vladislav.training.platform.assignment.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Контракт сервиса {@code AssignmentCampaignPreviewService}.
 */
public interface AssignmentCampaignPreviewService {

    AssignmentCampaignRecipientPoolPreview previewRecipientPool(RecipientPoolPreviewRequest request);

    /**
     * Минимальный вход для расчёта предпросмотра до появления сохранённой кампании.
     */
    record RecipientPoolPreviewRequest(
        String sourceType,
        String sourceRef
    ) {
        public RecipientPoolPreviewRequest {
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(sourceRef, "sourceRef must not be null");
            if (sourceType.isBlank()) {
                throw new IllegalArgumentException("sourceType must not be blank");
            }
            if (sourceRef.isBlank()) {
                throw new IllegalArgumentException("sourceRef must not be blank");
            }
        }
    }

    /**
     * Эфемерный результат предпросмотра текущего пула получателей.
     */
    record AssignmentCampaignRecipientPoolPreview(
        String sourceType,
        String sourceRef,
        Long targetingUnitId,
        String targetingUnitPath,
        String targetingUnitName,
        boolean targetingBasisActive,
        Instant previewedAt,
        List<PreviewRecipient> recipients
    ) {
        public AssignmentCampaignRecipientPoolPreview {
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(sourceRef, "sourceRef must not be null");
            Objects.requireNonNull(targetingUnitPath, "targetingUnitPath must not be null");
            Objects.requireNonNull(targetingUnitName, "targetingUnitName must not be null");
            Objects.requireNonNull(previewedAt, "previewedAt must not be null");
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
            if (sourceType.isBlank()) {
                throw new IllegalArgumentException("sourceType must not be blank");
            }
            if (sourceRef.isBlank()) {
                throw new IllegalArgumentException("sourceRef must not be blank");
            }
            if (targetingUnitPath.isBlank()) {
                throw new IllegalArgumentException("targetingUnitPath must not be blank");
            }
            if (targetingUnitName.isBlank()) {
                throw new IllegalArgumentException("targetingUnitName must not be blank");
            }
        }
    }

    /**
     * Краткая проекция участника внутри текущего предпросмотра.
     */
    record PreviewRecipient(
        Long userId,
        String employeeNumber,
        String lastName,
        String firstName,
        String middleName
    ) {
        public PreviewRecipient {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(employeeNumber, "employeeNumber must not be null");
            Objects.requireNonNull(lastName, "lastName must not be null");
            Objects.requireNonNull(firstName, "firstName must not be null");
            if (employeeNumber.isBlank()) {
                throw new IllegalArgumentException("employeeNumber must not be blank");
            }
            if (lastName.isBlank()) {
                throw new IllegalArgumentException("lastName must not be blank");
            }
            if (firstName.isBlank()) {
                throw new IllegalArgumentException("firstName must not be blank");
            }
        }
    }
}
