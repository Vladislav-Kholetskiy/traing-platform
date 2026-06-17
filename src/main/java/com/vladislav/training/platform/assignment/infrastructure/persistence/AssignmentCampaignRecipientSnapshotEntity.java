package com.vladislav.training.platform.assignment.infrastructure.persistence;

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
    name = "assignment_campaign_recipient_snapshot",
    indexes = {
        @Index(name = "ix_asg_camp_rcpt_snap__campaign_id", columnList = "campaign_id"),
        @Index(name = "ix_asg_camp_rcpt_snap__user_id", columnList = "user_id"),
        @Index(name = "ix_asg_camp_rcpt_snap__org_unit_id_snap", columnList = "organizational_unit_id_snapshot")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_asg_camp_rcpt_snap__campaign_id__user_id",
            columnNames = {"campaign_id", "user_id"}
        )
    }
)
public class AssignmentCampaignRecipientSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "organizational_unit_id_snapshot", nullable = false)
    private Long organizationalUnitIdSnapshot;

    @Column(name = "organizational_path_snapshot", nullable = false)
    private String organizationalPathSnapshot;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "inclusion_basis_code", nullable = false)
    private String inclusionBasisCode;

    @Column(name = "employee_number_snapshot")
    private String employeeNumberSnapshot;

    @Column(name = "full_name_snapshot")
    private String fullNameSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssignmentCampaignRecipientSnapshotEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrganizationalUnitIdSnapshot() {
        return organizationalUnitIdSnapshot;
    }

    public void setOrganizationalUnitIdSnapshot(Long organizationalUnitIdSnapshot) {
        this.organizationalUnitIdSnapshot = organizationalUnitIdSnapshot;
    }

    public String getOrganizationalPathSnapshot() {
        return organizationalPathSnapshot;
    }

    public void setOrganizationalPathSnapshot(String organizationalPathSnapshot) {
        this.organizationalPathSnapshot = organizationalPathSnapshot;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getInclusionBasisCode() {
        return inclusionBasisCode;
    }

    public void setInclusionBasisCode(String inclusionBasisCode) {
        this.inclusionBasisCode = inclusionBasisCode;
    }

    public String getEmployeeNumberSnapshot() {
        return employeeNumberSnapshot;
    }

    public void setEmployeeNumberSnapshot(String employeeNumberSnapshot) {
        this.employeeNumberSnapshot = employeeNumberSnapshot;
    }

    public String getFullNameSnapshot() {
        return fullNameSnapshot;
    }

    public void setFullNameSnapshot(String fullNameSnapshot) {
        this.fullNameSnapshot = fullNameSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
