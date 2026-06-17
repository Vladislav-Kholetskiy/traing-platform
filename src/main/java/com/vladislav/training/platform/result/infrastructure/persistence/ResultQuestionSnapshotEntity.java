package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.result.domain.ResultQuestionType;
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
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "result_question_snapshot",
    indexes = {
        @Index(name = "ix_result_q_snap__result_id", columnList = "result_id"),
        @Index(name = "ix_result_q_snap__q_orig_id", columnList = "question_original_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_result_q_snap__result_id__q_orig_id",
            columnNames = {"result_id", "question_original_id"}
        ),
        @UniqueConstraint(
            name = "uq_result_q_snap__result_id__display_order",
            columnNames = {"result_id", "display_order"}
        )
    }
)
/**
 * JPA-сущность {@code ResultQuestionSnapshotEntity}.
 */
public class ResultQuestionSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "question_original_id")
    private Long questionOriginalId;

    @Column(name = "topic_id_snapshot")
    private Long topicIdSnapshot;

    @Column(name = "body", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private ResultQuestionType questionType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "weight", nullable = false)
    private BigDecimal weight;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer_snapshot", nullable = false, columnDefinition = "jsonb")
    private String correctAnswerSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_answer_snapshot", nullable = false, columnDefinition = "jsonb")
    private String userAnswerSnapshot;

    @Column(name = "earned_score", nullable = false)
    private BigDecimal earnedScore;

    @Column(name = "max_score", nullable = false)
    private BigDecimal maxScore;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "evaluation_note")
    private String evaluationNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ResultQuestionSnapshotEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public Long getQuestionOriginalId() {
        return questionOriginalId;
    }

    public void setQuestionOriginalId(Long questionOriginalId) {
        this.questionOriginalId = questionOriginalId;
    }

    public String getBody() {
        return body;
    }

    public Long getTopicIdSnapshot() {
        return topicIdSnapshot;
    }

    public void setTopicIdSnapshot(Long topicIdSnapshot) {
        this.topicIdSnapshot = topicIdSnapshot;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ResultQuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(ResultQuestionType questionType) {
        this.questionType = questionType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getCorrectAnswerSnapshot() {
        return correctAnswerSnapshot;
    }

    public void setCorrectAnswerSnapshot(String correctAnswerSnapshot) {
        this.correctAnswerSnapshot = correctAnswerSnapshot;
    }

    public String getUserAnswerSnapshot() {
        return userAnswerSnapshot;
    }

    public void setUserAnswerSnapshot(String userAnswerSnapshot) {
        this.userAnswerSnapshot = userAnswerSnapshot;
    }

    public BigDecimal getEarnedScore() {
        return earnedScore;
    }

    public void setEarnedScore(BigDecimal earnedScore) {
        this.earnedScore = earnedScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getEvaluationNote() {
        return evaluationNote;
    }

    public void setEvaluationNote(String evaluationNote) {
        this.evaluationNote = evaluationNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
