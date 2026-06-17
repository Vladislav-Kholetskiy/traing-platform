package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentAdministrativeActionRepositoryAdapter implements AssignmentAdministrativeActionRepository {

    private final SpringDataAssignmentAdministrativeActionJpaRepository repository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentAdministrativeActionRepositoryAdapter(
        SpringDataAssignmentAdministrativeActionJpaRepository repository,
        AssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AssignmentAdministrativeAction findAssignmentAdministrativeActionById(Long assignmentAdministrativeActionId) {
        return repository.findById(assignmentAdministrativeActionId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment administrative action not found: " + assignmentAdministrativeActionId
            ));
    }

    @Override
    public List<AssignmentAdministrativeAction> findAssignmentAdministrativeActionsByAssignmentId(Long assignmentId) {
        return mapper.toAssignmentAdministrativeActions(repository.findAllByAssignmentIdOrderByIdAsc(assignmentId));
    }

    @Override
    @Transactional
    public AssignmentAdministrativeAction saveAssignmentAdministrativeAction(
        AssignmentAdministrativeAction assignmentAdministrativeAction
    ) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(assignmentAdministrativeAction)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to persist assignment_administrative_action",
                exception
            );
        }
    }
}
