package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Запрос {@code CreateUserRequest}.
 */
public record CreateUserRequest(
    @NotBlank String employeeNumber,
    @Pattern(regexp = ".*\\S.*", message = "externalId must not be blank when provided") String externalId,
    @NotBlank String lastName,
    @NotBlank String firstName,
    String middleName,
    @NotNull UserStatus status
) {
}
