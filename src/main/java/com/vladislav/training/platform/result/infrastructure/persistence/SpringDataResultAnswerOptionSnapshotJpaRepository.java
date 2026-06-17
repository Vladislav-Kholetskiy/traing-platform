package com.vladislav.training.platform.result.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataResultAnswerOptionSnapshotJpaRepository}.
 */
public interface SpringDataResultAnswerOptionSnapshotJpaRepository
    extends JpaRepository<ResultAnswerOptionSnapshotEntity, Long> {

    List<ResultAnswerOptionSnapshotEntity> findAllByResultQuestionSnapshotIdOrderByDisplayOrderAscIdAsc(
        Long resultQuestionSnapshotId
    );
}
