package com.vladislav.training.platform.result.infrastructure.persistence;

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
    name = "result_answer_option_snapshot",
    indexes = {
        @Index(name = "ix_result_opt_snap__result_q_snap_id", columnList = "result_question_snapshot_id"),
        @Index(name = "ix_result_opt_snap__ans_opt_orig_id", columnList = "answer_option_original_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_result_opt_snap__rqs_id__display_order",
            columnNames = {"result_question_snapshot_id", "display_order"}
        )
    }
)
/**
 * JPA-сущность {@code ResultAnswerOptionSnapshotEntity}.
 */
public class ResultAnswerOptionSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_question_snapshot_id", nullable = false)
    private Long resultQuestionSnapshotId;

    @Column(name = "answer_option_original_id")
    private Long answerOptionOriginalId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_correct_at_snapshot", nullable = false)
    private boolean correctAtSnapshot;

    @Column(name = "is_selected_by_user", nullable = false)
    private boolean selectedByUser;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ResultAnswerOptionSnapshotEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResultQuestionSnapshotId() {
        return resultQuestionSnapshotId;
    }

    public void setResultQuestionSnapshotId(Long resultQuestionSnapshotId) {
        this.resultQuestionSnapshotId = resultQuestionSnapshotId;
    }

    public Long getAnswerOptionOriginalId() {
        return answerOptionOriginalId;
    }

    public void setAnswerOptionOriginalId(Long answerOptionOriginalId) {
        this.answerOptionOriginalId = answerOptionOriginalId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isCorrectAtSnapshot() {
        return correctAtSnapshot;
    }

    public void setCorrectAtSnapshot(boolean correctAtSnapshot) {
        this.correctAtSnapshot = correctAtSnapshot;
    }

    public boolean isSelectedByUser() {
        return selectedByUser;
    }

    public void setSelectedByUser(boolean selectedByUser) {
        this.selectedByUser = selectedByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
