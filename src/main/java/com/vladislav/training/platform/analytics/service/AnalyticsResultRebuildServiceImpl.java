package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnalyticsResultRebuildServiceImpl implements AnalyticsRebuildService, AnalyticsResultRebuildService {

    private static final String NOT_IMPLEMENTED_MESSAGE =
        "SCN-11 result-based analytics materialization is not implemented yet.";

    private final AnalyticsQuestionAggregateResultSourceReader sourceReader;
    private final AnalyticsTopicKeyStrategy topicKeyStrategy;
    private final AnalyticsUserTopicAggregateWriter userTopicAggregateWriter;
    private final AnalyticsDepartmentTopicAggregateWriter departmentTopicAggregateWriter;
    private final AnalyticsQuestionAggregateWriter questionAggregateWriter;
    private final AnalyticsResultRebuildOutcomeReporter outcomeReporter;

    public AnalyticsResultRebuildServiceImpl() {
        this.sourceReader = null;
        this.topicKeyStrategy = null;
        this.userTopicAggregateWriter = null;
        this.departmentTopicAggregateWriter = null;
        this.questionAggregateWriter = null;
        this.outcomeReporter = null;
    }

    public AnalyticsResultRebuildServiceImpl(
        AnalyticsQuestionAggregateResultSourceReader sourceReader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter userTopicAggregateWriter
    ) {
        this.sourceReader = Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        this.topicKeyStrategy = Objects.requireNonNull(topicKeyStrategy, "topicKeyStrategy must not be null");
        this.userTopicAggregateWriter = Objects.requireNonNull(
            userTopicAggregateWriter,
            "userTopicAggregateWriter must not be null"
        );
        this.departmentTopicAggregateWriter = null;
        this.questionAggregateWriter = null;
        this.outcomeReporter = null;
    }

    public AnalyticsResultRebuildServiceImpl(
        AnalyticsQuestionAggregateResultSourceReader sourceReader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter userTopicAggregateWriter,
        AnalyticsDepartmentTopicAggregateWriter departmentTopicAggregateWriter
    ) {
        this.sourceReader = Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        this.topicKeyStrategy = Objects.requireNonNull(topicKeyStrategy, "topicKeyStrategy must not be null");
        this.userTopicAggregateWriter = Objects.requireNonNull(
            userTopicAggregateWriter,
            "userTopicAggregateWriter must not be null"
        );
        this.departmentTopicAggregateWriter = departmentTopicAggregateWriter;
        this.questionAggregateWriter = null;
        this.outcomeReporter = null;
    }

    public AnalyticsResultRebuildServiceImpl(
        AnalyticsQuestionAggregateResultSourceReader sourceReader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter userTopicAggregateWriter,
        AnalyticsDepartmentTopicAggregateWriter departmentTopicAggregateWriter,
        AnalyticsQuestionAggregateWriter questionAggregateWriter
    ) {
        this.sourceReader = Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        this.topicKeyStrategy = Objects.requireNonNull(topicKeyStrategy, "topicKeyStrategy must not be null");
        this.userTopicAggregateWriter = Objects.requireNonNull(
            userTopicAggregateWriter,
            "userTopicAggregateWriter must not be null"
        );
        this.departmentTopicAggregateWriter = departmentTopicAggregateWriter;
        this.questionAggregateWriter = Objects.requireNonNull(
            questionAggregateWriter,
            "questionAggregateWriter must not be null"
        );
        this.outcomeReporter = null;
    }

    public AnalyticsResultRebuildServiceImpl(
        AnalyticsQuestionAggregateResultSourceReader sourceReader,
        AnalyticsTopicKeyStrategy topicKeyStrategy,
        AnalyticsUserTopicAggregateWriter userTopicAggregateWriter,
        AnalyticsDepartmentTopicAggregateWriter departmentTopicAggregateWriter,
        AnalyticsQuestionAggregateWriter questionAggregateWriter,
        AnalyticsResultRebuildOutcomeReporter outcomeReporter
    ) {
        this.sourceReader = Objects.requireNonNull(sourceReader, "sourceReader must not be null");
        this.topicKeyStrategy = Objects.requireNonNull(topicKeyStrategy, "topicKeyStrategy must not be null");
        this.userTopicAggregateWriter = Objects.requireNonNull(
            userTopicAggregateWriter,
            "userTopicAggregateWriter must not be null"
        );
        this.departmentTopicAggregateWriter = departmentTopicAggregateWriter;
        this.questionAggregateWriter = Objects.requireNonNull(
            questionAggregateWriter,
            "questionAggregateWriter must not be null"
        );
        this.outcomeReporter = Objects.requireNonNull(outcomeReporter, "outcomeReporter must not be null");
    }

    @Override
    public void rebuildAllAnalytics() {
        AnalyticsResultSourceWindow window = requireAvailableResultSourceWindow();
        for (Instant bucketStart = window.periodStartInclusive();
            bucketStart.isBefore(window.periodEndExclusive());
            bucketStart = bucketStart.plus(1, ChronoUnit.DAYS)) {
            rebuildResultAnalytics(bucketStart, bucketStart.plus(1, ChronoUnit.DAYS));
        }
    }

    @Override
    public void reconcileAnalytics() {
        rebuildAllAnalytics();
    }

    public AnalyticsResultRebuildOutcome rebuildResultAnalytics(Instant periodStartInclusive, Instant periodEndExclusive) {
        Objects.requireNonNull(periodStartInclusive, "periodStartInclusive must not be null");
        Objects.requireNonNull(periodEndExclusive, "periodEndExclusive must not be null");
        if (!periodStartInclusive.isBefore(periodEndExclusive)) {
            throw new IllegalArgumentException("periodStartInclusive must be before periodEndExclusive");
        }
        if (sourceReader == null || topicKeyStrategy == null || userTopicAggregateWriter == null) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
        }

        List<AnalyticsQuestionAggregateResultSourceRow> sourceRows = sourceReader.readQuestionAggregateRows(
            periodStartInclusive,
            periodEndExclusive
        );
        Map<UserTopicAggregateKey, AggregateCounters> userTopicAggregateCountersByKey = new LinkedHashMap<>();
        Map<UserTopicAggregateKey, LatestAssignedFinalSnapshot> latestAssignedFinalSnapshotByUserTopicKey = new LinkedHashMap<>();
        Map<DepartmentTopicAggregateKey, AggregateCounters> departmentTopicAggregateCountersByKey = new LinkedHashMap<>();
        Map<QuestionAggregateKey, AggregateCounters> questionAggregateCountersByKey = new LinkedHashMap<>();
        Map<UserTopicAttemptKey, AttemptContribution> userTopicAttemptContributionsByKey = new LinkedHashMap<>();
        Map<DepartmentTopicAttemptKey, AttemptContribution> departmentTopicAttemptContributionsByKey = new LinkedHashMap<>();
        List<AnalyticsUnsupportedTopicKeyReportRow> unsupportedTopicRows = new ArrayList<>();
        long supportedTopicRowCount = 0L;
        long unsupportedTopicRowCount = 0L;

        for (AnalyticsQuestionAggregateResultSourceRow sourceRow : sourceRows) {
            QuestionAggregateKey questionAggregateKey = new QuestionAggregateKey(
                sourceRow.questionOriginalId()
            );
            AggregateCounters questionAggregateCounters = questionAggregateCountersByKey.computeIfAbsent(
                questionAggregateKey,
                ignored -> new AggregateCounters()
            );
            questionAggregateCounters.attemptCount++;
            questionAggregateCounters.earnedScoreSum = questionAggregateCounters.earnedScoreSum.add(
                sourceRow.earnedScore() == null ? BigDecimal.ZERO : sourceRow.earnedScore()
            );
            if (Boolean.TRUE.equals(sourceRow.answeredCorrectly())) {
                questionAggregateCounters.correctCount++;
            }

            AnalyticsTopicKeyResolution resolution = topicKeyStrategy.resolveTopicKey(sourceRow);
            if (!resolution.supported() || resolution.topicId() == null) {
                unsupportedTopicRowCount++;
                unsupportedTopicRows.add(
                    new AnalyticsUnsupportedTopicKeyReportRow(
                        sourceRow.resultId(),
                        sourceRow.questionOriginalId(),
                        resolution.reason()
                    )
                );
                continue;
            }
            supportedTopicRowCount++;
            if (!isEligibleForStandardTopicAggregates(sourceRow.attemptModeSnapshot())) {
                continue;
            }

            UserTopicAggregateKey userTopicAggregateKey = new UserTopicAggregateKey(
                sourceRow.userIdSnapshot(),
                resolution.topicId()
            );
            AttemptContribution userTopicAttemptContribution = userTopicAttemptContributionsByKey.computeIfAbsent(
                new UserTopicAttemptKey(sourceRow.resultId(), userTopicAggregateKey.userId, userTopicAggregateKey.topicId),
                ignored -> new AttemptContribution()
            );
            userTopicAttemptContribution.earnedScoreSum = userTopicAttemptContribution.earnedScoreSum.add(
                sourceRow.earnedScore() == null ? BigDecimal.ZERO : sourceRow.earnedScore()
            );
            userTopicAttemptContribution.maxScoreSum = userTopicAttemptContribution.maxScoreSum.add(
                sourceRow.maxScore() == null ? BigDecimal.ZERO : sourceRow.maxScore()
            );
            if (!Boolean.TRUE.equals(sourceRow.answeredCorrectly())) {
                userTopicAttemptContribution.hasFailure = true;
            }
            if (isAssignedFinalTopicControlCandidate(sourceRow)) {
                latestAssignedFinalSnapshotByUserTopicKey.merge(
                    userTopicAggregateKey,
                    LatestAssignedFinalSnapshot.from(sourceRow),
                    AnalyticsResultRebuildServiceImpl::selectLatestAssignedFinalSnapshot
                );
            }

            DepartmentTopicAggregateKey departmentTopicAggregateKey = new DepartmentTopicAggregateKey(
                sourceRow.organizationalUnitIdSnapshot(),
                resolution.topicId()
            );
            AttemptContribution departmentTopicAttemptContribution = departmentTopicAttemptContributionsByKey.computeIfAbsent(
                new DepartmentTopicAttemptKey(
                    sourceRow.resultId(),
                    departmentTopicAggregateKey.organizationalUnitIdSnapshot,
                    departmentTopicAggregateKey.topicId
                ),
                ignored -> new AttemptContribution()
            );
            if (departmentTopicAttemptContribution.organizationalPathSnapshot == null) {
                departmentTopicAttemptContribution.organizationalPathSnapshot = sourceRow.organizationalPathSnapshot();
            }
            departmentTopicAttemptContribution.earnedScoreSum = departmentTopicAttemptContribution.earnedScoreSum.add(
                sourceRow.earnedScore() == null ? BigDecimal.ZERO : sourceRow.earnedScore()
            );
            departmentTopicAttemptContribution.maxScoreSum = departmentTopicAttemptContribution.maxScoreSum.add(
                sourceRow.maxScore() == null ? BigDecimal.ZERO : sourceRow.maxScore()
            );
            if (!Boolean.TRUE.equals(sourceRow.answeredCorrectly())) {
                departmentTopicAttemptContribution.hasFailure = true;
            }
        }

        for (Map.Entry<UserTopicAttemptKey, AttemptContribution> entry : userTopicAttemptContributionsByKey.entrySet()) {
            UserTopicAttemptKey key = entry.getKey();
            AttemptContribution contribution = entry.getValue();
            AggregateCounters userTopicCounters = userTopicAggregateCountersByKey.computeIfAbsent(
                new UserTopicAggregateKey(key.userId, key.topicId),
                ignored -> new AggregateCounters()
            );
            userTopicCounters.attemptCount++;
            userTopicCounters.earnedScoreSum = userTopicCounters.earnedScoreSum.add(contribution.earnedScoreSum);
            userTopicCounters.maxScoreSum = userTopicCounters.maxScoreSum.add(contribution.maxScoreSum);
            if (contribution.hasFailure) {
                userTopicCounters.errorCount++;
            } else {
                userTopicCounters.correctCount++;
            }
        }

        for (Map.Entry<DepartmentTopicAttemptKey, AttemptContribution> entry : departmentTopicAttemptContributionsByKey.entrySet()) {
            DepartmentTopicAttemptKey key = entry.getKey();
            AttemptContribution contribution = entry.getValue();
            AggregateCounters departmentTopicCounters = departmentTopicAggregateCountersByKey.computeIfAbsent(
                new DepartmentTopicAggregateKey(key.organizationalUnitIdSnapshot, key.topicId),
                ignored -> new AggregateCounters()
            );
            if (departmentTopicCounters.organizationalPathSnapshot == null) {
                departmentTopicCounters.organizationalPathSnapshot = contribution.organizationalPathSnapshot;
            }
            departmentTopicCounters.attemptCount++;
            departmentTopicCounters.earnedScoreSum = departmentTopicCounters.earnedScoreSum.add(
                calculateScorePercent(contribution.earnedScoreSum, contribution.maxScoreSum)
            );
            if (contribution.hasFailure) {
                departmentTopicCounters.errorCount++;
            }
        }

        List<AnalyticsUserTopicAggregateRow> userTopicAggregateRows = new ArrayList<>();
        for (Map.Entry<UserTopicAggregateKey, AggregateCounters> entry : userTopicAggregateCountersByKey.entrySet()) {
            UserTopicAggregateKey key = entry.getKey();
            AggregateCounters counters = entry.getValue();
            BigDecimal passRatePercent = counters.attemptCount == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(counters.correctCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(counters.attemptCount), 4, RoundingMode.HALF_UP);
            BigDecimal averageScorePercent = counters.maxScoreSum.signum() == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : counters.earnedScoreSum
                    .multiply(BigDecimal.valueOf(100))
                    .divide(counters.maxScoreSum, 4, RoundingMode.HALF_UP);
            userTopicAggregateRows.add(
                new AnalyticsUserTopicAggregateRow(
                    key.userId,
                    key.topicId,
                    latestAssignedFinalSnapshotByUserTopicKey.get(key) == null
                        ? null
                        : latestAssignedFinalSnapshotByUserTopicKey.get(key).resultId,
                    latestAssignedFinalSnapshotByUserTopicKey.get(key) == null
                        ? null
                        : latestAssignedFinalSnapshotByUserTopicKey.get(key).completedAt,
                    latestAssignedFinalSnapshotByUserTopicKey.get(key) == null
                        ? null
                        : latestAssignedFinalSnapshotByUserTopicKey.get(key).scorePercent,
                    latestAssignedFinalSnapshotByUserTopicKey.get(key) == null
                        ? null
                        : latestAssignedFinalSnapshotByUserTopicKey.get(key).passed,
                    averageScorePercent,
                    passRatePercent,
                    counters.attemptCount,
                    counters.errorCount,
                    periodStartInclusive,
                    periodEndExclusive
                )
            );
        }

        userTopicAggregateWriter.replaceUserTopicAggregates(
            periodStartInclusive,
            periodEndExclusive,
            userTopicAggregateRows
        );

        List<AnalyticsDepartmentTopicAggregateRow> departmentTopicAggregateRows = List.of();
        if (departmentTopicAggregateWriter != null) {
            departmentTopicAggregateRows = new ArrayList<>();
            for (Map.Entry<DepartmentTopicAggregateKey, AggregateCounters> entry : departmentTopicAggregateCountersByKey.entrySet()) {
                DepartmentTopicAggregateKey key = entry.getKey();
                AggregateCounters counters = entry.getValue();
                BigDecimal averageScorePercent = counters.attemptCount == 0
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : counters.earnedScoreSum.divide(BigDecimal.valueOf(counters.attemptCount), 4, RoundingMode.HALF_UP);
                BigDecimal passRatePercent = counters.attemptCount == 0
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(counters.attemptCount - counters.errorCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(counters.attemptCount), 4, RoundingMode.HALF_UP);
                departmentTopicAggregateRows.add(
                    new AnalyticsDepartmentTopicAggregateRow(
                        key.organizationalUnitIdSnapshot,
                        counters.organizationalPathSnapshot,
                        key.topicId,
                        averageScorePercent,
                        passRatePercent,
                        counters.attemptCount,
                        counters.errorCount,
                        periodStartInclusive,
                        periodEndExclusive
                    )
                );
            }

            departmentTopicAggregateWriter.replaceDepartmentTopicAggregates(
                periodStartInclusive,
                periodEndExclusive,
                departmentTopicAggregateRows
            );
        }

        List<AnalyticsQuestionAggregateRow> questionAggregateRows = List.of();
        if (questionAggregateWriter != null) {
            questionAggregateRows = new ArrayList<>();
            for (Map.Entry<QuestionAggregateKey, AggregateCounters> entry : questionAggregateCountersByKey.entrySet()) {
                QuestionAggregateKey key = entry.getKey();
                AggregateCounters counters = entry.getValue();
                long incorrectCount = counters.attemptCount - counters.correctCount;
                BigDecimal averageEarnedScore = counters.attemptCount == 0
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : counters.earnedScoreSum.divide(BigDecimal.valueOf(counters.attemptCount), 4, RoundingMode.HALF_UP);
                questionAggregateRows.add(
                    new AnalyticsQuestionAggregateRow(
                        key.questionId,
                        counters.attemptCount,
                        counters.correctCount,
                        incorrectCount,
                        averageEarnedScore,
                        periodStartInclusive,
                        periodEndExclusive
                    )
                );
            }

            questionAggregateWriter.replaceQuestionAggregates(
                periodStartInclusive,
                periodEndExclusive,
                questionAggregateRows
            );
        }

        if (outcomeReporter != null) {
            outcomeReporter.reportResultRebuildOutcome(outcome(
                periodStartInclusive,
                periodEndExclusive,
                sourceRows.size(),
                supportedTopicRowCount,
                unsupportedTopicRowCount,
                userTopicAggregateRows.size(),
                departmentTopicAggregateRows.size(),
                questionAggregateRows.size(),
                unsupportedTopicRows
            ));
        }
        return outcome(
            periodStartInclusive,
            periodEndExclusive,
            sourceRows.size(),
            supportedTopicRowCount,
            unsupportedTopicRowCount,
            userTopicAggregateRows.size(),
            departmentTopicAggregateRows.size(),
            questionAggregateRows.size(),
            unsupportedTopicRows
        );
    }

    private AnalyticsResultRebuildOutcome outcome(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        long sourceRowCount,
        long supportedTopicRowCount,
        long unsupportedTopicRowCount,
        long userTopicAggregateRowCount,
        long departmentTopicAggregateRowCount,
        long questionAggregateRowCount,
        List<AnalyticsUnsupportedTopicKeyReportRow> unsupportedTopicRows
    ) {
        return new AnalyticsResultRebuildOutcome(
            periodStartInclusive,
            periodEndExclusive,
            sourceRowCount,
            supportedTopicRowCount,
            unsupportedTopicRowCount,
            userTopicAggregateRowCount,
            departmentTopicAggregateRowCount,
            questionAggregateRowCount,
            unsupportedTopicRows
        );
    }

    private AnalyticsResultSourceWindow requireAvailableResultSourceWindow() {
        if (sourceReader == null || topicKeyStrategy == null || userTopicAggregateWriter == null) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
        }
        return sourceReader.findAvailableResultSourceWindow()
            .orElseThrow(() -> new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE));
    }

    private static final class UserTopicAggregateKey {

        private final Long userId;
        private final Long topicId;

        private UserTopicAggregateKey(Long userId, Long topicId) {
            this.userId = userId;
            this.topicId = topicId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof UserTopicAggregateKey that)) {
                return false;
            }
            return Objects.equals(userId, that.userId)
                && Objects.equals(topicId, that.topicId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, topicId);
        }
    }

    private static final class AggregateCounters {

        private long attemptCount;
        private long correctCount;
        private long errorCount;
        private String organizationalPathSnapshot;
        private BigDecimal earnedScoreSum = BigDecimal.ZERO;
        private BigDecimal maxScoreSum = BigDecimal.ZERO;
    }

    private static final class AttemptContribution {

        private boolean hasFailure;
        private String organizationalPathSnapshot;
        private BigDecimal earnedScoreSum = BigDecimal.ZERO;
        private BigDecimal maxScoreSum = BigDecimal.ZERO;
    }

    private static final class DepartmentTopicAggregateKey {

        private final Long organizationalUnitIdSnapshot;
        private final Long topicId;

        private DepartmentTopicAggregateKey(Long organizationalUnitIdSnapshot, Long topicId) {
            this.organizationalUnitIdSnapshot = organizationalUnitIdSnapshot;
            this.topicId = topicId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DepartmentTopicAggregateKey that)) {
                return false;
            }
            return Objects.equals(organizationalUnitIdSnapshot, that.organizationalUnitIdSnapshot)
                && Objects.equals(topicId, that.topicId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(organizationalUnitIdSnapshot, topicId);
        }
    }

    private static BigDecimal calculateScorePercent(BigDecimal earnedScore, BigDecimal maxScore) {
        if (maxScore.signum() == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return earnedScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 4, RoundingMode.HALF_UP);
    }

    private static boolean isEligibleForStandardTopicAggregates(String attemptModeSnapshot) {
        return AttemptMode.ASSIGNED.name().equals(attemptModeSnapshot);
    }

    private static boolean isAssignedFinalTopicControlCandidate(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
        return isEligibleForStandardTopicAggregates(sourceRow.attemptModeSnapshot())
            && Boolean.TRUE.equals(sourceRow.finalTopicControlSnapshot());
    }

    private static LatestAssignedFinalSnapshot selectLatestAssignedFinalSnapshot(
        LatestAssignedFinalSnapshot current,
        LatestAssignedFinalSnapshot candidate
    ) {
        if (candidate.completedAt.isAfter(current.completedAt)) {
            return candidate;
        }
        if (candidate.completedAt.equals(current.completedAt) && candidate.resultId > current.resultId) {
            return candidate;
        }
        return current;
    }

    private static final class UserTopicAttemptKey {

        private final Long resultId;
        private final Long userId;
        private final Long topicId;

        private UserTopicAttemptKey(Long resultId, Long userId, Long topicId) {
            this.resultId = resultId;
            this.userId = userId;
            this.topicId = topicId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof UserTopicAttemptKey that)) {
                return false;
            }
            return Objects.equals(resultId, that.resultId)
                && Objects.equals(userId, that.userId)
                && Objects.equals(topicId, that.topicId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resultId, userId, topicId);
        }
    }

    private static final class DepartmentTopicAttemptKey {

        private final Long resultId;
        private final Long organizationalUnitIdSnapshot;
        private final Long topicId;

        private DepartmentTopicAttemptKey(Long resultId, Long organizationalUnitIdSnapshot, Long topicId) {
            this.resultId = resultId;
            this.organizationalUnitIdSnapshot = organizationalUnitIdSnapshot;
            this.topicId = topicId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DepartmentTopicAttemptKey that)) {
                return false;
            }
            return Objects.equals(resultId, that.resultId)
                && Objects.equals(organizationalUnitIdSnapshot, that.organizationalUnitIdSnapshot)
                && Objects.equals(topicId, that.topicId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resultId, organizationalUnitIdSnapshot, topicId);
        }
    }

    private static final class QuestionAggregateKey {

        private final Long questionId;

        private QuestionAggregateKey(Long questionId) {
            this.questionId = questionId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof QuestionAggregateKey that)) {
                return false;
            }
            return Objects.equals(questionId, that.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(questionId);
        }
    }

    private static final class LatestAssignedFinalSnapshot {

        private final Long resultId;
        private final Instant completedAt;
        private final BigDecimal scorePercent;
        private final Boolean passed;

        private LatestAssignedFinalSnapshot(
            Long resultId,
            Instant completedAt,
            BigDecimal scorePercent,
            Boolean passed
        ) {
            this.resultId = resultId;
            this.completedAt = completedAt;
            this.scorePercent = scorePercent;
            this.passed = passed;
        }

        private static LatestAssignedFinalSnapshot from(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
            return new LatestAssignedFinalSnapshot(
                sourceRow.resultId(),
                sourceRow.completedAt(),
                sourceRow.scorePercent(),
                sourceRow.passed()
            );
        }
    }
}
