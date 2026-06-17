package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataUserOrganizationAssignmentJpaRepository}.
 */
public interface SpringDataUserOrganizationAssignmentJpaRepository
    extends JpaRepository<UserOrganizationAssignmentEntity, Long>, JpaSpecificationExecutor<UserOrganizationAssignmentEntity> {

    List<UserOrganizationAssignmentEntity> findAllByUserIdOrderByValidFromDescIdDesc(Long userId);

    List<UserOrganizationAssignmentEntity> findAllByOrganizationalUnitIdOrderByValidFromDescIdDesc(Long organizationalUnitId);

    List<UserOrganizationAssignmentEntity> findAllByAssignmentTypeOrderByValidFromDescIdDesc(
        OrganizationAssignmentType assignmentType
    );

    @Query(
        """
        select assignment
        from UserOrganizationAssignmentEntity assignment
        where assignment.userId = :userId
          and assignment.validFrom <= :activeAt
          and (assignment.validTo is null or assignment.validTo > :activeAt)
        order by assignment.validFrom desc, assignment.id desc
        """
    )
    List<UserOrganizationAssignmentEntity> findActiveByUserId(
        @Param("userId") Long userId,
        @Param("activeAt") Instant activeAt
    );

    @Query(
        """
        select distinct assignment.userId
        from UserOrganizationAssignmentEntity assignment
        where assignment.organizationalUnitId in :organizationalUnitIds
          and assignment.validFrom <= :activeAt
          and (assignment.validTo is null or assignment.validTo > :activeAt)
        """
    )
    List<Long> findDistinctActiveUserIdsByOrganizationalUnitIdIn(
        @Param("organizationalUnitIds") Collection<Long> organizationalUnitIds,
        @Param("activeAt") Instant activeAt
    );
}
