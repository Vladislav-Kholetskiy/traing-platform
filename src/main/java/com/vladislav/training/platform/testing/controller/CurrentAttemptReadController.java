package com.vladislav.training.platform.testing.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.testing.controller.dto.CurrentAttemptResponse;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.service.AssignedCurrentAttemptReadService;
import com.vladislav.training.platform.testing.service.SelfCurrentAttemptReadService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code CurrentAttemptReadController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/current-attempts")
public class CurrentAttemptReadController {

    private final AssignedCurrentAttemptReadService assignedCurrentAttemptReadService;
    private final SelfCurrentAttemptReadService selfCurrentAttemptReadService;
    private final InteractiveActorResolver interactiveActorResolver;

    public CurrentAttemptReadController(
        AssignedCurrentAttemptReadService assignedCurrentAttemptReadService,
        SelfCurrentAttemptReadService selfCurrentAttemptReadService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.assignedCurrentAttemptReadService = Objects.requireNonNull(
            assignedCurrentAttemptReadService,
            "assignedCurrentAttemptReadService must not be null"
        );
        this.selfCurrentAttemptReadService = Objects.requireNonNull(
            selfCurrentAttemptReadService,
            "selfCurrentAttemptReadService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @GetMapping("/assigned/assignments/{assignmentId}/assignment-tests/{assignmentTestId}")
    public CurrentAttemptResponse findCurrentAssignedAttempt(
        @PathVariable @Positive Long assignmentId,
        @PathVariable @Positive Long assignmentTestId
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        TestAttempt activeAttempt = assignedCurrentAttemptReadService.findCurrentAssignedAttemptForActor(
            actorUserId,
            assignmentId,
            assignmentTestId
        );
        return toResponse(activeAttempt);
    }

    @GetMapping("/self/tests/{testId}")
    public CurrentAttemptResponse findCurrentSelfAttempt(@PathVariable @Positive Long testId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        TestAttempt activeAttempt = selfCurrentAttemptReadService.findCurrentSelfAttemptForActor(actorUserId, testId);
        return toResponse(activeAttempt);
    }

    private CurrentAttemptResponse toResponse(TestAttempt attempt) {
        return new CurrentAttemptResponse(
            attempt.id(),
            attempt.userId(),
            attempt.testId(),
            attempt.assignmentTestId(),
            attempt.attemptMode(),
            attempt.status(),
            attempt.startedAt(),
            attempt.completedAt(),
            attempt.expiredAt(),
            attempt.abandonedAt(),
            attempt.lastActivityAt()
        );
    }
}
