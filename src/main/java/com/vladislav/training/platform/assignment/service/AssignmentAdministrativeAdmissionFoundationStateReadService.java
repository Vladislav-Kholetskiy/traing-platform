package com.vladislav.training.platform.assignment.service;

public interface AssignmentAdministrativeAdmissionFoundationStateReadService {

        AssignmentAdministrativeAdmissionFoundationState findAssignmentAdministrativeAdmissionFoundationState(Long assignmentId);

        record AssignmentAdministrativeAdmissionFoundationState(
        Long assignmentId,
        Long campaignId,
        boolean cancelled,
        boolean closed
    ) {
    }
}
