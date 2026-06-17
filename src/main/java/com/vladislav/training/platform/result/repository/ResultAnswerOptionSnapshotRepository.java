package com.vladislav.training.platform.result.repository;

import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import java.util.List;
/**
 * Контракт репозитория {@code ResultAnswerOptionSnapshotRepository}.
 */
public interface ResultAnswerOptionSnapshotRepository {

    ResultAnswerOptionSnapshot findResultAnswerOptionSnapshotById(Long resultAnswerOptionSnapshotId);

    List<ResultAnswerOptionSnapshot> findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(
        Long resultQuestionSnapshotId
    );

    ResultAnswerOptionSnapshot saveResultAnswerOptionSnapshot(
        ResultAnswerOptionSnapshot resultAnswerOptionSnapshot
    );
}
