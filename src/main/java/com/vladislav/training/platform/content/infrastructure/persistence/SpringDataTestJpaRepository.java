package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/**
 * Контракт репозитория {@code SpringDataTestJpaRepository}.
 */
public interface SpringDataTestJpaRepository extends JpaRepository<TestEntity, Long> {

    List<TestEntity> findAllByTopicIdOrderBySortOrderAscIdAsc(Long topicId);

    List<TestEntity> findAllByTopicIdAndStatusOrderBySortOrderAscIdAsc(Long topicId, ContentStatus status);

    List<TestEntity> findAllByTopicIdAndStatusAndTestTypeOrderBySortOrderAscIdAsc(Long topicId, ContentStatus status, TestType testType);

    Optional<TestEntity> findByTopicIdAndActiveFinalForTopicTrue(Long topicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TestEntity t where t.id = :testId")
    Optional<TestEntity> findByIdForUpdate(@Param("testId") Long testId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TestEntity t where t.topicId = :topicId and t.activeFinalForTopic = true")
    Optional<TestEntity> findActiveFinalByTopicIdForUpdate(@Param("topicId") Long topicId);

    boolean existsByTopicIdAndStatusNot(Long topicId, ContentStatus status);
}
