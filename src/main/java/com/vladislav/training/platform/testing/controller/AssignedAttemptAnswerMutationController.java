package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationRequest;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.service.AssignedAttemptAnswerMutationEntryService;
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
 * Контроллер {@code AssignedAttemptAnswerMutationController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/assigned-attempt-answers")
public class AssignedAttemptAnswerMutationController {

    private final AssignedAttemptAnswerMutationEntryService assignedAttemptAnswerMutationEntryService;

    public AssignedAttemptAnswerMutationController(
        AssignedAttemptAnswerMutationEntryService assignedAttemptAnswerMutationEntryService
    ) {
        this.assignedAttemptAnswerMutationEntryService = Objects.requireNonNull(
            assignedAttemptAnswerMutationEntryService,
            "assignedAttemptAnswerMutationEntryService must not be null"
        );
    }

    @PutMapping("/attempts/{testAttemptId}/questions/{questionId}")
    public ActiveAttemptAnswerMutationResponse saveOrReplaceAssignedAnswer(
        @PathVariable @Positive Long testAttemptId,
        @PathVariable @Positive Long questionId,
        @RequestBody @Valid ActiveAttemptAnswerMutationRequest request
    ) {
        TestAttempt updatedAttempt = assignedAttemptAnswerMutationEntryService.saveOrReplaceAssignedAnswer(
            testAttemptId,
            questionId,
            request
        );
        return toResponse(updatedAttempt, questionId);
    }

    @DeleteMapping("/attempts/{testAttemptId}/questions/{questionId}")
    public ActiveAttemptAnswerMutationResponse clearAssignedAnswer(
        @PathVariable @Positive Long testAttemptId,
        @PathVariable @Positive Long questionId
    ) {
        TestAttempt updatedAttempt = assignedAttemptAnswerMutationEntryService.clearAssignedAnswer(testAttemptId, questionId);
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
