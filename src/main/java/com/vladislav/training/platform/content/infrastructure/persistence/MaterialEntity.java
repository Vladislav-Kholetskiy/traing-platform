package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.MaterialType;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA-сущность {@code MaterialEntity}.
 */
@Entity
@Table(name = "material")
public class MaterialEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="topic_id", nullable=false) private Long topicId;
    @Column(name="name", nullable=false) private String name;
    @Column(name="description") private String description;
    @Column(name="body") private String body;
    @Column(name="video_url") private String videoUrl;
    @Enumerated(EnumType.STRING) @Column(name="material_type", nullable=false) private MaterialType materialType;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false) private ContentStatus status;
    @Column(name="sort_order", nullable=false) private int sortOrder;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected MaterialEntity(){}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getTopicId(){return topicId;} public void setTopicId(Long topicId){this.topicId=topicId;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public String getBody(){return body;} public void setBody(String body){this.body=body;}
    public String getVideoUrl(){return videoUrl;} public void setVideoUrl(String videoUrl){this.videoUrl=videoUrl;}
    public MaterialType getMaterialType(){return materialType;} public void setMaterialType(MaterialType materialType){this.materialType=materialType;}
    public ContentStatus getStatus(){return status;} public void setStatus(ContentStatus status){this.status=status;}
    public int getSortOrder(){return sortOrder;} public void setSortOrder(int sortOrder){this.sortOrder=sortOrder;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
