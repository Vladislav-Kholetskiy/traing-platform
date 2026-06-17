package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;

/**
 * Ответ {@code UserResponse}.
 */
public record UserResponse(
    Long id,
    String employeeNumber,
    String externalId,
    String lastName,
    String firstName,
    String middleName,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
