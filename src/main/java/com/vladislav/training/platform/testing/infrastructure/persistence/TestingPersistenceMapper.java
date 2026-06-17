package com.vladislav.training.platform.testing.infrastructure.persistence;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Преобразователь {@code TestingPersistenceMapper}.
 */
@Component
public class TestingPersistenceMapper {

    public TestAttempt toDomain(TestAttemptEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TestAttempt(
            entity.getId(),
            entity.getUserId(),
            entity.getTestId(),
            entity.getAssignmentTestId(),
            entity.getAttemptMode(),
            entity.getStatus(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getExpiredAt(),
            entity.getAbandonedAt(),
            entity.getLastActivityAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public TestAttemptEntity toEntity(TestAttempt domain) {
        if (domain == null) {
            return null;
        }
        TestAttemptEntity entity = new TestAttemptEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setTestId(domain.testId());
        entity.setAssignmentTestId(domain.assignmentTestId());
        entity.setAttemptMode(domain.attemptMode());
        entity.setStatus(domain.status());
        entity.setStartedAt(domain.startedAt());
        entity.setCompletedAt(domain.completedAt());
        entity.setExpiredAt(domain.expiredAt());
        entity.setAbandonedAt(domain.abandonedAt());
        entity.setLastActivityAt(domain.lastActivityAt());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<TestAttempt> toTestAttempts(List<TestAttemptEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public UserAnswer toDomain(UserAnswerEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserAnswer(
            entity.getId(),
            entity.getTestAttemptId(),
            entity.getQuestionId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public UserAnswerEntity toEntity(UserAnswer domain) {
        if (domain == null) {
            return null;
        }
        UserAnswerEntity entity = new UserAnswerEntity();
        entity.setId(domain.id());
        entity.setTestAttemptId(domain.testAttemptId());
        entity.setQuestionId(domain.questionId());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<UserAnswer> toUserAnswers(List<UserAnswerEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public UserAnswerItem toDomain(UserAnswerItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserAnswerItem(
            entity.getId(),
            entity.getUserAnswerId(),
            entity.getAnswerOptionId(),
            entity.getLeftAnswerOptionId(),
            entity.getRightAnswerOptionId(),
            entity.getUserOrderPosition(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public UserAnswerItemEntity toEntity(UserAnswerItem domain) {
        if (domain == null) {
            return null;
        }
        UserAnswerItemEntity entity = new UserAnswerItemEntity();
        entity.setId(domain.id());
        entity.setUserAnswerId(domain.userAnswerId());
        entity.setAnswerOptionId(domain.answerOptionId());
        entity.setLeftAnswerOptionId(domain.leftAnswerOptionId());
        entity.setRightAnswerOptionId(domain.rightAnswerOptionId());
        entity.setUserOrderPosition(domain.userOrderPosition());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<UserAnswerItem> toUserAnswerItems(List<UserAnswerItemEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }
}
