package com.vladislav.training.platform.access.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataTemporaryRoleAssignmentJpaRepository}.
 */
public interface SpringDataTemporaryRoleAssignmentJpaRepository
    extends JpaRepository<TemporaryRoleAssignmentEntity, Long>, JpaSpecificationExecutor<TemporaryRoleAssignmentEntity> {

    List<TemporaryRoleAssignmentEntity> findAllByUserIdOrderByValidFromDescIdDesc(Long userId);

    @Query(
        """
        select assignment
        from TemporaryRoleAssignmentEntity assignment
        where assignment.userId = :userId
          and assignment.validFrom <= :activeAt
          and (assignment.validTo is null or assignment.validTo > :activeAt)
        order by assignment.validFrom desc, assignment.id desc
        """
    )
    List<TemporaryRoleAssignmentEntity> findActiveByUserId(@Param("userId") Long userId, @Param("activeAt") Instant activeAt);
}
