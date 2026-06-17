package com.vladislav.training.platform.result.domain;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.QuestionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
/**
 * Запись данных {@code ResultSnapshotFacts}.
 */
public record ResultSnapshotFacts(
    Long assignmentId,
    Long assignmentTestId,
    Long testIdSnapshot,
    String testNameSnapshot,
    ResultScoringSnapshot scoringSnapshot,
    Boolean withinDeadline,
    Boolean countedInAssignment,
    ResultOrgContextSnapshot orgContextSnapshot,
    boolean snapshotFinalTopicControlFlag,
    Instant recordedAt,
    ResultSubordinateSnapshotFacts subordinateSnapshotFacts
) {

    public ResultSnapshotFacts(
        Long assignmentId,
        Long assignmentTestId,
        Long testIdSnapshot,
        String testNameSnapshot,
        ResultScoringSnapshot scoringSnapshot,
        Boolean withinDeadline,
        Boolean countedInAssignment,
        ResultOrgContextSnapshot orgContextSnapshot,
        boolean snapshotFinalTopicControlFlag,
        Instant recordedAt
    ) {
        this(
            assignmentId,
            assignmentTestId,
            testIdSnapshot,
            testNameSnapshot,
            scoringSnapshot,
            withinDeadline,
            countedInAssignment,
            orgContextSnapshot,
            snapshotFinalTopicControlFlag,
            recordedAt,
            ResultSubordinateSnapshotFacts.empty()
        );
    }

    public ResultSnapshotFacts {
        Objects.requireNonNull(testIdSnapshot, "testIdSnapshot must not be null");
        Objects.requireNonNull(testNameSnapshot, "testNameSnapshot must not be null");
        if (testNameSnapshot.isBlank()) {
            throw new IllegalArgumentException("testNameSnapshot must not be blank");
        }
        Objects.requireNonNull(scoringSnapshot, "scoringSnapshot must not be null");
        Objects.requireNonNull(orgContextSnapshot, "orgContextSnapshot must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        Objects.requireNonNull(subordinateSnapshotFacts, "subordinateSnapshotFacts must not be null");
    }

    public record ResultSubordinateSnapshotFacts(
        List<ResultQuestionSnapshotFact> questionSnapshotFacts
    ) {

        public ResultSubordinateSnapshotFacts {
            questionSnapshotFacts = List.copyOf(Objects.requireNonNull(
                questionSnapshotFacts,
                "questionSnapshotFacts must not be null"
            ));
        }

        public static ResultSubordinateSnapshotFacts empty() {
            return new ResultSubordinateSnapshotFacts(List.of());
        }
    }

    public record ResultQuestionSnapshotFact(
        Long questionOriginalId,
        Long topicIdSnapshot,
        String body,
        QuestionType questionType,
        int displayOrder,
        BigDecimal weight,
        List<ResultAnswerOptionSnapshotFact> answerOptionSnapshotFacts,
        List<ResultUserAnswerItemSnapshotFact> userAnswerItemSnapshotFacts
    ) {

        public ResultQuestionSnapshotFact(
            Long questionOriginalId,
            String body,
            QuestionType questionType,
            int displayOrder,
            BigDecimal weight,
            List<ResultAnswerOptionSnapshotFact> answerOptionSnapshotFacts,
            List<ResultUserAnswerItemSnapshotFact> userAnswerItemSnapshotFacts
        ) {
            this(
                questionOriginalId,
                null,
                body,
                questionType,
                displayOrder,
                weight,
                answerOptionSnapshotFacts,
                userAnswerItemSnapshotFacts
            );
        }

        public ResultQuestionSnapshotFact {
            Objects.requireNonNull(questionOriginalId, "questionOriginalId must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(questionType, "questionType must not be null");
            Objects.requireNonNull(weight, "weight must not be null");
            answerOptionSnapshotFacts = List.copyOf(Objects.requireNonNull(
                answerOptionSnapshotFacts,
                "answerOptionSnapshotFacts must not be null"
            ));
            userAnswerItemSnapshotFacts = List.copyOf(Objects.requireNonNull(
                userAnswerItemSnapshotFacts,
                "userAnswerItemSnapshotFacts must not be null"
            ));
        }
    }

    public record ResultAnswerOptionSnapshotFact(
        Long answerOptionOriginalId,
        String body,
        AnswerOptionRole answerOptionRole,
        Boolean correctAtSource,
        int displayOrder,
        String pairingKey,
        Integer canonicalOrderPosition
    ) {

        public ResultAnswerOptionSnapshotFact {
            Objects.requireNonNull(answerOptionOriginalId, "answerOptionOriginalId must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(answerOptionRole, "answerOptionRole must not be null");
        }
    }

    public record ResultUserAnswerItemSnapshotFact(
        Long answerOptionId,
        Long leftAnswerOptionId,
        Long rightAnswerOptionId,
        Integer userOrderPosition
    ) {
    }
}
