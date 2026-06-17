package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.assignment.controller.dto.ManagerialCurrentSupervisionResponse;
import com.vladislav.training.platform.assignment.service.ManagerialCurrentSupervisionQueryService;
import com.vladislav.training.platform.common.time.UtcClock;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code ManagerialCurrentSupervisionController}.
 */
@RestController
@RequestMapping("/api/v1/managerial/current-supervision")
class ManagerialCurrentSupervisionController {

    private final ManagerialCurrentSupervisionQueryService managerialCurrentSupervisionQueryService;
    private final InteractiveActorResolver interactiveActorResolver;
    private final UtcClock utcClock;

    ManagerialCurrentSupervisionController(
        ManagerialCurrentSupervisionQueryService managerialCurrentSupervisionQueryService,
        InteractiveActorResolver interactiveActorResolver,
        UtcClock utcClock
    ) {
        this.managerialCurrentSupervisionQueryService = Objects.requireNonNull(
            managerialCurrentSupervisionQueryService,
            "managerialCurrentSupervisionQueryService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @GetMapping
    List<ManagerialCurrentSupervisionResponse> findCurrentSupervision() {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return managerialCurrentSupervisionQueryService.findCurrentSupervision(
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(actorUserId, utcClock.now())
        ).stream()
            .map(this::toResponse)
            .toList();
    }

    private ManagerialCurrentSupervisionResponse toResponse(
        ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow row
    ) {
        return new ManagerialCurrentSupervisionResponse(
            row.assignmentId(),
            row.userId(),
            row.userDisplayName(),
            row.courseId(),
            row.courseName(),
            row.assignmentTestCount(),
            row.assignedAt(),
            row.deadlineAt(),
            row.assignmentStatus()
        );
    }
}
