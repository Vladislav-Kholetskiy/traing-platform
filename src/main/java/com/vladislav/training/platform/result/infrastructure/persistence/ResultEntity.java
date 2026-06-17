package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.common.model.AttemptMode;
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
    name = "result",
    indexes = {
        @Index(name = "ix_result__asg_id", columnList = "assignment_id"),
        @Index(name = "ix_result__asg_test_id", columnList = "assignment_test_id"),
        @Index(name = "ix_result__completed_at", columnList = "completed_at"),
        @Index(name = "ix_result__user_id_snap", columnList = "user_id_snapshot"),
        @Index(name = "ix_result__org_unit_id_snap", columnList = "organizational_unit_id_snapshot"),
        @Index(name = "ix_result__attempt_mode", columnList = "attempt_mode"),
        @Index(name = "ix_result__passed", columnList = "passed"),
        @Index(name = "ix_result__snapshot_final_topic_control_flag", columnList = "snapshot_final_topic_control_flag")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_result__test_att_id", columnNames = {"test_attempt_id"})
    }
)
/**
 * JPA-сущность {@code ResultEntity}.
 */
public class ResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_attempt_id", nullable = false)
    private Long testAttemptId;

    @Column(name = "user_id_snapshot", nullable = false)
    private Long userIdSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_mode", nullable = false)
    private AttemptMode attemptMode;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "assignment_test_id")
    private Long assignmentTestId;

    @Column(name = "test_id_snapshot", nullable = false)
    private Long testIdSnapshot;

    @Column(name = "test_name_snapshot", nullable = false)
    private String testNameSnapshot;

    @Column(name = "threshold_percent", nullable = false)
    private BigDecimal thresholdPercent;

    @Column(name = "earned_score", nullable = false)
    private BigDecimal earnedScore;

    @Column(name = "max_score", nullable = false)
    private BigDecimal maxScore;

    @Column(name = "score_percent", nullable = false)
    private BigDecimal scorePercent;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "within_deadline")
    private Boolean withinDeadline;

    @Column(name = "counted_in_assignment")
    private Boolean countedInAssignment;

    @Column(name = "scoring_policy_code", nullable = false)
    private String scoringPolicyCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scoring_policy_snapshot", nullable = false, columnDefinition = "jsonb")
    private String scoringPolicySnapshot;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "organizational_unit_id_snapshot", nullable = false)
    private Long organizationalUnitIdSnapshot;

    @Column(name = "organizational_path_snapshot", nullable = false)
    private String organizationalPathSnapshot;

    @Column(name = "snapshot_final_topic_control_flag", nullable = false)
    private boolean snapshotFinalTopicControlFlag;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ResultEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestAttemptId() {
        return testAttemptId;
    }

    public void setTestAttemptId(Long testAttemptId) {
        this.testAttemptId = testAttemptId;
    }

    public Long getUserIdSnapshot() {
        return userIdSnapshot;
    }

    public void setUserIdSnapshot(Long userIdSnapshot) {
        this.userIdSnapshot = userIdSnapshot;
    }

    public AttemptMode getAttemptMode() {
        return attemptMode;
    }

    public void setAttemptMode(AttemptMode attemptMode) {
        this.attemptMode = attemptMode;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getAssignmentTestId() {
        return assignmentTestId;
    }

    public void setAssignmentTestId(Long assignmentTestId) {
        this.assignmentTestId = assignmentTestId;
    }

    public Long getTestIdSnapshot() {
        return testIdSnapshot;
    }

    public void setTestIdSnapshot(Long testIdSnapshot) {
        this.testIdSnapshot = testIdSnapshot;
    }

    public String getTestNameSnapshot() {
        return testNameSnapshot;
    }

    public void setTestNameSnapshot(String testNameSnapshot) {
        this.testNameSnapshot = testNameSnapshot;
    }

    public BigDecimal getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(BigDecimal thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
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

    public BigDecimal getScorePercent() {
        return scorePercent;
    }

    public void setScorePercent(BigDecimal scorePercent) {
        this.scorePercent = scorePercent;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public Boolean getWithinDeadline() {
        return withinDeadline;
    }

    public void setWithinDeadline(Boolean withinDeadline) {
        this.withinDeadline = withinDeadline;
    }

    public Boolean getCountedInAssignment() {
        return countedInAssignment;
    }

    public void setCountedInAssignment(Boolean countedInAssignment) {
        this.countedInAssignment = countedInAssignment;
    }

    public String getScoringPolicyCode() {
        return scoringPolicyCode;
    }

    public void setScoringPolicyCode(String scoringPolicyCode) {
        this.scoringPolicyCode = scoringPolicyCode;
    }

    public String getScoringPolicySnapshot() {
        return scoringPolicySnapshot;
    }

    public void setScoringPolicySnapshot(String scoringPolicySnapshot) {
        this.scoringPolicySnapshot = scoringPolicySnapshot;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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

    public boolean isSnapshotFinalTopicControlFlag() {
        return snapshotFinalTopicControlFlag;
    }

    public void setSnapshotFinalTopicControlFlag(boolean snapshotFinalTopicControlFlag) {
        this.snapshotFinalTopicControlFlag = snapshotFinalTopicControlFlag;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
