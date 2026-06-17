package com.vladislav.training.platform.result.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataResultQuestionSnapshotJpaRepository}.
 */
public interface SpringDataResultQuestionSnapshotJpaRepository
    extends JpaRepository<ResultQuestionSnapshotEntity, Long> {

    List<ResultQuestionSnapshotEntity> findAllByResultIdOrderByDisplayOrderAscIdAsc(Long resultId);
}
