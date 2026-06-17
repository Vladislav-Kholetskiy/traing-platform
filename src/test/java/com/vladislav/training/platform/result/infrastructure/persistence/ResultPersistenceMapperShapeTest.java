package com.vladislav.training.platform.result.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
/**
 * Проверяет форму и состав {@code ResultPersistenceMapper}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class ResultPersistenceMapperShapeTest {

    private static final Instant COMPLETED_AT = Instant.parse("2026-04-13T08:40:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-04-13T08:45:00Z");

    private final ResultPersistenceMapper mapper = new ResultPersistenceMapper();

    @Test
    void mapperCarriesAllCanonicalFieldsFromDomainToEntityAcrossResultPersistenceFamily() {
        assertThat(mapper.toEntity(assignedResult()))
            .usingRecursiveComparison()
            .isEqualTo(assignedResultEntity());
        assertThat(mapper.toEntity(resultQuestionSnapshot()))
            .usingRecursiveComparison()
            .isEqualTo(resultQuestionSnapshotEntity());
        assertThat(mapper.toEntity(resultAnswerOptionSnapshot()))
            .usingRecursiveComparison()
            .isEqualTo(resultAnswerOptionSnapshotEntity());
    }

    @Test
    void mapperCarriesAllCanonicalFieldsFromEntityToDomainAcrossResultPersistenceFamily() {
        assertThat(mapper.toDomain(assignedResultEntity())).isEqualTo(assignedResult());
        assertThat(mapper.toDomain(resultQuestionSnapshotEntity())).isEqualTo(resultQuestionSnapshot());
        assertThat(mapper.toDomain(resultAnswerOptionSnapshotEntity())).isEqualTo(resultAnswerOptionSnapshot());
    }

    @Test
    void mapperFlattensAndUnflattensScoringAndOrgContextSnapshotsWithoutDataLoss() {
        ResultEntity assignedEntity = assignedResultEntity();
        ResultEntity selfEntity = selfResultEntity();

        assertThat(mapper.toDomain(assignedEntity).scoringSnapshot())
            .isEqualTo(assignedResult().scoringSnapshot());
        assertThat(mapper.toDomain(assignedEntity).orgContextSnapshot())
            .isEqualTo(assignedResult().orgContextSnapshot());
        assertThat(mapper.toDomain(assignedEntity).userIdSnapshot())
            .isEqualTo(assignedResult().userIdSnapshot());
        assertThat(mapper.toDomain(selfEntity).assignmentId()).isNull();
        assertThat(mapper.toDomain(selfEntity).assignmentTestId()).isNull();
        assertThat(mapper.toDomain(selfEntity).withinDeadline()).isNull();
        assertThat(mapper.toDomain(selfEntity).countedInAssignment()).isNull();

        assertThat(mapper.toResults(List.of(assignedEntity, selfEntity)))
            .containsExactly(assignedResult(), selfResult());
        assertThat(mapper.toResultQuestionSnapshots(List.of(resultQuestionSnapshotEntity())))
            .containsExactly(resultQuestionSnapshot());
        assertThat(mapper.toResultAnswerOptionSnapshots(List.of(resultAnswerOptionSnapshotEntity())))
            .containsExactly(resultAnswerOptionSnapshot());
    }

    @Test
    void mapperRemainsCompileSafeForNullInputsAndListHelpers() {
        assertThat(ResultPersistenceMapper.class.isAnnotationPresent(Component.class)).isTrue();

        assertThat(mapper.toDomain((ResultEntity) null)).isNull();
        assertThat(mapper.toEntity((Result) null)).isNull();
        assertThat(mapper.toResults(null)).isEmpty();

        assertThat(mapper.toDomain((ResultQuestionSnapshotEntity) null)).isNull();
        assertThat(mapper.toEntity((ResultQuestionSnapshot) null)).isNull();
        assertThat(mapper.toResultQuestionSnapshots(null)).isEmpty();

        assertThat(mapper.toDomain((ResultAnswerOptionSnapshotEntity) null)).isNull();
        assertThat(mapper.toEntity((ResultAnswerOptionSnapshot) null)).isNull();
        assertThat(mapper.toResultAnswerOptionSnapshots(null)).isEmpty();
    }

    private Result assignedResult() {
        return new Result(
            101L,
            201L,
            601L,
            AttemptMode.ASSIGNED,
            301L,
            401L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("8.5000"),
                new BigDecimal("10.0000"),
                new BigDecimal("85.0000"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            COMPLETED_AT,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            CREATED_AT
        );
    }

    private Result selfResult() {
        return new Result(
            102L,
            202L,
            602L,
            AttemptMode.SELF,
            null,
            null,
            502L,
            "Self Test",
            new ResultScoringSnapshot(
                new BigDecimal("50.0000"),
                new BigDecimal("4.0000"),
                new BigDecimal("8.0000"),
                new BigDecimal("50.0000"),
                true,
                "SELF_POLICY",
                "{\"policy\":\"self\"}"
            ),
            null,
            null,
            COMPLETED_AT,
            new ResultOrgContextSnapshot(502L, "/company/self"),
            false,
            CREATED_AT
        );
    }

    private ResultEntity assignedResultEntity() {
        ResultEntity entity = new ResultEntity();
        entity.setId(101L);
        entity.setTestAttemptId(201L);
        entity.setUserIdSnapshot(601L);
        entity.setAttemptMode(AttemptMode.ASSIGNED);
        entity.setAssignmentId(301L);
        entity.setAssignmentTestId(401L);
        entity.setTestIdSnapshot(501L);
        entity.setTestNameSnapshot("Assigned Test");
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setEarnedScore(new BigDecimal("8.5000"));
        entity.setMaxScore(new BigDecimal("10.0000"));
        entity.setScorePercent(new BigDecimal("85.0000"));
        entity.setPassed(true);
        entity.setWithinDeadline(true);
        entity.setCountedInAssignment(true);
        entity.setScoringPolicyCode("DEFAULT_POLICY");
        entity.setScoringPolicySnapshot("{\"policy\":\"v1\"}");
        entity.setCompletedAt(COMPLETED_AT);
        entity.setOrganizationalUnitIdSnapshot(501L);
        entity.setOrganizationalPathSnapshot("/company/ops");
        entity.setSnapshotFinalTopicControlFlag(true);
        entity.setCreatedAt(CREATED_AT);
        return entity;
    }

    private ResultEntity selfResultEntity() {
        ResultEntity entity = new ResultEntity();
        entity.setId(102L);
        entity.setTestAttemptId(202L);
        entity.setUserIdSnapshot(602L);
        entity.setAttemptMode(AttemptMode.SELF);
        entity.setAssignmentId(null);
        entity.setAssignmentTestId(null);
        entity.setTestIdSnapshot(502L);
        entity.setTestNameSnapshot("Self Test");
        entity.setThresholdPercent(new BigDecimal("50.0000"));
        entity.setEarnedScore(new BigDecimal("4.0000"));
        entity.setMaxScore(new BigDecimal("8.0000"));
        entity.setScorePercent(new BigDecimal("50.0000"));
        entity.setPassed(true);
        entity.setWithinDeadline(null);
        entity.setCountedInAssignment(null);
        entity.setScoringPolicyCode("SELF_POLICY");
        entity.setScoringPolicySnapshot("{\"policy\":\"self\"}");
        entity.setCompletedAt(COMPLETED_AT);
        entity.setOrganizationalUnitIdSnapshot(502L);
        entity.setOrganizationalPathSnapshot("/company/self");
        entity.setSnapshotFinalTopicControlFlag(false);
        entity.setCreatedAt(CREATED_AT);
        return entity;
    }

    private ResultQuestionSnapshot resultQuestionSnapshot() {
        return new ResultQuestionSnapshot(
            601L,
            101L,
            701L,
            "Question body",
            ResultQuestionType.MATCHING,
            2,
            new BigDecimal("1.5000"),
            "{\"pairs\":[1,2]}",
            "{\"userPairs\":[2,1]}",
            new BigDecimal("1.5000"),
            new BigDecimal("2.0000"),
            false,
            "partially correct",
            CREATED_AT
        );
    }

    private ResultQuestionSnapshotEntity resultQuestionSnapshotEntity() {
        ResultQuestionSnapshotEntity entity = new ResultQuestionSnapshotEntity();
        entity.setId(601L);
        entity.setResultId(101L);
        entity.setQuestionOriginalId(701L);
        entity.setBody("Question body");
        entity.setQuestionType(ResultQuestionType.MATCHING);
        entity.setDisplayOrder(2);
        entity.setWeight(new BigDecimal("1.5000"));
        entity.setCorrectAnswerSnapshot("{\"pairs\":[1,2]}");
        entity.setUserAnswerSnapshot("{\"userPairs\":[2,1]}");
        entity.setEarnedScore(new BigDecimal("1.5000"));
        entity.setMaxScore(new BigDecimal("2.0000"));
        entity.setCorrect(false);
        entity.setEvaluationNote("partially correct");
        entity.setCreatedAt(CREATED_AT);
        return entity;
    }

    private ResultAnswerOptionSnapshot resultAnswerOptionSnapshot() {
        return new ResultAnswerOptionSnapshot(
            801L,
            601L,
            901L,
            "Option body",
            1,
            true,
            false,
            CREATED_AT
        );
    }

    private ResultAnswerOptionSnapshotEntity resultAnswerOptionSnapshotEntity() {
        ResultAnswerOptionSnapshotEntity entity = new ResultAnswerOptionSnapshotEntity();
        entity.setId(801L);
        entity.setResultQuestionSnapshotId(601L);
        entity.setAnswerOptionOriginalId(901L);
        entity.setBody("Option body");
        entity.setDisplayOrder(1);
        entity.setCorrectAtSnapshot(true);
        entity.setSelectedByUser(false);
        entity.setCreatedAt(CREATED_AT);
        return entity;
    }
}
