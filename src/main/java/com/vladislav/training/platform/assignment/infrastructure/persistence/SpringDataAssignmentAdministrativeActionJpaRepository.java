package com.vladislav.training.platform.assignment.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAssignmentAdministrativeActionJpaRepository
    extends JpaRepository<AssignmentAdministrativeActionEntity, Long> {

    List<AssignmentAdministrativeActionEntity> findAllByAssignmentIdOrderByIdAsc(Long assignmentId);
}
