package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.repository.ResultRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Адаптер репозитория {@code JpaResultRepositoryAdapter}.
 */

@Repository
@Transactional(readOnly = true)
public class JpaResultRepositoryAdapter implements ResultRepository {

    private final SpringDataResultJpaRepository repository;
    private final ResultPersistenceMapper mapper;

    public JpaResultRepositoryAdapter(
        SpringDataResultJpaRepository repository,
        ResultPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Result findResultById(Long resultId) {
        return repository.findById(resultId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Result not found: " + resultId));
    }

    @Override
    public Result findResultByTestAttemptId(Long testAttemptId) {
        return repository.findByTestAttemptId(testAttemptId)
            .map(mapper::toDomain)
            .orElse(null);
    }

    @Override
    public List<Result> findResultsByAssignmentId(Long assignmentId) {
        return mapper.toResults(repository.findAllByAssignmentIdOrderByIdAsc(assignmentId));
    }

    @Override
    public List<Result> findResultsByAssignmentTestId(Long assignmentTestId) {
        return mapper.toResults(repository.findAllByAssignmentTestIdOrderByIdAsc(assignmentTestId));
    }

    @Override
    @Transactional
    public Result saveResult(Result result) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(result)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist result", exception);
        }
    }
}
