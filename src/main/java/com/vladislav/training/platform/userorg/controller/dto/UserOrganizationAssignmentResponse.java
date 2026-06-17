package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import java.time.Instant;

/**
 * Ответ {@code UserOrganizationAssignmentResponse}.
 */
public record UserOrganizationAssignmentResponse(
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
