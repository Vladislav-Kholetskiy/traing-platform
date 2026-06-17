package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptSubmitResponse;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code AssignedAttemptSubmitController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/assigned-attempt-submissions")
public class AssignedAttemptSubmitController {

    private final AssignedAttemptSubmissionService assignedAttemptSubmissionService;

    public AssignedAttemptSubmitController(AssignedAttemptSubmissionService assignedAttemptSubmissionService) {
        this.assignedAttemptSubmissionService = Objects.requireNonNull(
            assignedAttemptSubmissionService,
            "assignedAttemptSubmissionService must not be null"
        );
    }

    @PostMapping("/attempts/{testAttemptId}")
    public AssignedAttemptSubmitResponse submitAssignedAttempt(@PathVariable @Positive Long testAttemptId) {
        var outcome = assignedAttemptSubmissionService.submitAssignedAttempt(testAttemptId);
        return new AssignedAttemptSubmitResponse(outcome.attemptId(), outcome.terminalStatus(), outcome.recordedResult());
    }
}
