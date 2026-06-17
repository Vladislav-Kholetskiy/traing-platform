package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataTestQuestionJpaRepository}.
 */
public interface SpringDataTestQuestionJpaRepository extends JpaRepository<TestQuestionEntity, Long> {
    List<TestQuestionEntity> findAllByTestIdOrderByDisplayOrderAscIdAsc(Long testId);

    @Query("""
        select case when count(tq) > 0 then true else false end
        from TestQuestionEntity tq
        join TestEntity t on t.id = tq.testId
        where tq.questionId = :questionId
          and t.status = :status
        """)
    boolean existsByQuestionIdAndTestStatus(
        @Param("questionId") Long questionId,
        @Param("status") ContentStatus status
    );
}
