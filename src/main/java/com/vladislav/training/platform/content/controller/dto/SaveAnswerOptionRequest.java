package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SaveAnswerOptionRequest(@NotBlank String body,
                                      @NotNull AnswerOptionRole answerOptionRole,
                                      Boolean isCorrect,
                                      @PositiveOrZero int displayOrder,
                                      String pairingKey,
                                      @PositiveOrZero Integer canonicalOrderPosition) {}
