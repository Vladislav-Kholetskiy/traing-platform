package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import java.util.List;
import org.springframework.stereotype.Component;
/**
 * Преобразователь {@code ResultPersistenceMapper}.
 */

@Component
public class ResultPersistenceMapper {

    public Result toDomain(ResultEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Result(
            entity.getId(),
            entity.getTestAttemptId(),
            entity.getUserIdSnapshot(),
            entity.getAttemptMode(),
            entity.getAssignmentId(),
            entity.getAssignmentTestId(),
            entity.getTestIdSnapshot(),
            entity.getTestNameSnapshot(),
            new ResultScoringSnapshot(
                entity.getThresholdPercent(),
                entity.getEarnedScore(),
                entity.getMaxScore(),
                entity.getScorePercent(),
                entity.isPassed(),
                entity.getScoringPolicyCode(),
                entity.getScoringPolicySnapshot()
            ),
            entity.getWithinDeadline(),
            entity.getCountedInAssignment(),
            entity.getCompletedAt(),
            new ResultOrgContextSnapshot(
                entity.getOrganizationalUnitIdSnapshot(),
                entity.getOrganizationalPathSnapshot()
            ),
            entity.isSnapshotFinalTopicControlFlag(),
            entity.getCreatedAt()
        );
    }

    public ResultEntity toEntity(Result domain) {
        if (domain == null) {
            return null;
        }
        ResultEntity entity = new ResultEntity();
        entity.setId(domain.id());
        entity.setTestAttemptId(domain.testAttemptId());
        entity.setUserIdSnapshot(domain.userIdSnapshot());
        entity.setAttemptMode(domain.attemptMode());
        entity.setAssignmentId(domain.assignmentId());
        entity.setAssignmentTestId(domain.assignmentTestId());
        entity.setTestIdSnapshot(domain.testIdSnapshot());
        entity.setTestNameSnapshot(domain.testNameSnapshot());
        entity.setThresholdPercent(domain.scoringSnapshot().thresholdPercent());
        entity.setEarnedScore(domain.scoringSnapshot().earnedScore());
        entity.setMaxScore(domain.scoringSnapshot().maxScore());
        entity.setScorePercent(domain.scoringSnapshot().scorePercent());
        entity.setPassed(domain.scoringSnapshot().passed());
        entity.setWithinDeadline(domain.withinDeadline());
        entity.setCountedInAssignment(domain.countedInAssignment());
        entity.setScoringPolicyCode(domain.scoringSnapshot().scoringPolicyCode());
        entity.setScoringPolicySnapshot(domain.scoringSnapshot().scoringPolicySnapshot());
        entity.setCompletedAt(domain.completedAt());
        entity.setOrganizationalUnitIdSnapshot(domain.orgContextSnapshot().organizationalUnitIdSnapshot());
        entity.setOrganizationalPathSnapshot(domain.orgContextSnapshot().organizationalPathSnapshot());
        entity.setSnapshotFinalTopicControlFlag(domain.snapshotFinalTopicControlFlag());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public List<Result> toResults(List<ResultEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public ResultQuestionSnapshot toDomain(ResultQuestionSnapshotEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ResultQuestionSnapshot(
            entity.getId(),
            entity.getResultId(),
            entity.getQuestionOriginalId(),
            entity.getTopicIdSnapshot(),
            entity.getBody(),
            entity.getQuestionType(),
            entity.getDisplayOrder(),
            entity.getWeight(),
            entity.getCorrectAnswerSnapshot(),
            entity.getUserAnswerSnapshot(),
            entity.getEarnedScore(),
            entity.getMaxScore(),
            entity.isCorrect(),
            entity.getEvaluationNote(),
            entity.getCreatedAt()
        );
    }

    public ResultQuestionSnapshotEntity toEntity(ResultQuestionSnapshot domain) {
        if (domain == null) {
            return null;
        }
        ResultQuestionSnapshotEntity entity = new ResultQuestionSnapshotEntity();
        entity.setId(domain.id());
        entity.setResultId(domain.resultId());
        entity.setQuestionOriginalId(domain.questionOriginalId());
        entity.setTopicIdSnapshot(domain.topicIdSnapshot());
        entity.setBody(domain.body());
        entity.setQuestionType(domain.questionType());
        entity.setDisplayOrder(domain.displayOrder());
        entity.setWeight(domain.weight());
        entity.setCorrectAnswerSnapshot(domain.correctAnswerSnapshot());
        entity.setUserAnswerSnapshot(domain.userAnswerSnapshot());
        entity.setEarnedScore(domain.earnedScore());
        entity.setMaxScore(domain.maxScore());
        entity.setCorrect(domain.isCorrect());
        entity.setEvaluationNote(domain.evaluationNote());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public List<ResultQuestionSnapshot> toResultQuestionSnapshots(List<ResultQuestionSnapshotEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public ResultAnswerOptionSnapshot toDomain(ResultAnswerOptionSnapshotEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ResultAnswerOptionSnapshot(
            entity.getId(),
            entity.getResultQuestionSnapshotId(),
            entity.getAnswerOptionOriginalId(),
            entity.getBody(),
            entity.getDisplayOrder(),
            entity.isCorrectAtSnapshot(),
            entity.isSelectedByUser(),
            entity.getCreatedAt()
        );
    }

    public ResultAnswerOptionSnapshotEntity toEntity(ResultAnswerOptionSnapshot domain) {
        if (domain == null) {
            return null;
        }
        ResultAnswerOptionSnapshotEntity entity = new ResultAnswerOptionSnapshotEntity();
        entity.setId(domain.id());
        entity.setResultQuestionSnapshotId(domain.resultQuestionSnapshotId());
        entity.setAnswerOptionOriginalId(domain.answerOptionOriginalId());
        entity.setBody(domain.body());
        entity.setDisplayOrder(domain.displayOrder());
        entity.setCorrectAtSnapshot(domain.isCorrectAtSnapshot());
        entity.setSelectedByUser(domain.isSelectedByUser());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public List<ResultAnswerOptionSnapshot> toResultAnswerOptionSnapshots(
        List<ResultAnswerOptionSnapshotEntity> entities
    ) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }
}
