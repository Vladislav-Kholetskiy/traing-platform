package com.vladislav.training.platform.testing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
/**
 * Проверяет поведение {@code TestingPersistenceBaselineAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TestingPersistenceBaselineAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-12T08:00:00Z");

    @Mock
    private SpringDataTestAttemptJpaRepository testAttemptJpaRepository;
    @Mock
    private SpringDataUserAnswerJpaRepository userAnswerJpaRepository;
    @Mock
    private SpringDataUserAnswerItemJpaRepository userAnswerItemJpaRepository;

    private final TestingPersistenceMapper mapper = new TestingPersistenceMapper();

    @Test
    void testAttemptAdapterMapsPrimaryKeyListAndActiveLookups() {
        JpaTestAttemptRepositoryAdapter adapter = new JpaTestAttemptRepositoryAdapter(testAttemptJpaRepository, mapper);
        TestAttemptEntity entity = testAttemptEntity(11L, 21L, 31L, 41L, AttemptMode.ASSIGNED, TestAttemptStatus.STARTED);

        when(testAttemptJpaRepository.findById(11L)).thenReturn(Optional.of(entity));
        when(testAttemptJpaRepository.findAllByUserIdOrderByIdAsc(21L)).thenReturn(List.of(entity));
        when(testAttemptJpaRepository.findAllByAssignmentTestIdOrderByIdAsc(41L)).thenReturn(List.of(entity));
        when(testAttemptJpaRepository.findAllByUserIdAndTestIdOrderByIdAsc(21L, 31L)).thenReturn(List.of(entity));
        when(testAttemptJpaRepository.findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn(any(), any(), any(), any()))
            .thenReturn(Optional.of(entity));
        when(testAttemptJpaRepository.findByUserIdAndAssignmentTestIdAndStatusInForUpdate(any(), any(), any()))
            .thenReturn(Optional.of(entity));
        when(testAttemptJpaRepository.findByIdAndUserIdForUpdate(11L, 21L)).thenReturn(Optional.of(entity));
        when(testAttemptJpaRepository.findByUserIdAndTestIdAndAttemptModeAndStatusIn(any(), any(), any(), any()))
            .thenReturn(Optional.of(testAttemptEntity(12L, 22L, 32L, null, AttemptMode.SELF, TestAttemptStatus.IN_PROGRESS)));

        assertThat(adapter.findTestAttemptById(11L).id()).isEqualTo(11L);
        assertThat(adapter.findTestAttemptsByUserId(21L)).singleElement().extracting(TestAttempt::userId).isEqualTo(21L);
        assertThat(adapter.findTestAttemptsByAssignmentTestId(41L))
            .singleElement()
            .extracting(TestAttempt::assignmentTestId)
            .isEqualTo(41L);
        assertThat(adapter.findTestAttemptsByUserIdAndTestId(21L, 31L))
            .singleElement()
            .satisfies(attempt -> {
                assertThat(attempt.userId()).isEqualTo(21L);
                assertThat(attempt.testId()).isEqualTo(31L);
            });
        assertThat(adapter.findAndLockTestAttemptByIdAndUserId(11L, 21L).id()).isEqualTo(11L);
        assertThat(adapter.findActiveAssignedAttemptForActor(21L, 41L).assignmentTestId()).isEqualTo(41L);
        assertThat(adapter.findAndLockActiveAssignedAttemptForActor(21L, 41L).assignmentTestId()).isEqualTo(41L);
        assertThat(adapter.findActiveSelfAttempt(22L, 32L).attemptMode()).isEqualTo(AttemptMode.SELF);
    }

    @Test
    void testAttemptAdapterReturnsNullForOptionalLookupsAndThrowsForMissingPrimaryKey() {
        JpaTestAttemptRepositoryAdapter adapter = new JpaTestAttemptRepositoryAdapter(testAttemptJpaRepository, mapper);

        when(testAttemptJpaRepository.findById(99L)).thenReturn(Optional.empty());
        when(testAttemptJpaRepository.findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn(any(), any(), any(), any()))
            .thenReturn(Optional.empty());
        when(testAttemptJpaRepository.findByUserIdAndAssignmentTestIdAndStatusInForUpdate(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(testAttemptJpaRepository.findByUserIdAndTestIdAndAttemptModeAndStatusIn(any(), any(), any(), any()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.findTestAttemptById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("99");
        assertThat(adapter.findActiveAssignedAttemptForActor(77L, 55L)).isNull();
        assertThat(adapter.findAndLockActiveAssignedAttemptForActor(77L, 55L)).isNull();
        assertThat(adapter.findActiveSelfAttempt(77L, 88L)).isNull();
    }

    @Test
    void testAttemptAdapterMapsSaveAndWrapsConstraintViolations() {
        JpaTestAttemptRepositoryAdapter adapter = new JpaTestAttemptRepositoryAdapter(testAttemptJpaRepository, mapper);
        TestAttemptEntity entity = testAttemptEntity(13L, 23L, 33L, null, AttemptMode.SELF, TestAttemptStatus.STARTED);

        when(testAttemptJpaRepository.save(any(TestAttemptEntity.class))).thenReturn(entity);

        assertThat(adapter.saveTestAttempt(mapper.toDomain(entity)).id()).isEqualTo(13L);
        verify(testAttemptJpaRepository).save(any(TestAttemptEntity.class));

        when(testAttemptJpaRepository.save(any(TestAttemptEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.saveTestAttempt(mapper.toDomain(entity)))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("test_attempt");
    }

    @Test
    void userAnswerAdapterMapsReadsOptionalLookupAndSave() {
        JpaUserAnswerRepositoryAdapter adapter = new JpaUserAnswerRepositoryAdapter(userAnswerJpaRepository, mapper);
        UserAnswerEntity entity = userAnswerEntity(14L, 24L, 34L);

        when(userAnswerJpaRepository.findById(14L)).thenReturn(Optional.of(entity));
        when(userAnswerJpaRepository.findAllByTestAttemptIdOrderByIdAsc(24L)).thenReturn(List.of(entity));
        when(userAnswerJpaRepository.findByTestAttemptIdAndQuestionId(24L, 34L)).thenReturn(Optional.of(entity));
        when(userAnswerJpaRepository.findByTestAttemptIdAndQuestionId(24L, 35L)).thenReturn(Optional.empty());
        when(userAnswerJpaRepository.save(any(UserAnswerEntity.class))).thenReturn(entity);

        assertThat(adapter.findUserAnswerById(14L).id()).isEqualTo(14L);
        assertThat(adapter.findUserAnswersByTestAttemptId(24L)).singleElement().extracting(UserAnswer::questionId).isEqualTo(34L);
        assertThat(adapter.findUserAnswerByTestAttemptIdAndQuestionId(24L, 34L).questionId()).isEqualTo(34L);
        assertThat(adapter.findUserAnswerByTestAttemptIdAndQuestionId(24L, 35L)).isNull();
        assertThat(adapter.saveUserAnswer(mapper.toDomain(entity)).id()).isEqualTo(14L);
        verify(userAnswerJpaRepository).save(any(UserAnswerEntity.class));
    }

    @Test
    void userAnswerAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaUserAnswerRepositoryAdapter adapter = new JpaUserAnswerRepositoryAdapter(userAnswerJpaRepository, mapper);
        UserAnswerEntity entity = userAnswerEntity(15L, 25L, 35L);

        when(userAnswerJpaRepository.findById(999L)).thenReturn(Optional.empty());
        when(userAnswerJpaRepository.save(any(UserAnswerEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.findUserAnswerById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
        assertThatThrownBy(() -> adapter.saveUserAnswer(mapper.toDomain(entity)))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("user_answer");
    }

    @Test
    void userAnswerItemAdapterMapsPrimaryKeyReadSaveAndDeletes() {
        JpaUserAnswerItemRepositoryAdapter adapter =
            new JpaUserAnswerItemRepositoryAdapter(userAnswerItemJpaRepository, mapper);
        UserAnswerItemEntity entity = userAnswerItemEntity(16L, 26L, 36L);

        when(userAnswerItemJpaRepository.findById(16L)).thenReturn(Optional.of(entity));
        when(userAnswerItemJpaRepository.findAllByUserAnswerIdOrderByIdAsc(26L)).thenReturn(List.of(entity));
        when(userAnswerItemJpaRepository.save(any(UserAnswerItemEntity.class))).thenReturn(entity);

        assertThat(adapter.findUserAnswerItemById(16L).id()).isEqualTo(16L);
        assertThat(adapter.findUserAnswerItemsByUserAnswerId(26L))
            .singleElement()
            .extracting(UserAnswerItem::answerOptionId)
            .isEqualTo(36L);
        assertThat(adapter.saveUserAnswerItem(mapper.toDomain(entity)).id()).isEqualTo(16L);

        adapter.deleteUserAnswerItem(16L);
        adapter.deleteUserAnswerItemsByUserAnswerId(26L);

        verify(userAnswerItemJpaRepository).save(any(UserAnswerItemEntity.class));
        verify(userAnswerItemJpaRepository).deleteById(16L);
        verify(userAnswerItemJpaRepository).deleteAllByUserAnswerId(26L);
    }

    @Test
    void userAnswerItemAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaUserAnswerItemRepositoryAdapter adapter =
            new JpaUserAnswerItemRepositoryAdapter(userAnswerItemJpaRepository, mapper);
        UserAnswerItemEntity entity = userAnswerItemEntity(17L, 27L, 37L);

        when(userAnswerItemJpaRepository.findById(888L)).thenReturn(Optional.empty());
        when(userAnswerItemJpaRepository.save(any(UserAnswerItemEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.findUserAnswerItemById(888L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("888");
        assertThatThrownBy(() -> adapter.saveUserAnswerItem(mapper.toDomain(entity)))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("user_answer_item");
    }

    private TestAttemptEntity testAttemptEntity(
        Long id,
        Long userId,
        Long testId,
        Long assignmentTestId,
        AttemptMode attemptMode,
        TestAttemptStatus status
    ) {
        TestAttemptEntity entity = new TestAttemptEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(attemptMode);
        entity.setStatus(status);
        entity.setStartedAt(FIXED_INSTANT);
        entity.setCompletedAt(null);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(FIXED_INSTANT);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private UserAnswerEntity userAnswerEntity(Long id, Long testAttemptId, Long questionId) {
        UserAnswerEntity entity = new UserAnswerEntity();
        entity.setId(id);
        entity.setTestAttemptId(testAttemptId);
        entity.setQuestionId(questionId);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private UserAnswerItemEntity userAnswerItemEntity(Long id, Long userAnswerId, Long answerOptionId) {
        UserAnswerItemEntity entity = new UserAnswerItemEntity();
        entity.setId(id);
        entity.setUserAnswerId(userAnswerId);
        entity.setAnswerOptionId(answerOptionId);
        entity.setLeftAnswerOptionId(null);
        entity.setRightAnswerOptionId(null);
        entity.setUserOrderPosition(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }
}
