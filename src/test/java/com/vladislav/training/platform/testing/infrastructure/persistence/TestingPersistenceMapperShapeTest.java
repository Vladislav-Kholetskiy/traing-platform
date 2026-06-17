package com.vladislav.training.platform.testing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
/**
 * Проверяет форму и состав {@code TestingPersistenceMapper}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class TestingPersistenceMapperShapeTest {

    private static final Instant STARTED_AT = Instant.parse("2026-04-12T08:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-04-12T09:00:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2026-04-12T09:05:00Z");
    private static final Instant ABANDONED_AT = Instant.parse("2026-04-12T09:10:00Z");
    private static final Instant LAST_ACTIVITY_AT = Instant.parse("2026-04-12T08:30:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-04-12T07:55:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-12T08:35:00Z");

    private final TestingPersistenceMapper mapper = new TestingPersistenceMapper();

    @Test
    void mapperCarriesAllCanonicalFieldsFromDomainToEntityAcrossTestingPersistenceFamily() {
        assertThat(mapper.toEntity(testAttempt()))
            .usingRecursiveComparison()
            .isEqualTo(testAttemptEntity());
        assertThat(mapper.toEntity(userAnswer()))
            .usingRecursiveComparison()
            .isEqualTo(userAnswerEntity());
        assertThat(mapper.toEntity(userAnswerItem()))
            .usingRecursiveComparison()
            .isEqualTo(userAnswerItemEntity());
    }

    @Test
    void mapperCarriesAllCanonicalFieldsFromEntityToDomainAcrossTestingPersistenceFamily() {
        assertThat(mapper.toDomain(testAttemptEntity())).isEqualTo(testAttempt());
        assertThat(mapper.toDomain(userAnswerEntity())).isEqualTo(userAnswer());
        assertThat(mapper.toDomain(userAnswerItemEntity())).isEqualTo(userAnswerItem());
    }

    @Test
    void mapperPreservesNullableFieldsEnumsTimestampsAndListConversions() {
        TestAttemptEntity assignedEntity = testAttemptEntity();
        TestAttemptEntity selfEntity = selfAttemptEntity();
        UserAnswerItemEntity nullableItemEntity = nullableUserAnswerItemEntity();

        assertThat(mapper.toDomain(selfEntity).assignmentTestId()).isNull();
        assertThat(mapper.toDomain(selfEntity).attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(mapper.toDomain(assignedEntity).status()).isEqualTo(TestAttemptStatus.IN_PROGRESS);
        assertThat(mapper.toDomain(assignedEntity).completedAt()).isEqualTo(COMPLETED_AT);
        assertThat(mapper.toDomain(assignedEntity).expiredAt()).isEqualTo(EXPIRED_AT);
        assertThat(mapper.toDomain(assignedEntity).abandonedAt()).isEqualTo(ABANDONED_AT);
        assertThat(mapper.toDomain(nullableItemEntity).answerOptionId()).isNull();
        assertThat(mapper.toDomain(nullableItemEntity).leftAnswerOptionId()).isEqualTo(901L);
        assertThat(mapper.toDomain(nullableItemEntity).rightAnswerOptionId()).isNull();
        assertThat(mapper.toDomain(nullableItemEntity).userOrderPosition()).isNull();

        assertThat(mapper.toTestAttempts(List.of(assignedEntity, selfEntity)))
            .containsExactly(testAttempt(), selfAttempt());
        assertThat(mapper.toUserAnswers(List.of(userAnswerEntity()))).containsExactly(userAnswer());
        assertThat(mapper.toUserAnswerItems(List.of(userAnswerItemEntity(), nullableItemEntity)))
            .containsExactly(userAnswerItem(), nullableUserAnswerItem());
    }

    @Test
    void mapperRemainsCompileSafeForNullInputsAndListHelpers() {
        assertThat(TestingPersistenceMapper.class.isAnnotationPresent(Component.class)).isTrue();

        assertThat(mapper.toDomain((TestAttemptEntity) null)).isNull();
        assertThat(mapper.toEntity((TestAttempt) null)).isNull();
        assertThat(mapper.toTestAttempts(null)).isEmpty();

        assertThat(mapper.toDomain((UserAnswerEntity) null)).isNull();
        assertThat(mapper.toEntity((UserAnswer) null)).isNull();
        assertThat(mapper.toUserAnswers(null)).isEmpty();

        assertThat(mapper.toDomain((UserAnswerItemEntity) null)).isNull();
        assertThat(mapper.toEntity((UserAnswerItem) null)).isNull();
        assertThat(mapper.toUserAnswerItems(null)).isEmpty();
    }

    private TestAttempt testAttempt() {
        return new TestAttempt(
            101L,
            201L,
            301L,
            401L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.IN_PROGRESS,
            STARTED_AT,
            COMPLETED_AT,
            EXPIRED_AT,
            ABANDONED_AT,
            LAST_ACTIVITY_AT,
            CREATED_AT,
            UPDATED_AT
        );
    }

    private TestAttempt selfAttempt() {
        return new TestAttempt(
            102L,
            202L,
            302L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            STARTED_AT,
            null,
            null,
            null,
            LAST_ACTIVITY_AT,
            CREATED_AT,
            UPDATED_AT
        );
    }

    private TestAttemptEntity testAttemptEntity() {
        TestAttemptEntity entity = new TestAttemptEntity();
        entity.setId(101L);
        entity.setUserId(201L);
        entity.setTestId(301L);
        entity.setAssignmentTestId(401L);
        entity.setAttemptMode(AttemptMode.ASSIGNED);
        entity.setStatus(TestAttemptStatus.IN_PROGRESS);
        entity.setStartedAt(STARTED_AT);
        entity.setCompletedAt(COMPLETED_AT);
        entity.setExpiredAt(EXPIRED_AT);
        entity.setAbandonedAt(ABANDONED_AT);
        entity.setLastActivityAt(LAST_ACTIVITY_AT);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private TestAttemptEntity selfAttemptEntity() {
        TestAttemptEntity entity = new TestAttemptEntity();
        entity.setId(102L);
        entity.setUserId(202L);
        entity.setTestId(302L);
        entity.setAssignmentTestId(null);
        entity.setAttemptMode(AttemptMode.SELF);
        entity.setStatus(TestAttemptStatus.STARTED);
        entity.setStartedAt(STARTED_AT);
        entity.setCompletedAt(null);
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(LAST_ACTIVITY_AT);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private UserAnswer userAnswer() {
        return new UserAnswer(501L, 101L, 601L, CREATED_AT, UPDATED_AT);
    }

    private UserAnswerEntity userAnswerEntity() {
        UserAnswerEntity entity = new UserAnswerEntity();
        entity.setId(501L);
        entity.setTestAttemptId(101L);
        entity.setQuestionId(601L);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private UserAnswerItem userAnswerItem() {
        return new UserAnswerItem(701L, 501L, 801L, null, null, 2, CREATED_AT, UPDATED_AT);
    }

    private UserAnswerItem nullableUserAnswerItem() {
        return new UserAnswerItem(702L, 501L, null, 901L, null, null, CREATED_AT, UPDATED_AT);
    }

    private UserAnswerItemEntity userAnswerItemEntity() {
        UserAnswerItemEntity entity = new UserAnswerItemEntity();
        entity.setId(701L);
        entity.setUserAnswerId(501L);
        entity.setAnswerOptionId(801L);
        entity.setLeftAnswerOptionId(null);
        entity.setRightAnswerOptionId(null);
        entity.setUserOrderPosition(2);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private UserAnswerItemEntity nullableUserAnswerItemEntity() {
        UserAnswerItemEntity entity = new UserAnswerItemEntity();
        entity.setId(702L);
        entity.setUserAnswerId(501L);
        entity.setAnswerOptionId(null);
        entity.setLeftAnswerOptionId(901L);
        entity.setRightAnswerOptionId(null);
        entity.setUserOrderPosition(null);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }
}
