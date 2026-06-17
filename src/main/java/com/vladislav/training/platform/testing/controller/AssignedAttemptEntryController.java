package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptEntryResponse;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code AssignedAttemptEntryController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/assigned-attempt-entries")
public class AssignedAttemptEntryController {

    private final AssignedAttemptEntryService assignedAttemptEntryService;

    public AssignedAttemptEntryController(AssignedAttemptEntryService assignedAttemptEntryService) {
        this.assignedAttemptEntryService = Objects.requireNonNull(
            assignedAttemptEntryService,
            "assignedAttemptEntryService must not be null"
        );
    }

    @PostMapping("/assignments/{assignmentId}/assignment-tests/{assignmentTestId}")
    public AssignedAttemptEntryResponse enterAssignedAttempt(
        @PathVariable @Positive Long assignmentId,
        @PathVariable @Positive Long assignmentTestId
    ) {
        return toResponse(assignedAttemptEntryService.enterAssignedAttempt(assignmentId, assignmentTestId));
    }

    private AssignedAttemptEntryResponse toResponse(TestAttempt attempt) {
        return new AssignedAttemptEntryResponse(
            attempt.id(),
            attempt.assignmentTestId(),
            attempt.testId(),
            attempt.attemptMode(),
            attempt.status(),
            attempt.startedAt(),
            attempt.lastActivityAt()
        );
    }
}
