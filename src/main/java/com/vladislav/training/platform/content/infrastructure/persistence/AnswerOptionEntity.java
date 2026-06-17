package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA-сущность {@code AnswerOptionEntity}.
 */
@Entity
@Table(name = "answer_option")
public class AnswerOptionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="question_id", nullable=false) private Long questionId;
    @Column(name="body", nullable=false) private String body;
    @Enumerated(EnumType.STRING) @Column(name="answer_option_role", nullable=false) private AnswerOptionRole answerOptionRole;
    @Column(name="is_correct") private Boolean isCorrect;
    @Column(name="display_order", nullable=false) private int displayOrder;
    @Column(name="pairing_key") private String pairingKey;
    @Column(name="canonical_order_position") private Integer canonicalOrderPosition;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected AnswerOptionEntity(){}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getQuestionId(){return questionId;} public void setQuestionId(Long questionId){this.questionId=questionId;}
    public String getBody(){return body;} public void setBody(String body){this.body=body;}
    public AnswerOptionRole getAnswerOptionRole(){return answerOptionRole;} public void setAnswerOptionRole(AnswerOptionRole answerOptionRole){this.answerOptionRole=answerOptionRole;}
    public Boolean getIsCorrect(){return isCorrect;} public void setIsCorrect(Boolean isCorrect){this.isCorrect=isCorrect;}
    public int getDisplayOrder(){return displayOrder;} public void setDisplayOrder(int displayOrder){this.displayOrder=displayOrder;}
    public String getPairingKey(){return pairingKey;} public void setPairingKey(String pairingKey){this.pairingKey=pairingKey;}
    public Integer getCanonicalOrderPosition(){return canonicalOrderPosition;} public void setCanonicalOrderPosition(Integer canonicalOrderPosition){this.canonicalOrderPosition=canonicalOrderPosition;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
