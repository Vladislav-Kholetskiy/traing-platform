package com.vladislav.training.platform.integration.personnel.model;

import java.util.Set;

/**
 * Запись данных {@code PersonnelCurrentState}.
 */
public record PersonnelCurrentState(
    Long userId,
    String employeeNumber,
    String externalId,
    String userStatus,
    String primaryHomeOrgUnitCode,
    Set<String> activeRoleCodes,
    boolean managementRelationActive,
    Set<String> activeAccessScopeCodes,
    Set<String> activeTemporaryRoleCodes,
    Set<String> activeTemporaryAccessScopeCodes,
    boolean activeTemporaryManagementDelegation
) {

    public PersonnelCurrentState(
        String employeeNumber,
        String externalId,
        String userStatus,
        String primaryHomeOrgUnitCode,
        Set<String> activeRoleCodes,
        boolean managementRelationActive,
        Set<String> activeAccessScopeCodes,
        Set<String> activeTemporaryRoleCodes,
        Set<String> activeTemporaryAccessScopeCodes,
        boolean activeTemporaryManagementDelegation
    ) {
        this(
            null,
            employeeNumber,
            externalId,
            userStatus,
            primaryHomeOrgUnitCode,
            activeRoleCodes,
            managementRelationActive,
            activeAccessScopeCodes,
            activeTemporaryRoleCodes,
            activeTemporaryAccessScopeCodes,
            activeTemporaryManagementDelegation
        );
    }

    public PersonnelCurrentState {
        if (employeeNumber == null || employeeNumber.isBlank()) {
            throw new IllegalArgumentException("employeeNumber must not be blank");
        }
        activeRoleCodes = Set.copyOf(activeRoleCodes);
        activeAccessScopeCodes = Set.copyOf(activeAccessScopeCodes);
        activeTemporaryRoleCodes = Set.copyOf(activeTemporaryRoleCodes);
        activeTemporaryAccessScopeCodes = Set.copyOf(activeTemporaryAccessScopeCodes);
    }
}
