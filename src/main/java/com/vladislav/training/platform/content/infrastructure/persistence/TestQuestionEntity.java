package com.vladislav.training.platform.content.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA-сущность {@code TestQuestionEntity}.
 */
@Entity
@Table(name = "test_question")
public class TestQuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="test_id", nullable=false) private Long testId;
    @Column(name="question_id", nullable=false) private Long questionId;
    @Column(name="display_order", nullable=false) private int displayOrder;
    @Column(name="weight", nullable=false) private BigDecimal weight;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected TestQuestionEntity(){}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getTestId(){return testId;} public void setTestId(Long testId){this.testId=testId;}
    public Long getQuestionId(){return questionId;} public void setQuestionId(Long questionId){this.questionId=questionId;}
    public int getDisplayOrder(){return displayOrder;} public void setDisplayOrder(int displayOrder){this.displayOrder=displayOrder;}
    public BigDecimal getWeight(){return weight;} public void setWeight(BigDecimal weight){this.weight=weight;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
