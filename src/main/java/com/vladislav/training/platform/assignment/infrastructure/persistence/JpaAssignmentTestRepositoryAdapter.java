package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentTestRepositoryAdapter implements AssignmentTestRepository {

    private final SpringDataAssignmentTestJpaRepository repository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentTestRepositoryAdapter(
        SpringDataAssignmentTestJpaRepository repository,
        AssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AssignmentTest findAssignmentTestById(Long assignmentTestId) {
        return repository.findById(assignmentTestId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Assignment test not found: " + assignmentTestId));
    }

    @Override
    public List<AssignmentTest> findAssignmentTestsByAssignmentId(Long assignmentId) {
        return mapper.toAssignmentTests(repository.findAllByAssignmentIdOrderByIdAsc(assignmentId));
    }

    @Override
    public AssignmentTest findAssignmentTestByCountedResultId(Long countedResultId) {
        return repository.findByCountedResultId(countedResultId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment test not found by counted result id: " + countedResultId
            ));
    }

    @Override
    @Transactional
    public AssignmentTest saveAssignmentTest(AssignmentTest assignmentTest) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(assignmentTest)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist assignment_test", exception);
        }
    }
}
