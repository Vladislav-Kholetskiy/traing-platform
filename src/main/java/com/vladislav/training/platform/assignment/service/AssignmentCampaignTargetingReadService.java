package com.vladislav.training.platform.assignment.service;

import java.util.List;

/**
 * Контракт сервиса {@code AssignmentCampaignTargetingReadService}.
 */
public interface AssignmentCampaignTargetingReadService {

    /**
     * Краткая проекция подразделения, доступного как цель кампании.
     */
    record TargetUnit(
        Long id,
        String name,
        String path
    ) {
    }

    List<TargetUnit> findAvailableTargetUnits();
}
