package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.testing.controller.dto.SelfAttemptSubmitResponse;
import com.vladislav.training.platform.testing.service.SelfAttemptSubmitSequencingService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code SelfAttemptSubmitController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/self-attempt-submissions")
public class SelfAttemptSubmitController {

    private final SelfAttemptSubmitSequencingService selfAttemptSubmitSequencingService;

    public SelfAttemptSubmitController(SelfAttemptSubmitSequencingService selfAttemptSubmitSequencingService) {
        this.selfAttemptSubmitSequencingService = Objects.requireNonNull(
            selfAttemptSubmitSequencingService,
            "selfAttemptSubmitSequencingService must not be null"
        );
    }

    @PostMapping("/attempts/{testAttemptId}")
    public SelfAttemptSubmitResponse submitSelfAttempt(@PathVariable @Positive Long testAttemptId) {
        return new SelfAttemptSubmitResponse(testAttemptId, selfAttemptSubmitSequencingService.submitSelfAttempt(testAttemptId));
    }
}
