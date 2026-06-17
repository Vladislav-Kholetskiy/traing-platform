package com.vladislav.training.platform.testing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "user_answer",
    indexes = {
        @Index(name = "ix_usr_answer__test_att_id", columnList = "test_attempt_id"),
        @Index(name = "ix_usr_answer__question_id", columnList = "question_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_usr_answer__test_att_id__question_id",
            columnNames = {"test_attempt_id", "question_id"}
        )
    }
)
/**
 * JPA-сущность {@code UserAnswerEntity}.
 */
public class UserAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_attempt_id", nullable = false)
    private Long testAttemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAnswerEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestAttemptId() {
        return testAttemptId;
    }

    public void setTestAttemptId(Long testAttemptId) {
        this.testAttemptId = testAttemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
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
