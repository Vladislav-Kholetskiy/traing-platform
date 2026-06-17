package com.vladislav.training.platform.integration.personnel.model;

import java.util.Set;

/**
 * Запись данных {@code PersonnelPositionMapping}.
 */
public record PersonnelPositionMapping(
    String positionCode,
    Set<String> roleCodes,
    boolean managementRelationRequired,
    String accessScopeRequired,
    boolean temporaryManagementDelegationRequired,
    boolean additiveOnly
) {

    public PersonnelPositionMapping {
        if (positionCode == null || positionCode.isBlank()) {
            throw new IllegalArgumentException("positionCode must not be blank");
        }
        roleCodes = Set.copyOf(roleCodes);
    }
}
