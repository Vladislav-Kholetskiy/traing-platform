package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.TestType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SaveTestRequest(@NotNull Long topicId, @NotBlank String name, String description,
                              @NotNull TestType testType,
                              @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal thresholdPercent,
                              @NotBlank String scoringPolicyCode,
                              @PositiveOrZero int sortOrder) {}
