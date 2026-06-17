package com.vladislav.training.platform.testing.infrastructure.persistence;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "test_attempt",
    indexes = {
        @Index(name = "ix_test_att__user_id", columnList = "user_id"),
        @Index(name = "ix_test_att__test_id", columnList = "test_id"),
        @Index(name = "ix_test_att__asg_test_id", columnList = "assignment_test_id"),
        @Index(name = "ix_test_att__status", columnList = "status"),
        @Index(name = "ix_test_att__last_activity_at", columnList = "last_activity_at")
    }
)
/**
 * JPA-сущность {@code TestAttemptEntity}.
 */
public class TestAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "assignment_test_id")
    private Long assignmentTestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_mode", nullable = false)
    private AttemptMode attemptMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TestAttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "abandoned_at")
    private Instant abandonedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TestAttemptEntity() {
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

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Long getAssignmentTestId() {
        return assignmentTestId;
    }

    public void setAssignmentTestId(Long assignmentTestId) {
        this.assignmentTestId = assignmentTestId;
    }

    public AttemptMode getAttemptMode() {
        return attemptMode;
    }

    public void setAttemptMode(AttemptMode attemptMode) {
        this.attemptMode = attemptMode;
    }

    public TestAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(TestAttemptStatus status) {
        this.status = status;
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

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Instant getAbandonedAt() {
        return abandonedAt;
    }

    public void setAbandonedAt(Instant abandonedAt) {
        this.abandonedAt = abandonedAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
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
