package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code SelfHistoricalResultReviewController}.
 */

@RestController
@RequestMapping("/api/v1/self/result-review")
class SelfHistoricalResultReviewController {

    private final SelfHistoricalResultReviewQueryService selfHistoricalResultReviewQueryService;
    private final InteractiveActorResolver interactiveActorResolver;

    SelfHistoricalResultReviewController(
        SelfHistoricalResultReviewQueryService selfHistoricalResultReviewQueryService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.selfHistoricalResultReviewQueryService = Objects.requireNonNull(
            selfHistoricalResultReviewQueryService,
            "selfHistoricalResultReviewQueryService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @GetMapping("/{resultId}")
    SelfHistoricalResultReviewDto findSelfHistoricalResultReview(@PathVariable Long resultId) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        SelfHistoricalResultReviewQueryService.SelfHistoricalResultReviewReadModel readModel =
            selfHistoricalResultReviewQueryService.findSelfHistoricalResultReview(
                new SelfHistoricalResultReviewQueryService.SelfHistoricalResultReviewQuery(actorUserId, resultId)
            );
        return toDto(readModel);
    }

    private SelfHistoricalResultReviewDto toDto(
        SelfHistoricalResultReviewQueryService.SelfHistoricalResultReviewReadModel readModel
    ) {
        return new SelfHistoricalResultReviewDto(
            readModel.resultId(),
            readModel.recordedAt(),
            readModel.testAttemptId(),
            readModel.testId(),
            readModel.testName(),
            readModel.scorePercent(),
            readModel.score(),
            readModel.passed(),
            readModel.attemptMode(),
            readModel.assignmentId(),
            readModel.questions().stream()
                .map(question -> new SelfHistoricalResultReviewDto.SelfHistoricalResultReviewQuestionDto(
                    question.resultQuestionSnapshotId(),
                    question.questionOriginalId(),
                    question.body(),
                    question.questionType(),
                    question.displayOrder(),
                    question.earnedScore(),
                    question.maxScore(),
                    question.correct(),
                    question.evaluationNote(),
                    question.correctAnswerSnapshot(),
                    question.userAnswerSnapshot(),
                    question.answerOptions().stream()
                        .map(option -> new SelfHistoricalResultReviewDto.SelfHistoricalResultReviewOptionDto(
                            option.resultAnswerOptionSnapshotId(),
                            option.answerOptionOriginalId(),
                            option.body(),
                            option.displayOrder(),
                            option.correctAtSnapshot(),
                            option.selectedByUser()
                        ))
                        .toList()
                ))
                .toList()
        );
    }
}
