package com.vladislav.training.platform.result.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
/**
 * Проверяет поведение {@code ResultPersistenceBaselineAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ResultPersistenceBaselineAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-13T08:00:00Z");
    private static final Long ACTOR_USER_ID = 501L;

    @Mock
    private SpringDataResultJpaRepository resultJpaRepository;
    @Mock
    private SpringDataResultQuestionSnapshotJpaRepository resultQuestionSnapshotJpaRepository;
    @Mock
    private SpringDataResultAnswerOptionSnapshotJpaRepository resultAnswerOptionSnapshotJpaRepository;

    private final ResultPersistenceMapper mapper = new ResultPersistenceMapper();

    @Test
    void resultAdapterMapsPrimaryKeyOptionalAndListLookups() {
        JpaResultRepositoryAdapter adapter = new JpaResultRepositoryAdapter(resultJpaRepository, mapper);
        ResultEntity entity = resultEntity(11L, 21L, 31L, 41L);

        when(resultJpaRepository.findById(11L)).thenReturn(Optional.of(entity));
        when(resultJpaRepository.findByTestAttemptId(21L)).thenReturn(Optional.of(entity));
        when(resultJpaRepository.findByTestAttemptId(22L)).thenReturn(Optional.empty());
        when(resultJpaRepository.findAllByAssignmentIdOrderByIdAsc(31L)).thenReturn(List.of(entity));
        when(resultJpaRepository.findAllByAssignmentTestIdOrderByIdAsc(41L)).thenReturn(List.of(entity));

        assertThat(adapter.findResultById(11L).id()).isEqualTo(11L);
        assertThat(adapter.findResultById(11L).userIdSnapshot()).isEqualTo(ACTOR_USER_ID);
        assertThat(adapter.findResultByTestAttemptId(21L).testAttemptId()).isEqualTo(21L);
        assertThat(adapter.findResultByTestAttemptId(21L).userIdSnapshot()).isEqualTo(ACTOR_USER_ID);
        assertThat(adapter.findResultByTestAttemptId(22L)).isNull();
        assertThat(adapter.findResultsByAssignmentId(31L))
            .singleElement()
            .satisfies(result -> {
                assertThat(result.assignmentId()).isEqualTo(31L);
                assertThat(result.userIdSnapshot()).isEqualTo(ACTOR_USER_ID);
            });
        assertThat(adapter.findResultsByAssignmentTestId(41L))
            .singleElement()
            .satisfies(result -> {
                assertThat(result.assignmentTestId()).isEqualTo(41L);
                assertThat(result.userIdSnapshot()).isEqualTo(ACTOR_USER_ID);
            });
    }

    @Test
    void resultAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaResultRepositoryAdapter adapter = new JpaResultRepositoryAdapter(resultJpaRepository, mapper);
        ResultEntity entity = resultEntity(12L, 22L, 32L, 42L);

        when(resultJpaRepository.findById(999L)).thenReturn(Optional.empty());
        when(resultJpaRepository.save(any(ResultEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.findResultById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
        Result domain = mapper.toDomain(entity);
        assertThat(domain.userIdSnapshot()).isEqualTo(ACTOR_USER_ID);

        assertThatThrownBy(() -> adapter.saveResult(domain))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result");
    }

    @Test
    void resultAdapterNormalizesDuplicateCanonicalResultForSameAttemptIntoLocalConstraintViolation() {
        JpaResultRepositoryAdapter adapter = new JpaResultRepositoryAdapter(resultJpaRepository, mapper);
        ResultEntity entity = resultEntity(19L, 29L, 39L, 49L);

        when(resultJpaRepository.save(any(ResultEntity.class)))
            .thenThrow(new DataIntegrityViolationException("uq_result__test_att_id"));

        Result domain = mapper.toDomain(entity);
        assertThat(domain.userIdSnapshot()).isEqualTo(ACTOR_USER_ID);

        assertThatThrownBy(() -> adapter.saveResult(domain))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result")
            .hasRootCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resultQuestionSnapshotAdapterMapsPrimaryKeyOrderedReadAndSave() {
        JpaResultQuestionSnapshotRepositoryAdapter adapter =
            new JpaResultQuestionSnapshotRepositoryAdapter(resultQuestionSnapshotJpaRepository, mapper);
        ResultQuestionSnapshotEntity first = resultQuestionSnapshotEntity(13L, 23L, 0);
        ResultQuestionSnapshotEntity second = resultQuestionSnapshotEntity(14L, 23L, 1);

        when(resultQuestionSnapshotJpaRepository.findById(13L)).thenReturn(Optional.of(first));
        when(resultQuestionSnapshotJpaRepository.findAllByResultIdOrderByDisplayOrderAscIdAsc(23L))
            .thenReturn(List.of(first, second));
        when(resultQuestionSnapshotJpaRepository.save(any(ResultQuestionSnapshotEntity.class))).thenReturn(first);

        assertThat(adapter.findResultQuestionSnapshotById(13L).id()).isEqualTo(13L);
        assertThat(adapter.findResultQuestionSnapshotsByResultId(23L))
            .extracting(ResultQuestionSnapshot::displayOrder)
            .containsExactly(0, 1);
        assertThat(adapter.saveResultQuestionSnapshot(mapper.toDomain(first)).id()).isEqualTo(13L);
        verify(resultQuestionSnapshotJpaRepository).save(any(ResultQuestionSnapshotEntity.class));
    }

    @Test
    void resultQuestionSnapshotAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaResultQuestionSnapshotRepositoryAdapter adapter =
            new JpaResultQuestionSnapshotRepositoryAdapter(resultQuestionSnapshotJpaRepository, mapper);
        ResultQuestionSnapshotEntity entity = resultQuestionSnapshotEntity(15L, 25L, 2);

        when(resultQuestionSnapshotJpaRepository.findById(888L)).thenReturn(Optional.empty());
        when(resultQuestionSnapshotJpaRepository.save(any(ResultQuestionSnapshotEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.findResultQuestionSnapshotById(888L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("888");
        assertThatThrownBy(() -> adapter.saveResultQuestionSnapshot(mapper.toDomain(entity)))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_question_snapshot");
    }

    @Test
    void resultAnswerOptionSnapshotAdapterMapsPrimaryKeyOrderedReadAndSave() {
        JpaResultAnswerOptionSnapshotRepositoryAdapter adapter =
            new JpaResultAnswerOptionSnapshotRepositoryAdapter(resultAnswerOptionSnapshotJpaRepository, mapper);
        ResultAnswerOptionSnapshotEntity first = resultAnswerOptionSnapshotEntity(16L, 26L, 0);
        ResultAnswerOptionSnapshotEntity second = resultAnswerOptionSnapshotEntity(17L, 26L, 1);

        when(resultAnswerOptionSnapshotJpaRepository.findById(16L)).thenReturn(Optional.of(first));
        when(resultAnswerOptionSnapshotJpaRepository.findAllByResultQuestionSnapshotIdOrderByDisplayOrderAscIdAsc(26L))
            .thenReturn(List.of(first, second));
        when(resultAnswerOptionSnapshotJpaRepository.save(any(ResultAnswerOptionSnapshotEntity.class))).thenReturn(first);

        assertThat(adapter.findResultAnswerOptionSnapshotById(16L).id()).isEqualTo(16L);
        assertThat(adapter.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(26L))
            .extracting(ResultAnswerOptionSnapshot::displayOrder)
            .containsExactly(0, 1);
        assertThat(adapter.saveResultAnswerOptionSnapshot(mapper.toDomain(first)).id()).isEqualTo(16L);
        verify(resultAnswerOptionSnapshotJpaRepository).save(any(ResultAnswerOptionSnapshotEntity.class));
    }

    @Test
    void resultAnswerOptionSnapshotAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaResultAnswerOptionSnapshotRepositoryAdapter adapter =
            new JpaResultAnswerOptionSnapshotRepositoryAdapter(resultAnswerOptionSnapshotJpaRepository, mapper);
        ResultAnswerOptionSnapshotEntity entity = resultAnswerOptionSnapshotEntity(18L, 28L, 2);

        when(resultAnswerOptionSnapshotJpaRepository.findById(777L)).thenReturn(Optional.empty());
        when(resultAnswerOptionSnapshotJpaRepository.save(any(ResultAnswerOptionSnapshotEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.findResultAnswerOptionSnapshotById(777L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("777");
        assertThatThrownBy(() -> adapter.saveResultAnswerOptionSnapshot(mapper.toDomain(entity)))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_answer_option_snapshot");
    }

    private ResultEntity resultEntity(Long id, Long testAttemptId, Long assignmentId, Long assignmentTestId) {
        ResultEntity entity = new ResultEntity();
        entity.setId(id);
        entity.setTestAttemptId(testAttemptId);
        entity.setUserIdSnapshot(ACTOR_USER_ID);
        entity.setAttemptMode(AttemptMode.ASSIGNED);
        entity.setAssignmentId(assignmentId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setTestIdSnapshot(61L);
        entity.setTestNameSnapshot("Persisted Test");
        entity.setThresholdPercent(new BigDecimal("70.0000"));
        entity.setEarnedScore(new BigDecimal("8.0000"));
        entity.setMaxScore(new BigDecimal("10.0000"));
        entity.setScorePercent(new BigDecimal("80.0000"));
        entity.setPassed(true);
        entity.setWithinDeadline(true);
        entity.setCountedInAssignment(true);
        entity.setScoringPolicyCode("DEFAULT_POLICY");
        entity.setScoringPolicySnapshot("{\"policy\":\"v1\"}");
        entity.setCompletedAt(FIXED_INSTANT);
        entity.setOrganizationalUnitIdSnapshot(51L);
        entity.setOrganizationalPathSnapshot("/company/ops");
        entity.setSnapshotFinalTopicControlFlag(true);
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }

    private ResultQuestionSnapshotEntity resultQuestionSnapshotEntity(Long id, Long resultId, int displayOrder) {
        ResultQuestionSnapshotEntity entity = new ResultQuestionSnapshotEntity();
        entity.setId(id);
        entity.setResultId(resultId);
        entity.setQuestionOriginalId(61L + id);
        entity.setBody("Question " + id);
        entity.setQuestionType(ResultQuestionType.SINGLE_CHOICE);
        entity.setDisplayOrder(displayOrder);
        entity.setWeight(new BigDecimal("1.0000"));
        entity.setCorrectAnswerSnapshot("{\"correct\":1}");
        entity.setUserAnswerSnapshot("{\"selected\":2}");
        entity.setEarnedScore(new BigDecimal("0.0000"));
        entity.setMaxScore(new BigDecimal("1.0000"));
        entity.setCorrect(false);
        entity.setEvaluationNote("not matched");
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }

    private ResultAnswerOptionSnapshotEntity resultAnswerOptionSnapshotEntity(
        Long id,
        Long resultQuestionSnapshotId,
        int displayOrder
    ) {
        ResultAnswerOptionSnapshotEntity entity = new ResultAnswerOptionSnapshotEntity();
        entity.setId(id);
        entity.setResultQuestionSnapshotId(resultQuestionSnapshotId);
        entity.setAnswerOptionOriginalId(71L + id);
        entity.setBody("Option " + id);
        entity.setDisplayOrder(displayOrder);
        entity.setCorrectAtSnapshot(displayOrder == 0);
        entity.setSelectedByUser(displayOrder == 1);
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }
}
