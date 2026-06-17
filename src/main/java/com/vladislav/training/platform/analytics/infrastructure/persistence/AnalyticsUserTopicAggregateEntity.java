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
@Table(name = "analytics_user_topic_aggregate")
public class AnalyticsUserTopicAggregateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "last_assigned_final_result_id")
    private Long lastAssignedFinalResultId;

    @Column(name = "last_assigned_final_completed_at")
    private Instant lastAssignedFinalCompletedAt;

    @Column(name = "last_assigned_final_score_percent", precision = 7, scale = 4)
    private BigDecimal lastAssignedFinalScorePercent;

    @Column(name = "last_assigned_final_passed")
    private Boolean lastAssignedFinalPassed;

    @Column(name = "average_score_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal averageScorePercent;

    @Column(name = "pass_rate_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal passRatePercent;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    protected AnalyticsUserTopicAggregateEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
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

    public Long getLastAssignedFinalResultId() {
        return lastAssignedFinalResultId;
    }

    public void setLastAssignedFinalResultId(Long lastAssignedFinalResultId) {
        this.lastAssignedFinalResultId = lastAssignedFinalResultId;
    }

    public Instant getLastAssignedFinalCompletedAt() {
        return lastAssignedFinalCompletedAt;
    }

    public void setLastAssignedFinalCompletedAt(Instant lastAssignedFinalCompletedAt) {
        this.lastAssignedFinalCompletedAt = lastAssignedFinalCompletedAt;
    }

    public BigDecimal getLastAssignedFinalScorePercent() {
        return lastAssignedFinalScorePercent;
    }

    public void setLastAssignedFinalScorePercent(BigDecimal lastAssignedFinalScorePercent) {
        this.lastAssignedFinalScorePercent = lastAssignedFinalScorePercent;
    }

    public Boolean getLastAssignedFinalPassed() {
        return lastAssignedFinalPassed;
    }

    public void setLastAssignedFinalPassed(Boolean lastAssignedFinalPassed) {
        this.lastAssignedFinalPassed = lastAssignedFinalPassed;
    }

    public BigDecimal getAverageScorePercent() {
        return averageScorePercent;
    }

    public void setAverageScorePercent(BigDecimal averageScorePercent) {
        this.averageScorePercent = averageScorePercent;
    }

    public BigDecimal getPassRatePercent() {
        return passRatePercent;
    }

    public void setPassRatePercent(BigDecimal passRatePercent) {
        this.passRatePercent = passRatePercent;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
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
