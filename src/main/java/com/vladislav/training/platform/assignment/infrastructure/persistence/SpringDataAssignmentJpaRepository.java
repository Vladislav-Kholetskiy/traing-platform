package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAssignmentJpaRepository extends JpaRepository<AssignmentEntity, Long> {

    List<AssignmentEntity> findAllByOrderByIdAsc();

    List<AssignmentEntity> findAllByCampaignIdOrderByIdAsc(Long campaignId);

    List<AssignmentEntity> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<AssignmentEntity> findByIdAndUserId(Long id, Long userId);

    List<AssignmentEntity> findAllByUserIdAndStatusOrderByIdAsc(Long userId, AssignmentStatus status);

    Optional<AssignmentEntity> findFirstByUserIdAndCourseIdAndCancelledAtIsNullAndClosedAtIsNullOrderByIdDesc(
        Long userId,
        Long courseId
    );
}
