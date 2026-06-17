package com.vladislav.training.platform.analytics.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "analytics_question_aggregate")
public class AnalyticsQuestionAggregateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private Integer incorrectCount;

    @Column(name = "average_earned_score", nullable = false, precision = 12, scale = 4)
    private BigDecimal averageEarnedScore;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    protected AnalyticsQuestionAggregateEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(Integer incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public BigDecimal getAverageEarnedScore() {
        return averageEarnedScore;
    }

    public void setAverageEarnedScore(BigDecimal averageEarnedScore) {
        this.averageEarnedScore = averageEarnedScore;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Instant getRefreshedAt() {
        return refreshedAt;
    }

    public void setRefreshedAt(Instant refreshedAt) {
        this.refreshedAt = refreshedAt;
    }

    public Instant getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(Instant reconciledAt) {
        this.reconciledAt = reconciledAt;
    }
}
