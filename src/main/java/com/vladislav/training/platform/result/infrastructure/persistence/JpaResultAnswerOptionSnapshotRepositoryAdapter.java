package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Адаптер репозитория {@code JpaResultAnswerOptionSnapshotRepositoryAdapter}.
 */

@Repository
@Transactional(readOnly = true)
public class JpaResultAnswerOptionSnapshotRepositoryAdapter implements ResultAnswerOptionSnapshotRepository {

    private final SpringDataResultAnswerOptionSnapshotJpaRepository repository;
    private final ResultPersistenceMapper mapper;

    public JpaResultAnswerOptionSnapshotRepositoryAdapter(
        SpringDataResultAnswerOptionSnapshotJpaRepository repository,
        ResultPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ResultAnswerOptionSnapshot findResultAnswerOptionSnapshotById(Long resultAnswerOptionSnapshotId) {
        return repository.findById(resultAnswerOptionSnapshotId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Result answer option snapshot not found: " + resultAnswerOptionSnapshotId
            ));
    }

    @Override
    public List<ResultAnswerOptionSnapshot> findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(
        Long resultQuestionSnapshotId
    ) {
        return mapper.toResultAnswerOptionSnapshots(
            repository.findAllByResultQuestionSnapshotIdOrderByDisplayOrderAscIdAsc(resultQuestionSnapshotId)
        );
    }

    @Override
    @Transactional
    public ResultAnswerOptionSnapshot saveResultAnswerOptionSnapshot(
        ResultAnswerOptionSnapshot resultAnswerOptionSnapshot
    ) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(resultAnswerOptionSnapshot)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to persist result_answer_option_snapshot",
                exception
            );
        }
    }
}
