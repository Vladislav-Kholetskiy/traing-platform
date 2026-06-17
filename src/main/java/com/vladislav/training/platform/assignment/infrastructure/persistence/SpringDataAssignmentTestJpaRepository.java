package com.vladislav.training.platform.assignment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAssignmentTestJpaRepository extends JpaRepository<AssignmentTestEntity, Long> {

    List<AssignmentTestEntity> findAllByAssignmentIdOrderByIdAsc(Long assignmentId);

    Optional<AssignmentTestEntity> findByCountedResultId(Long countedResultId);
}
