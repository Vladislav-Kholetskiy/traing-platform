package com.vladislav.training.platform.integration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
/**
 * JPA-сущность {@code ImportJobEntity}.
 */

@Entity
@Table(name = "import_job")
public class ImportJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "initiated_by_user_id")
    private Long initiatedByUserId;

    @Column(name = "status", nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_item_count")
    private Integer totalItemCount;

    @Column(name = "processed_item_count")
    private Integer processedItemCount;

    @Column(name = "applied_item_count")
    private Integer appliedItemCount;

    @Column(name = "failed_item_count")
    private Integer failedItemCount;

    @Column(name = "requires_review_item_count")
    private Integer requiresReviewItemCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ImportJobEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public Long getInitiatedByUserId() {
        return initiatedByUserId;
    }

    public void setInitiatedByUserId(Long initiatedByUserId) {
        this.initiatedByUserId = initiatedByUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(Integer totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    public Integer getProcessedItemCount() {
        return processedItemCount;
    }

    public void setProcessedItemCount(Integer processedItemCount) {
        this.processedItemCount = processedItemCount;
    }

    public Integer getAppliedItemCount() {
        return appliedItemCount;
    }

    public void setAppliedItemCount(Integer appliedItemCount) {
        this.appliedItemCount = appliedItemCount;
    }

    public Integer getFailedItemCount() {
        return failedItemCount;
    }

    public void setFailedItemCount(Integer failedItemCount) {
        this.failedItemCount = failedItemCount;
    }

    public Integer getRequiresReviewItemCount() {
        return requiresReviewItemCount;
    }

    public void setRequiresReviewItemCount(Integer requiresReviewItemCount) {
        this.requiresReviewItemCount = requiresReviewItemCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
