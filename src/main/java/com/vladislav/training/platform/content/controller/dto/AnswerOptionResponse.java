package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import java.time.Instant;

public record AnswerOptionResponse(Long id, Long questionId, String body, AnswerOptionRole answerOptionRole,
                                   Boolean isCorrect, int displayOrder, String pairingKey,
                                   Integer canonicalOrderPosition, Instant createdAt, Instant updatedAt) {}
