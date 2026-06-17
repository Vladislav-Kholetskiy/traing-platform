package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;

/**
 * Команда {@code CreateAnswerOptionCommand}.
 */
public record CreateAnswerOptionCommand(
    String body,
    AnswerOptionRole answerOptionRole,
    Boolean isCorrect,
    int displayOrder,
    String pairingKey,
    Integer canonicalOrderPosition
) {}
