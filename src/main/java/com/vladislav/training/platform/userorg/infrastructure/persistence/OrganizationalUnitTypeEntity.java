package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA-сущность {@code OrganizationalUnitTypeEntity}.
 */
@Entity
@Table(name = "organizational_unit_type")
public class OrganizationalUnitTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_kind", nullable = false)
    private OrganizationalNodeKind nodeKind;

    @Column(name = "can_be_operator_home_unit", nullable = false)
    private boolean canBeOperatorHomeUnit;

    @Column(name = "can_be_campaign_target", nullable = false)
    private boolean canBeCampaignTarget;

    @Column(name = "participates_in_subtree_scope", nullable = false)
    private boolean participatesInSubtreeScope;

    @Column(name = "can_have_management_relation", nullable = false)
    private boolean canHaveManagementRelation;

    @Column(name = "can_have_access_area", nullable = false)
    private boolean canHaveAccessArea;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationalUnitTypeEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OrganizationalNodeKind getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(OrganizationalNodeKind nodeKind) {
        this.nodeKind = nodeKind;
    }

    public boolean isCanBeOperatorHomeUnit() {
        return canBeOperatorHomeUnit;
    }

    public void setCanBeOperatorHomeUnit(boolean canBeOperatorHomeUnit) {
        this.canBeOperatorHomeUnit = canBeOperatorHomeUnit;
    }

    public boolean isCanBeCampaignTarget() {
        return canBeCampaignTarget;
    }

    public void setCanBeCampaignTarget(boolean canBeCampaignTarget) {
        this.canBeCampaignTarget = canBeCampaignTarget;
    }

    public boolean isParticipatesInSubtreeScope() {
        return participatesInSubtreeScope;
    }

    public void setParticipatesInSubtreeScope(boolean participatesInSubtreeScope) {
        this.participatesInSubtreeScope = participatesInSubtreeScope;
    }

    public boolean isCanHaveManagementRelation() {
        return canHaveManagementRelation;
    }

    public void setCanHaveManagementRelation(boolean canHaveManagementRelation) {
        this.canHaveManagementRelation = canHaveManagementRelation;
    }

    public boolean isCanHaveAccessArea() {
        return canHaveAccessArea;
    }

    public void setCanHaveAccessArea(boolean canHaveAccessArea) {
        this.canHaveAccessArea = canHaveAccessArea;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
