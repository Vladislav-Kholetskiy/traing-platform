package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.testing.controller.dto.SelfAttemptAbandonResponse;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonSequencingService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code SelfAttemptAbandonController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/self-attempt-abandonments")
public class SelfAttemptAbandonController {

    private final SelfAttemptAbandonSequencingService selfAttemptAbandonSequencingService;

    public SelfAttemptAbandonController(SelfAttemptAbandonSequencingService selfAttemptAbandonSequencingService) {
        this.selfAttemptAbandonSequencingService = Objects.requireNonNull(
            selfAttemptAbandonSequencingService,
            "selfAttemptAbandonSequencingService must not be null"
        );
    }

    @PostMapping("/attempts/{testAttemptId}")
    public SelfAttemptAbandonResponse abandonSelfAttempt(@PathVariable @Positive Long testAttemptId) {
        return new SelfAttemptAbandonResponse(selfAttemptAbandonSequencingService.abandonSelfAttempt(testAttemptId));
    }
}
