package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import java.time.Instant;

/**
 * Запись данных {@code UserAdministrationOrganizationAssignmentView}.
 */
public record UserAdministrationOrganizationAssignmentView(
        Long id,
        Long userId,
        Long organizationalUnitId,
        String organizationalUnitName,
        String organizationalUnitPath,
        OrganizationAssignmentType assignmentType,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt
) {
}