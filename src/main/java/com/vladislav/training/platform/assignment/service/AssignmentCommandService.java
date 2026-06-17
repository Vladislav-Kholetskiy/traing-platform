package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import java.time.Instant;

public interface AssignmentCommandService {

        Assignment createAssignment(Assignment assignment);

        AssignmentTest createAssignmentTest(AssignmentTest assignmentTest);

        AssignmentTest closeAssignmentTestWithCountedResult(Long assignmentTestId, Long countedResultId, Instant closedAt);
}
