package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Адаптер репозитория {@code JpaResultQuestionSnapshotRepositoryAdapter}.
 */

@Repository
@Transactional(readOnly = true)
public class JpaResultQuestionSnapshotRepositoryAdapter implements ResultQuestionSnapshotRepository {

    private final SpringDataResultQuestionSnapshotJpaRepository repository;
    private final ResultPersistenceMapper mapper;

    public JpaResultQuestionSnapshotRepositoryAdapter(
        SpringDataResultQuestionSnapshotJpaRepository repository,
        ResultPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ResultQuestionSnapshot findResultQuestionSnapshotById(Long resultQuestionSnapshotId) {
        return repository.findById(resultQuestionSnapshotId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Result question snapshot not found: " + resultQuestionSnapshotId
            ));
    }

    @Override
    public List<ResultQuestionSnapshot> findResultQuestionSnapshotsByResultId(Long resultId) {
        return mapper.toResultQuestionSnapshots(repository.findAllByResultIdOrderByDisplayOrderAscIdAsc(resultId));
    }

    @Override
    @Transactional
    public ResultQuestionSnapshot saveResultQuestionSnapshot(ResultQuestionSnapshot resultQuestionSnapshot) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(resultQuestionSnapshot)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to persist result_question_snapshot",
                exception
            );
        }
    }
}
