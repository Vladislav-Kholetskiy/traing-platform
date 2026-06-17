package com.vladislav.training.platform.result.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Проверка {@code ResultRecordingChildSnapshotCompletenessValidator}.
 */
@Component
@ConditionalOnBean({
    ResultQuestionSnapshotRepository.class,
    ResultAnswerOptionSnapshotRepository.class,
    ResultRecordingSubordinateSnapshotMaterializer.class
})
class ResultRecordingChildSnapshotCompletenessValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResultQuestionSnapshotRepository resultQuestionSnapshotRepository;
    private final ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository;
    private final ResultRecordingSubordinateSnapshotMaterializer subordinateSnapshotMaterializer;

    ResultRecordingChildSnapshotCompletenessValidator(
        ResultQuestionSnapshotRepository resultQuestionSnapshotRepository,
        ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository,
        ResultRecordingSubordinateSnapshotMaterializer subordinateSnapshotMaterializer
    ) {
        this.resultQuestionSnapshotRepository = Objects.requireNonNull(
            resultQuestionSnapshotRepository,
            "resultQuestionSnapshotRepository must not be null"
        );
        this.resultAnswerOptionSnapshotRepository = Objects.requireNonNull(
            resultAnswerOptionSnapshotRepository,
            "resultAnswerOptionSnapshotRepository must not be null"
        );
        this.subordinateSnapshotMaterializer = Objects.requireNonNull(
            subordinateSnapshotMaterializer,
            "subordinateSnapshotMaterializer must not be null"
        );
    }

    void requireCompletePersistedChildAggregate(Result existingResult, ResultSnapshotFacts snapshotFacts) {
        Objects.requireNonNull(existingResult, "existingResult must not be null");
        Objects.requireNonNull(snapshotFacts, "snapshotFacts must not be null");
        if (existingResult.id() == null) {
            throw incompleteAggregateConflict("persisted result id is required", existingResult, null);
        }

        ResultRecordingSubordinateSnapshotMaterializer.ExpectedSubordinateSnapshotAggregate expectedAggregate =
            subordinateSnapshotMaterializer.buildExpectedAggregate(existingResult, snapshotFacts);
        List<ResultQuestionSnapshot> persistedQuestionSnapshots = resultQuestionSnapshotRepository
            .findResultQuestionSnapshotsByResultId(existingResult.id());
        if (persistedQuestionSnapshots.size() != expectedAggregate.questionSnapshotAggregates().size()) {
            throw incompleteAggregateConflict(
                "question snapshot count mismatch: expected="
                    + expectedAggregate.questionSnapshotAggregates().size()
                    + ", actual="
                    + persistedQuestionSnapshots.size(),
                existingResult,
                null
            );
        }
        Map<Long, ResultQuestionSnapshot> persistedQuestionsByOriginalId = uniqueQuestionSnapshotsByOriginalId(
            persistedQuestionSnapshots,
            existingResult
        );
        for (ResultRecordingSubordinateSnapshotMaterializer.ExpectedQuestionSnapshotAggregate expectedQuestionAggregate
            : expectedAggregate.questionSnapshotAggregates()) {
            ResultQuestionSnapshot expectedQuestionSnapshot = expectedQuestionAggregate.questionSnapshot();
            ResultQuestionSnapshot persistedQuestionSnapshot = persistedQuestionsByOriginalId
                .get(expectedQuestionSnapshot.questionOriginalId());
            if (persistedQuestionSnapshot == null) {
                throw incompleteAggregateConflict(
                    "missing result_question_snapshot for questionOriginalId="
                        + expectedQuestionSnapshot.questionOriginalId(),
                    existingResult,
                    expectedQuestionSnapshot.questionOriginalId()
                );
            }
            requireMatchingQuestionSnapshot(existingResult, expectedQuestionSnapshot, persistedQuestionSnapshot);

            List<ResultAnswerOptionSnapshot> persistedAnswerOptionSnapshots =
                resultAnswerOptionSnapshotRepository.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(
                    persistedQuestionSnapshot.id()
                );
            if (persistedAnswerOptionSnapshots.size() != expectedQuestionAggregate.answerOptionSnapshots().size()) {
                throw incompleteAggregateConflict(
                    "answer option snapshot count mismatch for questionOriginalId="
                        + expectedQuestionSnapshot.questionOriginalId()
                        + ": expected="
                        + expectedQuestionAggregate.answerOptionSnapshots().size()
                        + ", actual="
                        + persistedAnswerOptionSnapshots.size(),
                    existingResult,
                    expectedQuestionSnapshot.questionOriginalId()
                );
            }
            Map<Long, ResultAnswerOptionSnapshot> persistedOptionsByOriginalId = uniqueAnswerOptionSnapshotsByOriginalId(
                persistedAnswerOptionSnapshots,
                existingResult,
                expectedQuestionSnapshot.questionOriginalId()
            );
            for (ResultAnswerOptionSnapshot expectedAnswerOptionSnapshot : expectedQuestionAggregate.answerOptionSnapshots()) {
                ResultAnswerOptionSnapshot persistedAnswerOptionSnapshot = persistedOptionsByOriginalId.get(
                    expectedAnswerOptionSnapshot.answerOptionOriginalId()
                );
                if (persistedAnswerOptionSnapshot == null) {
                    throw incompleteAggregateConflict(
                        "missing result_answer_option_snapshot for answerOptionOriginalId="
                            + expectedAnswerOptionSnapshot.answerOptionOriginalId(),
                        existingResult,
                        expectedQuestionSnapshot.questionOriginalId()
                    );
                }
                requireMatchingAnswerOptionSnapshot(
                    existingResult,
                    persistedQuestionSnapshot,
                    expectedQuestionSnapshot.questionOriginalId(),
                    expectedAnswerOptionSnapshot,
                    persistedAnswerOptionSnapshot
                );
            }
        }
    }

    private Map<Long, ResultQuestionSnapshot> uniqueQuestionSnapshotsByOriginalId(
        List<ResultQuestionSnapshot> persistedQuestionSnapshots,
        Result existingResult
    ) {
        Map<Long, ResultQuestionSnapshot> questionSnapshotsByOriginalId = new LinkedHashMap<>();
        for (ResultQuestionSnapshot persistedQuestionSnapshot : persistedQuestionSnapshots) {
            if (!Objects.equals(persistedQuestionSnapshot.resultId(), existingResult.id())) {
                throw incompleteAggregateConflict(
                    "question snapshot belongs to different resultId=" + persistedQuestionSnapshot.resultId(),
                    existingResult,
                    persistedQuestionSnapshot.questionOriginalId()
                );
            }
            ResultQuestionSnapshot previous = questionSnapshotsByOriginalId.putIfAbsent(
                persistedQuestionSnapshot.questionOriginalId(),
                persistedQuestionSnapshot
            );
            if (previous != null) {
                throw incompleteAggregateConflict(
                    "duplicate persisted question snapshot for questionOriginalId="
                        + persistedQuestionSnapshot.questionOriginalId(),
                    existingResult,
                    persistedQuestionSnapshot.questionOriginalId()
                );
            }
        }
        return questionSnapshotsByOriginalId;
    }

    private Map<Long, ResultAnswerOptionSnapshot> uniqueAnswerOptionSnapshotsByOriginalId(
        List<ResultAnswerOptionSnapshot> persistedAnswerOptionSnapshots,
        Result existingResult,
        Long questionOriginalId
    ) {
        Map<Long, ResultAnswerOptionSnapshot> answerOptionSnapshotsByOriginalId = new LinkedHashMap<>();
        for (ResultAnswerOptionSnapshot persistedAnswerOptionSnapshot : persistedAnswerOptionSnapshots) {
            ResultAnswerOptionSnapshot previous = answerOptionSnapshotsByOriginalId.putIfAbsent(
                persistedAnswerOptionSnapshot.answerOptionOriginalId(),
                persistedAnswerOptionSnapshot
            );
            if (previous != null) {
                throw incompleteAggregateConflict(
                    "duplicate persisted answer option snapshot for answerOptionOriginalId="
                        + persistedAnswerOptionSnapshot.answerOptionOriginalId(),
                    existingResult,
                    questionOriginalId
                );
            }
        }
        return answerOptionSnapshotsByOriginalId;
    }

    private void requireMatchingQuestionSnapshot(
        Result existingResult,
        ResultQuestionSnapshot expectedQuestionSnapshot,
        ResultQuestionSnapshot persistedQuestionSnapshot
    ) {
        boolean matches = Objects.equals(persistedQuestionSnapshot.resultId(), existingResult.id())
            && Objects.equals(persistedQuestionSnapshot.questionOriginalId(), expectedQuestionSnapshot.questionOriginalId())
            && Objects.equals(persistedQuestionSnapshot.body(), expectedQuestionSnapshot.body())
            && persistedQuestionSnapshot.questionType() == expectedQuestionSnapshot.questionType()
            && persistedQuestionSnapshot.displayOrder() == expectedQuestionSnapshot.displayOrder()
            && sameDecimal(persistedQuestionSnapshot.weight(), expectedQuestionSnapshot.weight())
            && sameJson(
                persistedQuestionSnapshot.correctAnswerSnapshot(),
                expectedQuestionSnapshot.correctAnswerSnapshot()
            )
            && sameJson(
                persistedQuestionSnapshot.userAnswerSnapshot(),
                expectedQuestionSnapshot.userAnswerSnapshot()
            )
            && sameDecimal(persistedQuestionSnapshot.earnedScore(), expectedQuestionSnapshot.earnedScore())
            && sameDecimal(persistedQuestionSnapshot.maxScore(), expectedQuestionSnapshot.maxScore())
            && persistedQuestionSnapshot.isCorrect() == expectedQuestionSnapshot.isCorrect()
            && Objects.equals(persistedQuestionSnapshot.evaluationNote(), expectedQuestionSnapshot.evaluationNote());
        if (!matches) {
            throw incompleteAggregateConflict(
                "persisted question snapshot does not match frozen expected facts for questionOriginalId="
                    + expectedQuestionSnapshot.questionOriginalId(),
                existingResult,
                expectedQuestionSnapshot.questionOriginalId()
            );
        }
    }

    private void requireMatchingAnswerOptionSnapshot(
        Result existingResult,
        ResultQuestionSnapshot persistedQuestionSnapshot,
        Long questionOriginalId,
        ResultAnswerOptionSnapshot expectedAnswerOptionSnapshot,
        ResultAnswerOptionSnapshot persistedAnswerOptionSnapshot
    ) {
        boolean matches = Objects.equals(
            persistedAnswerOptionSnapshot.resultQuestionSnapshotId(),
            persistedQuestionSnapshot.id()
        )
            && Objects.equals(
                persistedAnswerOptionSnapshot.answerOptionOriginalId(),
                expectedAnswerOptionSnapshot.answerOptionOriginalId()
            )
            && Objects.equals(persistedAnswerOptionSnapshot.body(), expectedAnswerOptionSnapshot.body())
            && persistedAnswerOptionSnapshot.displayOrder() == expectedAnswerOptionSnapshot.displayOrder()
            && persistedAnswerOptionSnapshot.isCorrectAtSnapshot() == expectedAnswerOptionSnapshot.isCorrectAtSnapshot()
            && persistedAnswerOptionSnapshot.isSelectedByUser() == expectedAnswerOptionSnapshot.isSelectedByUser();
        if (!matches) {
            throw incompleteAggregateConflict(
                "persisted answer option snapshot does not match frozen expected facts for answerOptionOriginalId="
                    + expectedAnswerOptionSnapshot.answerOptionOriginalId(),
                existingResult,
                questionOriginalId
            );
        }
    }

    private boolean sameDecimal(BigDecimal persisted, BigDecimal expected) {
        if (persisted == null || expected == null) {
            return Objects.equals(persisted, expected);
        }
        return persisted.compareTo(expected) == 0;
    }

    private boolean sameJson(String persisted, String expected) {
        if (persisted == null || expected == null) {
            return Objects.equals(persisted, expected);
        }
        try {
            return OBJECT_MAPPER.readTree(persisted).equals(OBJECT_MAPPER.readTree(expected));
        } catch (JsonProcessingException exception) {
            return Objects.equals(persisted, expected);
        }
    }

    private ConflictException incompleteAggregateConflict(String detail, Result existingResult, Long questionOriginalId) {
        String questionSuffix = questionOriginalId == null ? "" : ", questionOriginalId=" + questionOriginalId;
        return new ConflictException(
            "Result replay requires complete child snapshot aggregate: resultId="
                + existingResult.id()
                + ", testAttemptId="
                + existingResult.testAttemptId()
                + questionSuffix
                + ", detail="
                + detail
        );
    }
}
