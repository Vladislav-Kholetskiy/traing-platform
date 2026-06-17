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
@Table(name = "analytics_campaign_aggregate")
public class AnalyticsCampaignAggregateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "recipient_snapshot_count", nullable = false)
    private Integer recipientSnapshotCount;

    @Column(name = "non_cancelled_assignments_from_campaign_snapshot", nullable = false)
    private Integer nonCancelledAssignmentsFromCampaignSnapshot;

    @Column(name = "completed_assignments", nullable = false)
    private Integer completedAssignments;

    @Column(name = "overdue_assignments", nullable = false)
    private Integer overdueAssignments;

    @Column(name = "non_cancelled_active_pool", nullable = false)
    private Integer nonCancelledActivePool;

    @Column(name = "cancelled_assignments", nullable = false)
    private Integer cancelledAssignments;

    @Column(name = "coverage_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal coveragePercent;

    @Column(name = "overdue_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal overduePercent;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    protected AnalyticsCampaignAggregateEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Integer getRecipientSnapshotCount() {
        return recipientSnapshotCount;
    }

    public void setRecipientSnapshotCount(Integer recipientSnapshotCount) {
        this.recipientSnapshotCount = recipientSnapshotCount;
    }

    public Integer getNonCancelledAssignmentsFromCampaignSnapshot() {
        return nonCancelledAssignmentsFromCampaignSnapshot;
    }

    public void setNonCancelledAssignmentsFromCampaignSnapshot(Integer nonCancelledAssignmentsFromCampaignSnapshot) {
        this.nonCancelledAssignmentsFromCampaignSnapshot = nonCancelledAssignmentsFromCampaignSnapshot;
    }

    public Integer getCompletedAssignments() {
        return completedAssignments;
    }

    public void setCompletedAssignments(Integer completedAssignments) {
        this.completedAssignments = completedAssignments;
    }

    public Integer getOverdueAssignments() {
        return overdueAssignments;
    }

    public void setOverdueAssignments(Integer overdueAssignments) {
        this.overdueAssignments = overdueAssignments;
    }

    public Integer getNonCancelledActivePool() {
        return nonCancelledActivePool;
    }

    public void setNonCancelledActivePool(Integer nonCancelledActivePool) {
        this.nonCancelledActivePool = nonCancelledActivePool;
    }

    public Integer getCancelledAssignments() {
        return cancelledAssignments;
    }

    public void setCancelledAssignments(Integer cancelledAssignments) {
        this.cancelledAssignments = cancelledAssignments;
    }

    public BigDecimal getCoveragePercent() {
        return coveragePercent;
    }

    public void setCoveragePercent(BigDecimal coveragePercent) {
        this.coveragePercent = coveragePercent;
    }

    public BigDecimal getOverduePercent() {
        return overduePercent;
    }

    public void setOverduePercent(BigDecimal overduePercent) {
        this.overduePercent = overduePercent;
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
