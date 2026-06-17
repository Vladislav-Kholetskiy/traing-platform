package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentTestRepository}.
 */
public interface AssignmentTestRepository {

    AssignmentTest findAssignmentTestById(Long assignmentTestId);

    List<AssignmentTest> findAssignmentTestsByAssignmentId(Long assignmentId);

    AssignmentTest findAssignmentTestByCountedResultId(Long countedResultId);

    AssignmentTest saveAssignmentTest(AssignmentTest assignmentTest);
}
