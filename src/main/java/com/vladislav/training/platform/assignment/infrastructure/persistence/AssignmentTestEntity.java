package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "assignment_test",
    indexes = {
        @Index(name = "ix_asg_test__asg_id", columnList = "assignment_id"),
        @Index(name = "ix_asg_test__test_id", columnList = "test_id"),
        @Index(name = "ix_asg_test__cnt_result_id", columnList = "counted_result_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_asg_test__asg_id__test_id",
            columnNames = {"assignment_id", "test_id"}
        )
    }
)
public class AssignmentTestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_test_role", nullable = false)
    private AssignmentTestRole assignmentTestRole;

    @Column(name = "counted_result_id")
    private Long countedResultId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AssignmentTestEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public AssignmentTestRole getAssignmentTestRole() {
        return assignmentTestRole;
    }

    public void setAssignmentTestRole(AssignmentTestRole assignmentTestRole) {
        this.assignmentTestRole = assignmentTestRole;
    }

    public Long getCountedResultId() {
        return countedResultId;
    }

    public void setCountedResultId(Long countedResultId) {
        this.countedResultId = countedResultId;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
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
