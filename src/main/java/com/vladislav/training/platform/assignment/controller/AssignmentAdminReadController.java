package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.assignment.controller.dto.AssignmentResponse;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.service.AssignmentQueryService;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.service.CourseQueryService;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/assignments")
public class AssignmentAdminReadController {

    private final AssignmentQueryService assignmentQueryService;
    private final CourseQueryService courseQueryService;

    public AssignmentAdminReadController(
        AssignmentQueryService assignmentQueryService,
        CourseQueryService courseQueryService
    ) {
        this.assignmentQueryService = Objects.requireNonNull(
            assignmentQueryService,
            "assignmentQueryService must not be null"
        );
        this.courseQueryService = Objects.requireNonNull(courseQueryService, "courseQueryService must not be null");
    }

    @GetMapping
    public List<AssignmentResponse> findAssignments(
        @RequestParam(required = false) Long campaignId,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) AssignmentStatus status
    ) {
        List<Assignment> assignments;
        if (campaignId != null) {
            assignments = assignmentQueryService.findAssignmentsByCampaignId(campaignId);
        } else if (userId != null && status != null) {
            assignments = assignmentQueryService.findAssignmentsByUserIdAndStatus(userId, status);
        } else if (userId != null) {
            assignments = assignmentQueryService.findAssignmentsByUserId(userId);
        } else {
            assignments = assignmentQueryService.findAllAssignments();
        }
        return assignments.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{assignmentId}")
    public AssignmentResponse findAssignmentById(@PathVariable Long assignmentId) {
        return toResponse(assignmentQueryService.findAssignmentById(assignmentId));
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
