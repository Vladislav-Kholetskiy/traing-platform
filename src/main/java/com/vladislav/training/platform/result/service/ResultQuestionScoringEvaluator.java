package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import java.math.BigDecimal;
import java.util.List;

/**
 * Интерфейс {@code ResultQuestionScoringEvaluator}.
 */
interface ResultQuestionScoringEvaluator {

    QuestionScore evaluateQuestion(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore,
        String scoringPolicyCode
    );

    record QuestionScore(
        BigDecimal earnedScore,
        boolean correct
    ) {
    }
}
