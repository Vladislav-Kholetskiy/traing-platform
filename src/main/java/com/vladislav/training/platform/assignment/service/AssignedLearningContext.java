package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContext;
import java.util.List;
import java.util.Objects;

public record AssignedLearningContext(
    Assignment assignment,
    List<AssignmentTest> assignmentTests,
    PublishedCourseLearningContext publishedLearningContext
) {

    public AssignedLearningContext {
        Objects.requireNonNull(assignment, "assignment must not be null");
        assignmentTests = assignmentTests == null ? List.of() : List.copyOf(assignmentTests);
        Objects.requireNonNull(publishedLearningContext, "publishedLearningContext must not be null");
    }
}
