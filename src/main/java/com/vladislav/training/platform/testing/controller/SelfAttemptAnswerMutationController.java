package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationRequest;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.service.SelfAttemptAnswerMutationEntryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code SelfAttemptAnswerMutationController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/self-attempt-answers")
public class SelfAttemptAnswerMutationController {

    private final SelfAttemptAnswerMutationEntryService selfAttemptAnswerMutationEntryService;

    public SelfAttemptAnswerMutationController(
        SelfAttemptAnswerMutationEntryService selfAttemptAnswerMutationEntryService
    ) {
        this.selfAttemptAnswerMutationEntryService = Objects.requireNonNull(
            selfAttemptAnswerMutationEntryService,
            "selfAttemptAnswerMutationEntryService must not be null"
        );
    }

    @PutMapping("/attempts/{testAttemptId}/questions/{questionId}")
    public ActiveAttemptAnswerMutationResponse saveOrReplaceSelfAnswer(
        @PathVariable @Positive Long testAttemptId,
        @PathVariable @Positive Long questionId,
        @RequestBody @Valid ActiveAttemptAnswerMutationRequest request
    ) {
        TestAttempt updatedAttempt = selfAttemptAnswerMutationEntryService.saveOrReplaceSelfAnswer(testAttemptId, questionId, request);
        return toResponse(updatedAttempt, questionId);
    }

    @DeleteMapping("/attempts/{testAttemptId}/questions/{questionId}")
    public ActiveAttemptAnswerMutationResponse clearSelfAnswer(
        @PathVariable @Positive Long testAttemptId,
        @PathVariable @Positive Long questionId
    ) {
        TestAttempt updatedAttempt = selfAttemptAnswerMutationEntryService.clearSelfAnswer(testAttemptId, questionId);
        return toResponse(updatedAttempt, questionId);
    }

    private ActiveAttemptAnswerMutationResponse toResponse(TestAttempt updatedAttempt, Long questionId) {
        return new ActiveAttemptAnswerMutationResponse(
            updatedAttempt.id(),
            questionId,
            updatedAttempt.status(),
            updatedAttempt.lastActivityAt()
        );
    }
}
