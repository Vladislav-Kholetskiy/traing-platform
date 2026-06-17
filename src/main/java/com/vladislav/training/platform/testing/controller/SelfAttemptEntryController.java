package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.testing.controller.dto.SelfAttemptEntryResponse;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code SelfAttemptEntryController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/self-attempt-entries")
public class SelfAttemptEntryController {

    private final SelfAttemptEntryService selfAttemptEntryService;

    public SelfAttemptEntryController(SelfAttemptEntryService selfAttemptEntryService) {
        this.selfAttemptEntryService = Objects.requireNonNull(
            selfAttemptEntryService,
            "selfAttemptEntryService must not be null"
        );
    }

    @PostMapping("/tests/{testId}")
    public SelfAttemptEntryResponse startOrContinueSelfAttempt(@PathVariable @Positive Long testId) {
        return toResponse(selfAttemptEntryService.startOrContinueSelfAttempt(testId));
    }

    private SelfAttemptEntryResponse toResponse(TestAttempt attempt) {
        return new SelfAttemptEntryResponse(
            attempt.id(),
            attempt.testId(),
            attempt.attemptMode(),
            attempt.status(),
            attempt.startedAt(),
            attempt.lastActivityAt()
        );
    }
}
