package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA-сущность {@code TestEntity}.
 */
@Entity
@Table(name = "test")
public class TestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="topic_id", nullable=false) private Long topicId;
    @Column(name="name", nullable=false) private String name;
    @Column(name="description") private String description;
    @Enumerated(EnumType.STRING) @Column(name="test_type", nullable=false) private TestType testType;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false) private ContentStatus status;
    @Column(name="threshold_percent", nullable=false) private BigDecimal thresholdPercent;
    @Column(name="scoring_policy_code", nullable=false) private String scoringPolicyCode;
    @Column(name="is_active_final_for_topic", nullable=false) private boolean activeFinalForTopic;
    @Column(name="sort_order", nullable=false) private int sortOrder;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected TestEntity(){}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getTopicId(){return topicId;} public void setTopicId(Long topicId){this.topicId=topicId;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public TestType getTestType(){return testType;} public void setTestType(TestType testType){this.testType=testType;}
    public ContentStatus getStatus(){return status;} public void setStatus(ContentStatus status){this.status=status;}
    public BigDecimal getThresholdPercent(){return thresholdPercent;} public void setThresholdPercent(BigDecimal thresholdPercent){this.thresholdPercent=thresholdPercent;}
    public String getScoringPolicyCode(){return scoringPolicyCode;} public void setScoringPolicyCode(String scoringPolicyCode){this.scoringPolicyCode=scoringPolicyCode;}
    public boolean isActiveFinalForTopic(){return activeFinalForTopic;} public void setActiveFinalForTopic(boolean activeFinalForTopic){this.activeFinalForTopic=activeFinalForTopic;}
    public int getSortOrder(){return sortOrder;} public void setSortOrder(int sortOrder){this.sortOrder=sortOrder;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
