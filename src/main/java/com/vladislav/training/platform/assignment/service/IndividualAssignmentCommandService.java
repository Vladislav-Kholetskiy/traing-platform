package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;

/**
 * Контракт командного сервиса {@code IndividualAssignmentCommandService}.
 */
public interface IndividualAssignmentCommandService {

    AssignmentCampaign launchIndividualAssignment(LaunchIndividualAssignmentCommand command);
}
