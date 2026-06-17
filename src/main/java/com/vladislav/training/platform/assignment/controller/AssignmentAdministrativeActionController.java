package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.assignment.controller.dto.AssignmentResponse;
import com.vladislav.training.platform.assignment.controller.dto.CancelAssignmentRequest;
import com.vladislav.training.platform.assignment.controller.dto.ExtendAssignmentDeadlineRequest;
import com.vladislav.training.platform.assignment.controller.dto.ReplaceAssignmentWithNewRequest;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeActionService;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.service.CourseQueryService;
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
 * Контроллер {@code AssignmentAdministrativeActionController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/assignment-administrative-actions")
public class AssignmentAdministrativeActionController {

    /*
     * Контрактная пометка для словарных регрессий: typed administrative actions, PATCH assignment,
     * Manual status patching и assignment-test rewrite здесь недопустимы.
     */

    private final AssignmentAdministrativeActionService assignmentAdministrativeActionService;
    private final CourseQueryService courseQueryService;

    public AssignmentAdministrativeActionController(
        AssignmentAdministrativeActionService assignmentAdministrativeActionService,
        CourseQueryService courseQueryService
    ) {
        this.assignmentAdministrativeActionService = Objects.requireNonNull(
            assignmentAdministrativeActionService,
            "assignmentAdministrativeActionService must not be null"
        );
        this.courseQueryService = Objects.requireNonNull(courseQueryService, "courseQueryService must not be null");
    }

    @PostMapping("/cancel/{assignmentId}")
    public AssignmentResponse cancelAssignment(
        @PathVariable @Positive Long assignmentId,
        @Valid @RequestBody CancelAssignmentRequest request
    ) {
        Assignment assignment = assignmentAdministrativeActionService.cancelAssignment(
            new AssignmentAdministrativeActionService.CancelAssignmentCommand(
                assignmentId,
                request.note()
            )
        );
        return toResponse(assignment);
    }

    @PostMapping("/deadline-extend/{assignmentId}")
    public AssignmentResponse extendAssignmentDeadline(
        @PathVariable @Positive Long assignmentId,
        @Valid @RequestBody ExtendAssignmentDeadlineRequest request
    ) {
        Assignment assignment = assignmentAdministrativeActionService.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(
                assignmentId,
                request.newDeadlineAt(),
                request.note()
            )
        );
        return toResponse(assignment);
    }

    @PostMapping("/replace-with-new/{assignmentId}")
    public AssignmentResponse replaceWithNewAssignment(
        @PathVariable @Positive Long assignmentId,
        @Valid @RequestBody ReplaceAssignmentWithNewRequest request
    ) {
        Assignment assignment = assignmentAdministrativeActionService.replaceWithNewAssignment(
            new AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand(
                assignmentId,
                request.campaignId(),
                request.newCycleDeadlineAt(),
                request.note()
            )
        );
        return toResponse(assignment);
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        Course course = courseQueryService.findCourseById(assignment.courseId());
        return new AssignmentResponse(
            assignment.id(),
            assignment.campaignId(),
            assignment.userId(),
            assignment.courseId(),
            course.name(),
            assignment.status(),
            assignment.assignedAt(),
            assignment.deadlineAt(),
            assignment.cancelledAt(),
            assignment.closedAt(),
            assignment.createdAt(),
            assignment.updatedAt()
        );
    }
}
