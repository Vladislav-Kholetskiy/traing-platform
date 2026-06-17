package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import java.time.Instant;

/**
 * Контракт сервиса {@code ImportItemReviewService}.
 */
public interface ImportItemReviewService {

    ImportReviewResult applyReview(Long actorUserId, Long itemId, ImportReviewApplyCommand command);

    ImportReviewResult rejectReview(Long actorUserId, Long itemId, ImportReviewRejectCommand command);

    record ImportReviewApplyCommand(
        Long matchedUserId
    ) {
    }

    record ImportReviewRejectCommand(
        String reason
    ) {
    }

    record ImportReviewResult(
        Long itemId,
        Long importJobId,
        ImportItemStatus status,
        String matchedEntityId,
        String errorCode,
        String errorMessage,
        Instant processedAt,
        Instant updatedAt
    ) {
    }
}
