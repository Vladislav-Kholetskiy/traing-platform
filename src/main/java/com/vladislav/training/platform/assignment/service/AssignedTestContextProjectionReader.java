package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;

interface AssignedTestContextProjectionReader {

    AssignedTestContext readAssignedTestContext(AssignmentTest assignmentTest);
}
