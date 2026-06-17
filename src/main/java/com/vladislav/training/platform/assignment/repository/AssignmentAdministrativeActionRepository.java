package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import java.util.List;

public interface AssignmentAdministrativeActionRepository {

    AssignmentAdministrativeAction findAssignmentAdministrativeActionById(Long assignmentAdministrativeActionId);

    List<AssignmentAdministrativeAction> findAssignmentAdministrativeActionsByAssignmentId(Long assignmentId);

    AssignmentAdministrativeAction saveAssignmentAdministrativeAction(
        AssignmentAdministrativeAction assignmentAdministrativeAction
    );
}
