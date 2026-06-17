package com.vladislav.training.platform.integration.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.List;
/**
 * Запрос {@code ImportLaunchRequest}.
 */

@JsonIgnoreProperties(ignoreUnknown = false)
public record ImportLaunchRequest(
    @NotBlank String sourceType,
    String sourceRef,
    String payload,
    @NotEmpty List<@NotNull @Valid ImportLaunchItemRequest> items,
    @Null Long actorUserId,
    @Null Long initiatedByUserId,
    @Null Long userId
) {
}
