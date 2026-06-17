package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.QuestionType;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA-сущность {@code QuestionEntity}.
 */
@Entity
@Table(name = "question")
public class QuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="topic_id", nullable=false) private Long topicId;
    @Column(name="body", nullable=false) private String body;
    @Enumerated(EnumType.STRING) @Column(name="question_type", nullable=false) private QuestionType questionType;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false) private ContentStatus status;
    @Column(name="sort_order") private Integer sortOrder;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected QuestionEntity(){}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getTopicId(){return topicId;} public void setTopicId(Long topicId){this.topicId=topicId;}
    public String getBody(){return body;} public void setBody(String body){this.body=body;}
    public QuestionType getQuestionType(){return questionType;} public void setQuestionType(QuestionType questionType){this.questionType=questionType;}
    public ContentStatus getStatus(){return status;} public void setStatus(ContentStatus status){this.status=status;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer sortOrder){this.sortOrder=sortOrder;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
