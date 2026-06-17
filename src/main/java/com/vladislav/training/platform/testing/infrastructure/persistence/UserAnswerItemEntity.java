package com.vladislav.training.platform.testing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "user_answer_item",
    indexes = {
        @Index(name = "ix_usr_answer_item__usr_answer_id", columnList = "user_answer_id"),
        @Index(name = "ix_usr_answer_item__ans_opt_id", columnList = "answer_option_id"),
        @Index(name = "ix_usr_answer_item__left_ans_opt_id", columnList = "left_answer_option_id"),
        @Index(name = "ix_usr_answer_item__right_ans_opt_id", columnList = "right_answer_option_id")
    }
)
/**
 * JPA-сущность {@code UserAnswerItemEntity}.
 */
public class UserAnswerItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_answer_id", nullable = false)
    private Long userAnswerId;

    @Column(name = "answer_option_id")
    private Long answerOptionId;

    @Column(name = "left_answer_option_id")
    private Long leftAnswerOptionId;

    @Column(name = "right_answer_option_id")
    private Long rightAnswerOptionId;

    @Column(name = "user_order_position")
    private Integer userOrderPosition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAnswerItemEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserAnswerId() {
        return userAnswerId;
    }

    public void setUserAnswerId(Long userAnswerId) {
        this.userAnswerId = userAnswerId;
    }

    public Long getAnswerOptionId() {
        return answerOptionId;
    }

    public void setAnswerOptionId(Long answerOptionId) {
        this.answerOptionId = answerOptionId;
    }

    public Long getLeftAnswerOptionId() {
        return leftAnswerOptionId;
    }

    public void setLeftAnswerOptionId(Long leftAnswerOptionId) {
        this.leftAnswerOptionId = leftAnswerOptionId;
    }

    public Long getRightAnswerOptionId() {
        return rightAnswerOptionId;
    }

    public void setRightAnswerOptionId(Long rightAnswerOptionId) {
        this.rightAnswerOptionId = rightAnswerOptionId;
    }

    public Integer getUserOrderPosition() {
        return userOrderPosition;
    }

    public void setUserOrderPosition(Integer userOrderPosition) {
        this.userOrderPosition = userOrderPosition;
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
