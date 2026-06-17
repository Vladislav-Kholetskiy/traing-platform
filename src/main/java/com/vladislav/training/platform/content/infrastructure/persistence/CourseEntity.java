package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA-сущность {@code CourseEntity}.
 */
@Entity
@Table(name = "course")
public class CourseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="name", nullable=false) private String name;
    @Column(name="description") private String description;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false) private ContentStatus status;
    @Column(name="sort_order") private Integer sortOrder;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected CourseEntity(){}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public ContentStatus getStatus(){return status;} public void setStatus(ContentStatus status){this.status=status;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer sortOrder){this.sortOrder=sortOrder;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
