package com.vladislav.training.platform.result.repository;

import com.vladislav.training.platform.result.domain.Result;
import java.util.List;
/**
 * Контракт репозитория {@code ResultRepository}.
 */
public interface ResultRepository {

    Result findResultById(Long resultId);

    Result findResultByTestAttemptId(Long testAttemptId);

    List<Result> findResultsByAssignmentId(Long assignmentId);

    List<Result> findResultsByAssignmentTestId(Long assignmentTestId);

    Result saveResult(Result result);
}
