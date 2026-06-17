package com.vladislav.training.platform.integration.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.integration.controller.dto.ImportReviewApplyRequest;
import com.vladislav.training.platform.integration.controller.dto.ImportReviewRejectRequest;
import com.vladislav.training.platform.integration.controller.dto.ImportReviewResponse;
import com.vladislav.training.platform.integration.service.ImportItemReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code ImportItemReviewController}.
 */

@RestController
@Validated
@RequestMapping("/api/v1/admin/import-job-items")
class ImportItemReviewController {

    private final ImportItemReviewService importItemReviewService;
    private final InteractiveActorResolver interactiveActorResolver;

    ImportItemReviewController(
        ImportItemReviewService importItemReviewService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.importItemReviewService = Objects.requireNonNull(
            importItemReviewService,
            "importItemReviewService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @PostMapping("/{itemId}/apply-review")
    ImportReviewResponse applyReview(
        @PathVariable @Positive Long itemId,
        @Valid @RequestBody ImportReviewApplyRequest request
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(importItemReviewService.applyReview(
            actorUserId,
            itemId,
            new ImportItemReviewService.ImportReviewApplyCommand(request.matchedUserId())
        ));
    }

    @PostMapping("/{itemId}/reject-review")
    ImportReviewResponse rejectReview(
        @PathVariable @Positive Long itemId,
        @Valid @RequestBody ImportReviewRejectRequest request
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return toResponse(importItemReviewService.rejectReview(
            actorUserId,
            itemId,
            new ImportItemReviewService.ImportReviewRejectCommand(request.reason())
        ));
    }

    private ImportReviewResponse toResponse(ImportItemReviewService.ImportReviewResult result) {
        return new ImportReviewResponse(
            result.itemId(),
            result.importJobId(),
            result.status(),
            result.matchedEntityId(),
            result.errorCode(),
            result.errorMessage(),
            result.processedAt(),
            result.updatedAt()
        );
    }
}
