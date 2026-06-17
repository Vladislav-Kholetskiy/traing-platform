package com.vladislav.training.platform.application.actor;

import java.util.List;

/**
 * Ответ {@code CurrentActorResponse}.
 */
public record CurrentActorResponse(
    Long actorUserId,
    String username,
    String displayName,
    String positionTitle,
    String employeeNumber,
    String primaryOrganizationalUnitName,
    String primaryOrganizationalUnitPath,
    List<String> roles,
    List<String> enabledSections
) {

    public CurrentActorResponse(
        Long actorUserId,
        String username,
        String displayName,
        String employeeNumber,
        String primaryOrganizationalUnitName,
        String primaryOrganizationalUnitPath,
        List<String> roles,
        List<String> enabledSections
    ) {
        this(
            actorUserId,
            username,
            displayName,
            null,
            employeeNumber,
            primaryOrganizationalUnitName,
            primaryOrganizationalUnitPath,
            roles,
            enabledSections
        );
    }
}
