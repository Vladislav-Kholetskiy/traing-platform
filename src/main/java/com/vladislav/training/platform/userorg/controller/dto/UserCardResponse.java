package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;
import java.util.List;

/**
 * Ответ {@code UserCardResponse}.
 */
public record UserCardResponse(
    Long id,
    String employeeNumber,
    String externalId,
    String lastName,
    String firstName,
    String middleName,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt,
    List<UserRoleAssignmentResponse> activeRoleAssignments,
    List<UserOrganizationAssignmentResponse> activeOrganizationAssignments
) {
}
