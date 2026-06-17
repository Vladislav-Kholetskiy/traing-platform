package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
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

/**
 * JPA-сущность {@code AssignmentAdministrativeActionEntity}.
 */
@Entity
@Table(
    name = "assignment_administrative_action",
    indexes = {
        @Index(name = "ix_asg_admin_act__asg_id", columnList = "assignment_id"),
        @Index(name = "ix_asg_admin_act__occurred_at", columnList = "occurred_at")
    }
)
public class AssignmentAdministrativeActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private AssignmentAdministrativeActionType actionType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssignmentAdministrativeActionEntity() {
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

    public AssignmentAdministrativeActionType getActionType() {
        return actionType;
    }

    public void setActionType(AssignmentAdministrativeActionType actionType) {
        this.actionType = actionType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
