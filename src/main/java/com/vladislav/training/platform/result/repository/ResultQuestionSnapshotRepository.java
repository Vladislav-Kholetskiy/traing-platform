package com.vladislav.training.platform.result.repository;

import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import java.util.List;
/**
 * Контракт репозитория {@code ResultQuestionSnapshotRepository}.
 */
public interface ResultQuestionSnapshotRepository {

    ResultQuestionSnapshot findResultQuestionSnapshotById(Long resultQuestionSnapshotId);

    List<ResultQuestionSnapshot> findResultQuestionSnapshotsByResultId(Long resultId);

    ResultQuestionSnapshot saveResultQuestionSnapshot(ResultQuestionSnapshot resultQuestionSnapshot);
}
