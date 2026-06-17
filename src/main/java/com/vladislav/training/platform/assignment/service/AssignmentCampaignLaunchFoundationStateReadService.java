package com.vladislav.training.platform.assignment.service;

/**
 * Контракт сервиса {@code AssignmentCampaignLaunchFoundationStateReadService}.
 */
public interface AssignmentCampaignLaunchFoundationStateReadService {

    /**
     * Читает минимальное состояние, привязанное к якорю запуска кампании.
     */
    AssignmentCampaignLaunchFoundationState findAssignmentCampaignLaunchFoundationState(
        AssignmentCampaignLaunchAdmissionAnchor launchAdmissionAnchor
    );

    /**
     * Входной якорь запуска, построенный из команды, а не из уже сохраненной кампании.
     */
    record AssignmentCampaignLaunchAdmissionAnchor(
        String sourceType,
        String sourceRef
    ) {
    }

    /**
     * Минимальный набор фактов, доступный до старта записи кампании.
     */
    record AssignmentCampaignLaunchFoundationState(
        boolean sourceAnchorAlreadyMaterialized
    ) {
    }
}
