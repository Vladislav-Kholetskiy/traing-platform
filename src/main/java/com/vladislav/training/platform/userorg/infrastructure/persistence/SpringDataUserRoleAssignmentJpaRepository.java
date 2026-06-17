package com.vladislav.training.platform.userorg.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataUserRoleAssignmentJpaRepository}.
 */
public interface SpringDataUserRoleAssignmentJpaRepository
    extends JpaRepository<UserRoleAssignmentEntity, Long>, JpaSpecificationExecutor<UserRoleAssignmentEntity> {

    List<UserRoleAssignmentEntity> findAllByUserIdOrderByValidFromDescIdDesc(Long userId);

    List<UserRoleAssignmentEntity> findAllByRoleIdOrderByValidFromDescIdDesc(Long roleId);

    @Query(
        """
        select assignment
        from UserRoleAssignmentEntity assignment
        where assignment.userId = :userId
          and assignment.validFrom <= :activeAt
          and (assignment.validTo is null or assignment.validTo > :activeAt)
        order by assignment.validFrom desc, assignment.id desc
        """
    )
    List<UserRoleAssignmentEntity> findActiveByUserId(@Param("userId") Long userId, @Param("activeAt") Instant activeAt);
}

