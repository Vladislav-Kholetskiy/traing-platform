package com.vladislav.training.platform.integration.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
/**
 * Запрос {@code ImportLaunchItemRequest}.
 */

@JsonIgnoreProperties(ignoreUnknown = false)
public record ImportLaunchItemRequest(
    @NotBlank String targetEntityType,
    String externalId,
    String employeeNumber,
    @NotBlank String payload
) {
}
